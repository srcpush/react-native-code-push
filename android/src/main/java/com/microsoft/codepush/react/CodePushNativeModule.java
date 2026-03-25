package com.microsoft.codepush.react;

import com.facebook.react.bridge.BaseJavaModule;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;

import java.util.Map;

public class CodePushNativeModule extends BaseJavaModule {
    public static final String NAME = "CodePush";

    private final CodePushNativeModuleImpl impl;

    public CodePushNativeModule(
            ReactApplicationContext reactContext,
            CodePush codePush,
            CodePushUpdateManager codePushUpdateManager,
            CodePushTelemetryManager codePushTelemetryManager,
            SettingsManager settingsManager
    ) {
        super(reactContext);
        impl = new CodePushNativeModuleImpl(reactContext, codePush, codePushUpdateManager, codePushTelemetryManager, settingsManager);
    }

    @Override
    public Map<String, Object> getConstants() {
        return impl.getConstants();
    }

    @Override
    public String getName() {
        return NAME;
    }

    @ReactMethod
    public void allow(Promise promise) {
        impl.allow(promise);
    }

    @ReactMethod
    public void clearPendingRestart(Promise promise) {
        impl.clearPendingRestart(promise);
    }

    @ReactMethod
    public void disallow(Promise promise) {
        impl.disallow(promise);
    }

    @ReactMethod
    public void restartApp(boolean onlyIfUpdateIsPending, Promise promise) {
        impl.restartApp(onlyIfUpdateIsPending, promise);
    }

    @ReactMethod
    public void downloadUpdate(ReadableMap updatePackage, boolean notifyProgress, Promise promise) {
        impl.downloadUpdate(updatePackage, notifyProgress, promise);
    }

    @ReactMethod
    public void getConfiguration(Promise promise) {
        impl.getConfiguration(promise);
    }

    @ReactMethod
    public void getUpdateMetadata(double updateState, Promise promise) {
        impl.getUpdateMetadata(updateState, promise);
    }

    @ReactMethod
    public void getNewStatusReport(Promise promise) {
        impl.getNewStatusReport(promise);
    }

    @ReactMethod
    public void installUpdate(ReadableMap updatePackage, double installMode, double minimumBackgroundDuration, Promise promise) {
        impl.installUpdate(updatePackage, installMode, minimumBackgroundDuration, promise);
    }

    @ReactMethod
    public void isFailedUpdate(String packageHash, Promise promise) {
        impl.isFailedUpdate(packageHash, promise);
    }

    @ReactMethod
    public void getLatestRollbackInfo(Promise promise) {
        impl.getLatestRollbackInfo(promise);
    }

    @ReactMethod
    public void setLatestRollbackInfo(String packageHash, Promise promise) {
        impl.setLatestRollbackInfo(packageHash, promise);
    }

    @ReactMethod
    public void isFirstRun(String packageHash, Promise promise) {
        impl.isFirstRun(packageHash, promise);
    }

    @ReactMethod
    public void notifyApplicationReady(Promise promise) {
        impl.notifyApplicationReady(promise);
    }

    @ReactMethod
    public void recordStatusReported(ReadableMap statusReport) {
        impl.recordStatusReported(statusReport);
    }

    @ReactMethod
    public void saveStatusReportForRetry(ReadableMap statusReport) {
        impl.saveStatusReportForRetry(statusReport);
    }

    @ReactMethod
    public void downloadAndReplaceCurrentBundle(String remoteBundleUrl) {
        impl.downloadAndReplaceCurrentBundle(remoteBundleUrl);
    }

    @ReactMethod
    public void clearUpdates() {
        impl.clearUpdates();
    }

    @ReactMethod
    public void addListener(String eventName) {
        impl.addListener(eventName);
    }

    @ReactMethod
    public void removeListeners(double count) {
        impl.removeListeners(count);
    }
}
