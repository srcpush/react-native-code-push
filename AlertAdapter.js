import React, { Platform } from "react-native";
let { Alert } = React;

if (Platform.OS === "android") {
  function resolveNativeModule(name) {
    const ReactNative = require("react-native");
    try {
      const turboModule =
        ReactNative.TurboModuleRegistry && ReactNative.TurboModuleRegistry.get
          ? ReactNative.TurboModuleRegistry.get(name)
          : null;
      if (turboModule) return turboModule;
    } catch (_e) {
      // Ignore and fall back to legacy NativeModules.
    }

    return ReactNative.NativeModules ? ReactNative.NativeModules[name] : null;
  }

  const CodePushDialog = resolveNativeModule("CodePushDialog");
    
  Alert = {
    alert(title, message, buttons) {
      if (buttons.length > 2) {
        throw "Can only show 2 buttons for Android dialog.";
      }
      
      const button1Text = buttons[0] ? buttons[0].text : null,
            button2Text = buttons[1] ? buttons[1].text : null;
      
      if (!CodePushDialog) {
        throw "CodePushDialog native module is not installed.";
      }

      CodePushDialog.showDialog(
        title, message, button1Text, button2Text,
        (buttonId) => { buttons[buttonId].onPress && buttons[buttonId].onPress(); }, 
        (error) => { throw error; });
    }
  };
}

module.exports = { Alert };
