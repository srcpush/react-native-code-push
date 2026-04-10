package com.microsoft.codepush.react;

import com.facebook.react.bridge.BaseJavaModule;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactMethod;

public class CodePushDialog extends BaseJavaModule {
    public static final String NAME = "CodePushDialog";

    private final CodePushDialogImpl impl;

    public CodePushDialog(ReactApplicationContext reactContext) {
        super(reactContext);
        impl = new CodePushDialogImpl(reactContext);
    }

    @ReactMethod
    public void showDialog(
            final String title,
            final String message,
            final String button1Text,
            final String button2Text,
            final Callback successCallback,
            Callback errorCallback
    ) {
        try {
            impl.showDialog(title, message, button1Text, button2Text, successCallback, errorCallback);
        } catch (Throwable e) {
            if (errorCallback != null) {
                errorCallback.invoke(e.getMessage());
            }
        }
    }

    @ReactMethod
    public void addListener(String eventName) {
        // no-op
    }

    @ReactMethod
    public void removeListeners(double count) {
        // no-op
    }

    @Override
    public String getName() {
        return NAME;
    }
}
