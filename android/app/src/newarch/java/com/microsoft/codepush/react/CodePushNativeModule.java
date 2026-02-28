package com.microsoft.codepush.react;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.common.annotations.UnstableReactNativeAPI;
import com.facebook.react.module.annotations.ReactModule;

import java.util.Map;

@OptIn(markerClass = UnstableReactNativeAPI.class)
@ReactModule(name = CodePushNativeModule.NAME)
public class CodePushNativeModule extends NativeCodePushSpec {
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

    @NonNull
    @Override
    public String getName() {
        return NAME;
    }

    @Override
    protected Map<String, Object> getTypedExportedConstants() {
        return impl.getConstants();
    }

    @Override
    public void allow(Promise promise) {
        impl.allow(promise);
    }

    @Override
    public void clearPendingRestart(Promise promise) {
        impl.clearPendingRestart(promise);
    }

    @Override
    public void disallow(Promise promise) {
        impl.disallow(promise);
    }

    @Override
    public void restartApp(boolean onlyIfUpdateIsPending, Promise promise) {
        impl.restartApp(onlyIfUpdateIsPending, promise);
    }

    @Override
    public void downloadUpdate(ReadableMap updatePackage, boolean notifyProgress, Promise promise) {
        impl.downloadUpdate(updatePackage, notifyProgress, promise);
    }

    @Override
    public void getConfiguration(Promise promise) {
        impl.getConfiguration(promise);
    }

    @Override
    public void getUpdateMetadata(double updateState, Promise promise) {
        impl.getUpdateMetadata(updateState, promise);
    }

    @Override
    public void getNewStatusReport(Promise promise) {
        impl.getNewStatusReport(promise);
    }

    @Override
    public void installUpdate(ReadableMap updatePackage, double installMode, double minimumBackgroundDuration, Promise promise) {
        impl.installUpdate(updatePackage, installMode, minimumBackgroundDuration, promise);
    }

    @Override
    public void isFailedUpdate(String packageHash, Promise promise) {
        impl.isFailedUpdate(packageHash, promise);
    }

    @Override
    public void getLatestRollbackInfo(Promise promise) {
        impl.getLatestRollbackInfo(promise);
    }

    @Override
    public void setLatestRollbackInfo(String packageHash, Promise promise) {
        impl.setLatestRollbackInfo(packageHash, promise);
    }

    @Override
    public void isFirstRun(String packageHash, Promise promise) {
        impl.isFirstRun(packageHash, promise);
    }

    @Override
    public void notifyApplicationReady(Promise promise) {
        impl.notifyApplicationReady(promise);
    }

    @Override
    public void recordStatusReported(ReadableMap statusReport) {
        impl.recordStatusReported(statusReport);
    }

    @Override
    public void saveStatusReportForRetry(ReadableMap statusReport) {
        impl.saveStatusReportForRetry(statusReport);
    }

    @Override
    public void downloadAndReplaceCurrentBundle(String remoteBundleUrl) {
        impl.downloadAndReplaceCurrentBundle(remoteBundleUrl);
    }

    @Override
    public void clearUpdates() {
        impl.clearUpdates();
    }

    @Override
    public void addListener(String eventName) {
        impl.addListener(eventName);
    }

    @Override
    public void removeListeners(double count) {
        impl.removeListeners(count);
    }
}

