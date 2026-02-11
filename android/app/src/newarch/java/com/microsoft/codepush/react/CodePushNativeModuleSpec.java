package com.microsoft.codepush.react;

import com.facebook.react.bridge.ReactApplicationContext;

public abstract class CodePushNativeModuleSpec extends NativeRNCodePushSpec {
    public CodePushNativeModuleSpec(ReactApplicationContext reactContext) {
        super(reactContext);
    }
}
