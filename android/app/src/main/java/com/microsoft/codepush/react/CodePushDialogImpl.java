package com.microsoft.codepush.react;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReactApplicationContext;

final class CodePushDialogImpl {
    private final ReactApplicationContext reactContext;

    CodePushDialogImpl(ReactApplicationContext reactContext) {
        this.reactContext = reactContext;
    }

    void showDialog(
            final String title,
            final String message,
            final String button1Text,
            final String button2Text,
            final Callback successCallback,
            final Callback errorCallback
    ) {
        Activity currentActivity = reactContext.getCurrentActivity();
        if (currentActivity == null || currentActivity.isFinishing()) {
            reactContext.addLifecycleEventListener(new LifecycleEventListener() {
                @Override
                public void onHostResume() {
                    Activity resumedActivity = reactContext.getCurrentActivity();
                    if (resumedActivity != null && !resumedActivity.isFinishing()) {
                        reactContext.removeLifecycleEventListener(this);
                        showDialogInternal(title, message, button1Text, button2Text, successCallback, resumedActivity);
                    }
                }

                @Override
                public void onHostPause() {
                }

                @Override
                public void onHostDestroy() {
                }
            });
        } else {
            showDialogInternal(title, message, button1Text, button2Text, successCallback, currentActivity);
        }
    }

    private void showDialogInternal(
            String title,
            String message,
            String button1Text,
            String button2Text,
            final Callback successCallback,
            Activity currentActivity
    ) {
        AlertDialog.Builder builder = new AlertDialog.Builder(currentActivity);
        builder.setCancelable(false);

        DialogInterface.OnClickListener clickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    dialog.cancel();
                    switch (which) {
                        case DialogInterface.BUTTON_POSITIVE:
                            if (successCallback != null) successCallback.invoke(0);
                            break;
                        case DialogInterface.BUTTON_NEGATIVE:
                            if (successCallback != null) successCallback.invoke(1);
                            break;
                        default:
                            throw new CodePushUnknownException("Unknown button ID pressed.");
                    }
                } catch (Throwable e) {
                    CodePushUtils.log(e);
                }
            }
        };

        if (title != null) {
            builder.setTitle(title);
        }

        if (message != null) {
            builder.setMessage(message);
        }

        if (button1Text != null) {
            builder.setPositiveButton(button1Text, clickListener);
        }

        if (button2Text != null) {
            builder.setNegativeButton(button2Text, clickListener);
        }

        AlertDialog dialog = builder.create();
        dialog.show();
    }
}
