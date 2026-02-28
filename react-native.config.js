module.exports = {
    dependency: {
        platforms: {
            android: {
                packageImportPath:
                    "import com.microsoft.codepush.react.CodePush;",
                packageInstance:
                    "CodePush.getInstance(getResources().getString(R.string.CodePushDeploymentKey), getApplicationContext(), (0 != (getApplicationContext().getApplicationInfo().flags & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE)))",
                sourceDir: './android/app',
            }
        }
    }
};
