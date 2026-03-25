package com.microsoft.codepush.react;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.view.Choreographer;
import android.view.View;

import com.facebook.react.ReactApplication;
import com.facebook.react.ReactInstanceManager;
import com.facebook.react.ReactRootView;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.JSBundleLoader;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.devsupport.interfaces.DevSupportManager;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.facebook.react.modules.core.ReactChoreographer;
import com.facebook.react.modules.debug.interfaces.DeveloperSettings;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class CodePushNativeModuleImpl {
    private String mBinaryContentsHash = null;
    private String mClientUniqueId = null;
    private LifecycleEventListener mLifecycleEventListener = null;
    private int mMinimumBackgroundDuration = 0;

    private final ReactApplicationContext reactContext;
    private final CodePush mCodePush;
    private final SettingsManager mSettingsManager;
    private final CodePushTelemetryManager mTelemetryManager;
    private final CodePushUpdateManager mUpdateManager;

    private boolean _allowed = true;
    private boolean _restartInProgress = false;
    private final ArrayList<Boolean> _restartQueue = new ArrayList<>();

    CodePushNativeModuleImpl(
            ReactApplicationContext reactContext,
            CodePush codePush,
            CodePushUpdateManager codePushUpdateManager,
            CodePushTelemetryManager codePushTelemetryManager,
            SettingsManager settingsManager
    ) {
        this.reactContext = reactContext;
        mCodePush = codePush;
        mSettingsManager = settingsManager;
        mTelemetryManager = codePushTelemetryManager;
        mUpdateManager = codePushUpdateManager;

        mBinaryContentsHash = CodePushUpdateUtils.getHashForBinaryContents(reactContext, mCodePush.isDebugMode());

        SharedPreferences preferences = codePush.getContext().getSharedPreferences(CodePushConstants.CODE_PUSH_PREFERENCES, 0);
        mClientUniqueId = preferences.getString(CodePushConstants.CLIENT_UNIQUE_ID_KEY, null);
        if (mClientUniqueId == null) {
            mClientUniqueId = UUID.randomUUID().toString();
            preferences.edit().putString(CodePushConstants.CLIENT_UNIQUE_ID_KEY, mClientUniqueId).apply();
        }
    }

    Map<String, Object> getConstants() {
        final Map<String, Object> constants = new HashMap<>();

        constants.put("codePushInstallModeImmediate", CodePushInstallMode.IMMEDIATE.getValue());
        constants.put("codePushInstallModeOnNextRestart", CodePushInstallMode.ON_NEXT_RESTART.getValue());
        constants.put("codePushInstallModeOnNextResume", CodePushInstallMode.ON_NEXT_RESUME.getValue());
        constants.put("codePushInstallModeOnNextSuspend", CodePushInstallMode.ON_NEXT_SUSPEND.getValue());

        constants.put("codePushUpdateStateRunning", CodePushUpdateState.RUNNING.getValue());
        constants.put("codePushUpdateStatePending", CodePushUpdateState.PENDING.getValue());
        constants.put("codePushUpdateStateLatest", CodePushUpdateState.LATEST.getValue());

        return constants;
    }

    private void loadBundleLegacy() {
        final Activity currentActivity = reactContext.getCurrentActivity();
        if (currentActivity == null) {
            return;
        }
        mCodePush.invalidateCurrentInstance();

        currentActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                currentActivity.recreate();
            }
        });
    }

    private void setJSBundle(ReactInstanceManager instanceManager, String latestJSBundleFile) throws IllegalAccessException {
        try {
            JSBundleLoader latestJSBundleLoader;
            if (latestJSBundleFile.toLowerCase().startsWith("assets://")) {
                latestJSBundleLoader = JSBundleLoader.createAssetLoader(reactContext, latestJSBundleFile, false);
            } else {
                latestJSBundleLoader = JSBundleLoader.createFileLoader(latestJSBundleFile);
            }

            Field bundleLoaderField = instanceManager.getClass().getDeclaredField("mBundleLoader");
            bundleLoaderField.setAccessible(true);
            bundleLoaderField.set(instanceManager, latestJSBundleLoader);
        } catch (Exception e) {
            CodePushUtils.log("Unable to set JSBundle of ReactInstanceManager - CodePush may not support this version of React Native");
            throw new IllegalAccessException("Could not setJSBundle");
        }
    }

    private void loadBundle() {
        clearLifecycleEventListener();

        try {
            DevSupportManager devSupportManager = resolveDevSupportManager();
            boolean isLiveReloadEnabled = isLiveReloadEnabled(devSupportManager);
            mCodePush.clearDebugCacheIfNeeded(isLiveReloadEnabled);
        } catch (Exception e) {
            mCodePush.clearDebugCacheIfNeeded(false);
        }

        try {
            String latestJSBundleFile = mCodePush.getJSBundleFileInternal(mCodePush.getAssetsBundleFileName());
            Object reactHost = resolveReactHost();

            if (reactHost != null) {
                setReactHostBundleLoader(reactHost, latestJSBundleFile);
                if (reloadReactHost(reactHost)) {
                    mCodePush.initializeUpdateAfterRestart();
                    return;
                }
            }

            final ReactInstanceManager instanceManager = resolveInstanceManager();
            if (instanceManager == null) {
                loadBundleLegacy();
                return;
            }

            setJSBundle(instanceManager, latestJSBundleFile);
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    try {
                        instanceManager.recreateReactContextInBackground();
                        mCodePush.initializeUpdateAfterRestart();
                    } catch (Exception e) {
                        loadBundleLegacy();
                    }
                }
            });
        } catch (Exception e) {
            CodePushUtils.log("Failed to load the bundle, falling back to restarting the Activity (if it exists). " + e.getMessage());
            loadBundleLegacy();
        }
    }

    private boolean isLiveReloadEnabled(DevSupportManager devSupportManager) {
        if (devSupportManager != null) {
            DeveloperSettings devSettings = devSupportManager.getDevSettings();
            Method[] methods = devSettings.getClass().getMethods();
            for (Method m : methods) {
                if (m.getName().equals("isReloadOnJSChangeEnabled")) {
                    try {
                        return (boolean) m.invoke(devSettings);
                    } catch (Exception x) {
                        return false;
                    }
                }
            }
        }

        return false;
    }

    @SuppressWarnings("unused")
    private void resetReactRootViews(ReactInstanceManager instanceManager) throws NoSuchFieldException, IllegalAccessException {
        Field mAttachedRootViewsField = instanceManager.getClass().getDeclaredField("mAttachedRootViews");
        mAttachedRootViewsField.setAccessible(true);
        List<ReactRootView> mAttachedRootViews = (List<ReactRootView>) mAttachedRootViewsField.get(instanceManager);
        for (ReactRootView reactRootView : mAttachedRootViews) {
            reactRootView.removeAllViews();
            reactRootView.setId(View.NO_ID);
        }
        mAttachedRootViewsField.set(instanceManager, mAttachedRootViews);
    }

    private void clearLifecycleEventListener() {
        if (mLifecycleEventListener != null) {
            reactContext.removeLifecycleEventListener(mLifecycleEventListener);
            mLifecycleEventListener = null;
        }
    }

    private ReactInstanceManager resolveInstanceManager() {
        ReactInstanceManager instanceManager = CodePush.getReactInstanceManager();
        if (instanceManager != null) {
            return instanceManager;
        }

        final Activity currentActivity = reactContext.getCurrentActivity();
        if (currentActivity == null) {
            return null;
        }

        ReactApplication reactApplication = (ReactApplication) currentActivity.getApplication();
        return reactApplication.getReactNativeHost().getReactInstanceManager();
    }

    private DevSupportManager resolveDevSupportManager() {
        Object reactHost = resolveReactHost();
        if (reactHost != null) {
            DevSupportManager devSupportManager = getDevSupportManagerFromReactHost(reactHost);
            if (devSupportManager != null) {
                return devSupportManager;
            }
        }

        ReactInstanceManager instanceManager = resolveInstanceManager();
        return instanceManager != null ? instanceManager.getDevSupportManager() : null;
    }

    private Object resolveReactHost() {
        Object reactHost = CodePush.getReactHost();
        if (reactHost != null) {
            return reactHost;
        }

        final Activity currentActivity = reactContext.getCurrentActivity();
        if (currentActivity == null) {
            return null;
        }

        ReactApplication reactApplication = (ReactApplication) currentActivity.getApplication();
        try {
            Method getReactHostMethod = reactApplication.getClass().getMethod("getReactHost");
            return getReactHostMethod.invoke(reactApplication);
        } catch (Exception e) {
            return null;
        }
    }

    private void restartAppInternal(boolean onlyIfUpdateIsPending) {
        if (this._restartInProgress) {
            CodePushUtils.log("Restart request queued until the current restart is completed");
            this._restartQueue.add(onlyIfUpdateIsPending);
            return;
        } else if (!this._allowed) {
            CodePushUtils.log("Restart request queued until restarts are re-allowed");
            this._restartQueue.add(onlyIfUpdateIsPending);
            return;
        }

        this._restartInProgress = true;
        if (!onlyIfUpdateIsPending || mSettingsManager.isPendingUpdate(null)) {
            loadBundle();
            CodePushUtils.log("Restarting app");
            return;
        }

        this._restartInProgress = false;
        if (this._restartQueue.size() > 0) {
            boolean buf = this._restartQueue.get(0);
            this._restartQueue.remove(0);
            this.restartAppInternal(buf);
        }
    }

    void allow(Promise promise) {
        CodePushUtils.log("Re-allowing restarts");
        this._allowed = true;

        if (_restartQueue.size() > 0) {
            CodePushUtils.log("Executing pending restart");
            boolean buf = this._restartQueue.get(0);
            this._restartQueue.remove(0);
            this.restartAppInternal(buf);
        }

        promise.resolve(null);
    }

    void clearPendingRestart(Promise promise) {
        this._restartQueue.clear();
        promise.resolve(null);
    }

    void disallow(Promise promise) {
        CodePushUtils.log("Disallowing restarts");
        this._allowed = false;
        promise.resolve(null);
    }

    void restartApp(boolean onlyIfUpdateIsPending, Promise promise) {
        try {
            restartAppInternal(onlyIfUpdateIsPending);
            promise.resolve(null);
        } catch (CodePushUnknownException e) {
            CodePushUtils.log(e);
            promise.reject(e);
        }
    }

    void downloadUpdate(final ReadableMap updatePackage, final boolean notifyProgress, final Promise promise) {
        AsyncTask<Void, Void, Void> asyncTask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    JSONObject mutableUpdatePackage = CodePushUtils.convertReadableToJsonObject(updatePackage);
                    CodePushUtils.setJSONValueForKey(mutableUpdatePackage, CodePushConstants.BINARY_MODIFIED_TIME_KEY, "" + mCodePush.getBinaryResourcesModifiedTime());
                    mUpdateManager.downloadPackage(mutableUpdatePackage, mCodePush.getAssetsBundleFileName(), new DownloadProgressCallback() {
                        private boolean hasScheduledNextFrame = false;
                        private DownloadProgress latestDownloadProgress = null;

                        @Override
                        public void call(DownloadProgress downloadProgress) {
                            if (!notifyProgress) {
                                return;
                            }

                            latestDownloadProgress = downloadProgress;
                            if (latestDownloadProgress.isCompleted()) {
                                dispatchDownloadProgressEvent();
                                return;
                            }

                            if (hasScheduledNextFrame) {
                                return;
                            }

                            hasScheduledNextFrame = true;
                            reactContext.runOnUiQueueThread(new Runnable() {
                                @Override
                                public void run() {
                                    ReactChoreographer.getInstance().postFrameCallback(ReactChoreographer.CallbackType.TIMERS_EVENTS, new Choreographer.FrameCallback() {
                                        @Override
                                        public void doFrame(long frameTimeNanos) {
                                            if (!latestDownloadProgress.isCompleted()) {
                                                dispatchDownloadProgressEvent();
                                            }

                                            hasScheduledNextFrame = false;
                                        }
                                    });
                                }
                            });
                        }

                        public void dispatchDownloadProgressEvent() {
                            reactContext
                                    .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                                    .emit(CodePushConstants.DOWNLOAD_PROGRESS_EVENT_NAME, latestDownloadProgress.createWritableMap());
                        }
                    }, mCodePush.getPublicKey());

                    JSONObject newPackage = mUpdateManager.getPackage(CodePushUtils.tryGetString(updatePackage, CodePushConstants.PACKAGE_HASH_KEY));
                    promise.resolve(CodePushUtils.convertJsonObjectToWritable(newPackage));
                } catch (CodePushInvalidUpdateException e) {
                    CodePushUtils.log(e);
                    mSettingsManager.saveFailedUpdate(CodePushUtils.convertReadableToJsonObject(updatePackage));
                    promise.reject(e);
                } catch (IOException | CodePushUnknownException e) {
                    CodePushUtils.log(e);
                    promise.reject(e);
                }

                return null;
            }
        };

        asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    void getConfiguration(Promise promise) {
        try {
            WritableMap configMap = Arguments.createMap();
            configMap.putString("appVersion", mCodePush.getAppVersion());
            configMap.putString("clientUniqueId", mClientUniqueId);
            configMap.putString("deploymentKey", mCodePush.getDeploymentKey());
            configMap.putString("serverUrl", mCodePush.getServerUrl());

            if (mBinaryContentsHash != null) {
                configMap.putString(CodePushConstants.PACKAGE_HASH_KEY, mBinaryContentsHash);
            }

            promise.resolve(configMap);
        } catch (CodePushUnknownException e) {
            CodePushUtils.log(e);
            promise.reject(e);
        }
    }

    void getUpdateMetadata(final double updateState, final Promise promise) {
        AsyncTask<Void, Void, Void> asyncTask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    JSONObject currentPackage = mUpdateManager.getCurrentPackage();

                    if (currentPackage == null) {
                        promise.resolve(null);
                        return null;
                    }

                    Boolean currentUpdateIsPending = false;

                    if (currentPackage.has(CodePushConstants.PACKAGE_HASH_KEY)) {
                        String currentHash = currentPackage.optString(CodePushConstants.PACKAGE_HASH_KEY, null);
                        currentUpdateIsPending = mSettingsManager.isPendingUpdate(currentHash);
                    }

                    int updateStateValue = (int) updateState;
                    if (updateStateValue == CodePushUpdateState.PENDING.getValue() && !currentUpdateIsPending) {
                        promise.resolve(null);
                    } else if (updateStateValue == CodePushUpdateState.RUNNING.getValue() && currentUpdateIsPending) {
                        JSONObject previousPackage = mUpdateManager.getPreviousPackage();
                        if (previousPackage == null) {
                            promise.resolve(null);
                            return null;
                        }
                        promise.resolve(CodePushUtils.convertJsonObjectToWritable(previousPackage));
                    } else {
                        if (mCodePush.isRunningBinaryVersion()) {
                            CodePushUtils.setJSONValueForKey(currentPackage, "_isDebugOnly", true);
                        }

                        CodePushUtils.setJSONValueForKey(currentPackage, "isPending", currentUpdateIsPending);
                        promise.resolve(CodePushUtils.convertJsonObjectToWritable(currentPackage));
                    }
                } catch (CodePushMalformedDataException e) {
                    CodePushUtils.log(e.getMessage());
                    clearUpdates();
                    promise.resolve(null);
                } catch (CodePushUnknownException e) {
                    CodePushUtils.log(e);
                    promise.reject(e);
                }

                return null;
            }
        };

        asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    void getNewStatusReport(final Promise promise) {
        AsyncTask<Void, Void, Void> asyncTask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    if (mCodePush.needToReportRollback()) {
                        mCodePush.setNeedToReportRollback(false);
                        JSONArray failedUpdates = mSettingsManager.getFailedUpdates();
                        if (failedUpdates != null && failedUpdates.length() > 0) {
                            try {
                                JSONObject lastFailedPackageJSON = failedUpdates.getJSONObject(failedUpdates.length() - 1);
                                WritableMap lastFailedPackage = CodePushUtils.convertJsonObjectToWritable(lastFailedPackageJSON);
                                WritableMap failedStatusReport = mTelemetryManager.getRollbackReport(lastFailedPackage);
                                if (failedStatusReport != null) {
                                    promise.resolve(failedStatusReport);
                                    return null;
                                }
                            } catch (JSONException e) {
                                throw new CodePushUnknownException("Unable to read failed updates information stored in SharedPreferences.", e);
                            }
                        }
                    } else if (mCodePush.didUpdate()) {
                        JSONObject currentPackage = mUpdateManager.getCurrentPackage();
                        if (currentPackage != null) {
                            WritableMap newPackageStatusReport = mTelemetryManager.getUpdateReport(CodePushUtils.convertJsonObjectToWritable(currentPackage));
                            if (newPackageStatusReport != null) {
                                promise.resolve(newPackageStatusReport);
                                return null;
                            }
                        }
                    } else if (mCodePush.isRunningBinaryVersion()) {
                        WritableMap newAppVersionStatusReport = mTelemetryManager.getBinaryUpdateReport(mCodePush.getAppVersion());
                        if (newAppVersionStatusReport != null) {
                            promise.resolve(newAppVersionStatusReport);
                            return null;
                        }
                    } else {
                        WritableMap retryStatusReport = mTelemetryManager.getRetryStatusReport();
                        if (retryStatusReport != null) {
                            promise.resolve(retryStatusReport);
                            return null;
                        }
                    }

                    promise.resolve(null);
                } catch (CodePushUnknownException e) {
                    CodePushUtils.log(e);
                    promise.reject(e);
                }
                return null;
            }
        };

        asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    void installUpdate(final ReadableMap updatePackage, final double installMode, final double minimumBackgroundDuration, final Promise promise) {
        AsyncTask<Void, Void, Void> asyncTask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    mUpdateManager.installPackage(CodePushUtils.convertReadableToJsonObject(updatePackage), mSettingsManager.isPendingUpdate(null));

                    String pendingHash = CodePushUtils.tryGetString(updatePackage, CodePushConstants.PACKAGE_HASH_KEY);
                    if (pendingHash == null) {
                        throw new CodePushUnknownException("Update package to be installed has no hash.");
                    } else {
                        mSettingsManager.savePendingUpdate(pendingHash, /* isLoading */ false);
                    }

                    int installModeValue = (int) installMode;
                    if (installModeValue == CodePushInstallMode.ON_NEXT_RESUME.getValue()
                            || installModeValue == CodePushInstallMode.IMMEDIATE.getValue()
                            || installModeValue == CodePushInstallMode.ON_NEXT_SUSPEND.getValue()) {

                        CodePushNativeModuleImpl.this.mMinimumBackgroundDuration = (int) minimumBackgroundDuration;

                        if (mLifecycleEventListener == null) {
                            mLifecycleEventListener = new LifecycleEventListener() {
                                private Date lastPausedDate = null;
                                private Handler appSuspendHandler = new Handler(Looper.getMainLooper());
                                private Runnable loadBundleRunnable = new Runnable() {
                                    @Override
                                    public void run() {
                                        CodePushUtils.log("Loading bundle on suspend");
                                        restartAppInternal(false);
                                    }
                                };

                                @Override
                                public void onHostResume() {
                                    appSuspendHandler.removeCallbacks(loadBundleRunnable);
                                    if (lastPausedDate != null) {
                                        long durationInBackground = (new Date().getTime() - lastPausedDate.getTime()) / 1000;
                                        if (installModeValue == CodePushInstallMode.IMMEDIATE.getValue()
                                                || durationInBackground >= CodePushNativeModuleImpl.this.mMinimumBackgroundDuration) {
                                            CodePushUtils.log("Loading bundle on resume");
                                            restartAppInternal(false);
                                        }
                                    }
                                }

                                @Override
                                public void onHostPause() {
                                    lastPausedDate = new Date();

                                    if (installModeValue == CodePushInstallMode.ON_NEXT_SUSPEND.getValue() && mSettingsManager.isPendingUpdate(null)) {
                                        appSuspendHandler.postDelayed(loadBundleRunnable, CodePushNativeModuleImpl.this.mMinimumBackgroundDuration * 1000L);
                                    }
                                }

                                @Override
                                public void onHostDestroy() {
                                }
                            };

                            reactContext.addLifecycleEventListener(mLifecycleEventListener);
                        }
                    }

                    promise.resolve(null);
                } catch (CodePushUnknownException e) {
                    CodePushUtils.log(e);
                    promise.reject(e);
                }

                return null;
            }
        };

        asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    void isFailedUpdate(String packageHash, Promise promise) {
        try {
            promise.resolve(mSettingsManager.isFailedHash(packageHash));
        } catch (CodePushUnknownException e) {
            CodePushUtils.log(e);
            promise.reject(e);
        }
    }

    void getLatestRollbackInfo(Promise promise) {
        try {
            JSONObject latestRollbackInfo = mSettingsManager.getLatestRollbackInfo();
            if (latestRollbackInfo != null) {
                promise.resolve(CodePushUtils.convertJsonObjectToWritable(latestRollbackInfo));
            } else {
                promise.resolve(null);
            }
        } catch (CodePushUnknownException e) {
            CodePushUtils.log(e);
            promise.reject(e);
        }
    }

    void setLatestRollbackInfo(String packageHash, Promise promise) {
        try {
            mSettingsManager.setLatestRollbackInfo(packageHash);
            promise.resolve(null);
        } catch (CodePushUnknownException e) {
            CodePushUtils.log(e);
            promise.reject(e);
        }
    }

    void isFirstRun(String packageHash, Promise promise) {
        try {
            boolean isFirstRun = mCodePush.didUpdate()
                    && packageHash != null
                    && packageHash.length() > 0
                    && packageHash.equals(mUpdateManager.getCurrentPackageHash());
            promise.resolve(isFirstRun);
        } catch (CodePushUnknownException e) {
            CodePushUtils.log(e);
            promise.reject(e);
        }
    }

    void notifyApplicationReady(Promise promise) {
        try {
            mSettingsManager.removePendingUpdate();
            promise.resolve(null);
        } catch (CodePushUnknownException e) {
            CodePushUtils.log(e);
            promise.reject(e);
        }
    }

    void recordStatusReported(ReadableMap statusReport) {
        try {
            mTelemetryManager.recordStatusReported(statusReport);
        } catch (CodePushUnknownException e) {
            CodePushUtils.log(e);
        }
    }

    void saveStatusReportForRetry(ReadableMap statusReport) {
        try {
            mTelemetryManager.saveStatusReportForRetry(statusReport);
        } catch (CodePushUnknownException e) {
            CodePushUtils.log(e);
        }
    }

    void downloadAndReplaceCurrentBundle(String remoteBundleUrl) {
        try {
            if (mCodePush.isUsingTestConfiguration()) {
                try {
                    mUpdateManager.downloadAndReplaceCurrentBundle(remoteBundleUrl, mCodePush.getAssetsBundleFileName());
                } catch (IOException e) {
                    throw new CodePushUnknownException("Unable to replace current bundle", e);
                }
            }
        } catch (CodePushUnknownException | CodePushMalformedDataException e) {
            CodePushUtils.log(e);
        }
    }

    void clearUpdates() {
        CodePushUtils.log("Clearing updates.");
        mCodePush.clearUpdates();
    }

    void addListener(String eventName) {
        // no-op
    }

    void removeListeners(double count) {
        // no-op
    }

    private JSBundleLoader createBundleLoader(String latestJSBundleFile) {
        if (latestJSBundleFile.toLowerCase().startsWith("assets://")) {
            return JSBundleLoader.createAssetLoader(reactContext, latestJSBundleFile, false);
        }

        return JSBundleLoader.createFileLoader(latestJSBundleFile);
    }

    private DevSupportManager getDevSupportManagerFromReactHost(Object reactHost) {
        try {
            Method method = reactHost.getClass().getMethod("getDevSupportManager");
            Object devSupportManager = method.invoke(reactHost);
            return devSupportManager instanceof DevSupportManager ? (DevSupportManager) devSupportManager : null;
        } catch (Exception e) {
            return null;
        }
    }

    private Object getReactHostDelegate(Object reactHost) {
        try {
            Method method = reactHost.getClass().getMethod("getReactHostDelegate");
            return method.invoke(reactHost);
        } catch (Exception ignored) {
        }

        try {
            Field field = findField(reactHost.getClass(), "mReactHostDelegate", "reactHostDelegate");
            if (field == null) {
                return null;
            }

            field.setAccessible(true);
            return field.get(reactHost);
        } catch (Exception e) {
            return null;
        }
    }

    private void setReactHostBundleLoader(Object reactHost, String latestJSBundleFile) throws IllegalAccessException {
        Object reactHostDelegate = getReactHostDelegate(reactHost);
        if (reactHostDelegate == null) {
            throw new IllegalAccessException("Could not resolve ReactHostDelegate");
        }

        try {
            Field bundleLoaderField = findField(reactHostDelegate.getClass(), "jsBundleLoader", "mJsBundleLoader");
            if (bundleLoaderField == null) {
                throw new NoSuchFieldException("jsBundleLoader");
            }

            bundleLoaderField.setAccessible(true);
            bundleLoaderField.set(reactHostDelegate, createBundleLoader(latestJSBundleFile));
        } catch (Exception e) {
            CodePushUtils.log("Unable to set JSBundle of ReactHostDelegate - CodePush may not support this version of React Native");
            throw new IllegalAccessException("Could not setJSBundle");
        }
    }

    private boolean reloadReactHost(Object reactHost) {
        try {
            Method reloadWithReasonMethod = reactHost.getClass().getMethod("reload", String.class);
            reloadWithReasonMethod.invoke(reactHost, "CodePush triggers reload");
            return true;
        } catch (Exception ignored) {
        }

        try {
            Method reloadMethod = reactHost.getClass().getMethod("reload");
            reloadMethod.invoke(reactHost);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Field findField(Class<?> type, String... fieldNames) {
        Class<?> currentType = type;
        while (currentType != null) {
            for (String fieldName : fieldNames) {
                try {
                    return currentType.getDeclaredField(fieldName);
                } catch (NoSuchFieldException ignored) {
                }
            }

            currentType = currentType.getSuperclass();
        }

        return null;
    }
}

