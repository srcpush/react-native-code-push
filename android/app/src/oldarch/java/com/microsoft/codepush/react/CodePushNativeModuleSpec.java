package com.microsoft.codepush.react;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReadableMap;

public abstract class CodePushNativeModuleSpec extends ReactContextBaseJavaModule {
    public CodePushNativeModuleSpec(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    public abstract void allow(Promise promise);
    public abstract void clearPendingRestart(Promise promise);
    public abstract void disallow(Promise promise);
    public abstract void restartApp(boolean onlyIfUpdateIsPending, Promise promise);
    public abstract void downloadUpdate(ReadableMap updatePackage, boolean notifyProgress, Promise promise);
    public abstract void getConfiguration(Promise promise);
    public abstract void getUpdateMetadata(double updateState, Promise promise);
    public abstract void getNewStatusReport(Promise promise);
    public abstract void installUpdate(ReadableMap updatePackage, double installMode, double minimumBackgroundDuration, Promise promise);
    public abstract void isFailedUpdate(String packageHash, Promise promise);
    public abstract void isFirstRun(String packageHash, Promise promise);
    public abstract void notifyApplicationReady(Promise promise);
    public abstract void recordStatusReported(ReadableMap statusReport);
    public abstract void saveStatusReportForRetry(ReadableMap statusReport);
    public abstract void downloadAndReplaceCurrentBundle(String remoteBundleUrl);
    public abstract void clearUpdates();
    public abstract void getLatestRollbackInfo(Promise promise);
    public abstract void setLatestRollbackInfo(String packageHash, Promise promise);
    public abstract void addListener(String eventName);
    public abstract void removeListeners(double count);
    public abstract java.util.Map<String, Object> getTypedExportedConstants();
}
