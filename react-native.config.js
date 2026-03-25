module.exports = {
    dependency: {
        platforms: {
            android: {
                sourceDir: './android',
                packageImportPath: 'import com.microsoft.codepush.react.CodePush;',
                packageInstance:
                    'CodePush.getInstance(getResources().getString(R.string.CodePushDeploymentKey), getApplicationContext(), BuildConfig.DEBUG)',
            }
        }
    }
};
