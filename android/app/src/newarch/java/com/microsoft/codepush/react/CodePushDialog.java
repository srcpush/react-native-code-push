package com.microsoft.codepush.react;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.module.annotations.ReactModule;

@ReactModule(name = CodePushDialog.NAME)
public class CodePushDialog extends NativeCodePushDialogSpec {
    public static final String NAME = "CodePushDialog";

    private final CodePushDialogImpl impl;

    public CodePushDialog(ReactApplicationContext reactContext) {
        super(reactContext);
        impl = new CodePushDialogImpl(reactContext);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void showDialog(
            String title,
            String message,
            String button1Text,
            String button2Text,
            Callback successCallback,
            Callback errorCallback
    ) {
        try {
            impl.showDialog(title, message, button1Text, button2Text, successCallback, errorCallback);
        } catch (Throwable e) {
            if (errorCallback != null) errorCallback.invoke(e.getMessage());
        }
    }

    @Override
    public void addListener(String eventName) {
        // no-op
    }

    @Override
    public void removeListeners(double count) {
        // no-op
    }
}

