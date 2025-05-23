var CodePushWrapper = require("../codePushWrapper.js");
import CodePush from "@srcpush/react-native-code-push";

module.exports = {
    startTest: function (testApp) {
        CodePushWrapper.checkAndInstall(testApp, undefined, undefined, CodePush.InstallMode.ON_NEXT_RESTART);
    },

    getScenarioName: function () {
        return "Install on Restart with Revert";
    }
};
