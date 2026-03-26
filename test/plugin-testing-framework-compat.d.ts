declare module "@srcpush/plugin-testing-framework" {
    import Q = require("q");

    namespace Platform {
        interface IPlatform {
            getName(): string;
            getCommandLineFlagName(): string;
            getServerUrl(): string;
            getEmulatorManager(): IEmulatorManager;
            getDefaultDeploymentKey(): string;
        }

        interface IEmulatorManager {
            getTargetEmulator(): Q.Promise<string>;
            bootEmulator(restartEmulators: boolean): Q.Promise<void>;
            launchInstalledApplication(appId: string): Q.Promise<void>;
            endRunningApplication(appId: string): Q.Promise<void>;
            restartApplication(appId: string): Q.Promise<void>;
            resumeApplication(appId: string, delayBeforeResumingMs?: number): Q.Promise<void>;
            prepareEmulatorForTest(appId: string): Q.Promise<void>;
            uninstallApplication(appId: string): Q.Promise<void>;
        }

        class Android implements IPlatform {
            constructor(emulatorManager: IEmulatorManager);
            getName(): string;
            getCommandLineFlagName(): string;
            getServerUrl(): string;
            getEmulatorManager(): IEmulatorManager;
            getDefaultDeploymentKey(): string;
        }

        class IOS implements IPlatform {
            constructor(emulatorManager: IEmulatorManager);
            getName(): string;
            getCommandLineFlagName(): string;
            getServerUrl(): string;
            getEmulatorManager(): IEmulatorManager;
            getDefaultDeploymentKey(): string;
        }

        class AndroidEmulatorManager implements IEmulatorManager {
            getTargetEmulator(): Q.Promise<string>;
            bootEmulator(restartEmulators: boolean): Q.Promise<void>;
            launchInstalledApplication(appId: string): Q.Promise<void>;
            endRunningApplication(appId: string): Q.Promise<void>;
            restartApplication(appId: string): Q.Promise<void>;
            resumeApplication(appId: string, delayBeforeResumingMs?: number): Q.Promise<void>;
            prepareEmulatorForTest(appId: string): Q.Promise<void>;
            uninstallApplication(appId: string): Q.Promise<void>;
        }

        class IOSEmulatorManager implements IEmulatorManager {
            getTargetEmulator(): Q.Promise<string>;
            bootEmulator(restartEmulators: boolean): Q.Promise<void>;
            launchInstalledApplication(appId: string): Q.Promise<void>;
            endRunningApplication(appId: string): Q.Promise<void>;
            restartApplication(appId: string): Q.Promise<void>;
            resumeApplication(appId: string, delayBeforeResumingMs?: number): Q.Promise<void>;
            prepareEmulatorForTest(appId: string): Q.Promise<void>;
            uninstallApplication(appId: string): Q.Promise<void>;
        }
    }

    namespace PluginTestingFramework {
        function initializeTests(
            projectManager: ProjectManager,
            supportedTargetPlatforms: Platform.IPlatform[],
            describeTests: (projectManager: ProjectManager, targetPlatform: Platform.IPlatform) => void
        ): void;
    }

    class ProjectManager {
        static DEFAULT_APP_VERSION: string;
        getPluginName(): string;
        setupProject(projectDirectory: string, templatePath: string, appName: string, appNamespace: string, version?: string): Q.Promise<void>;
        setupScenario(projectDirectory: string, appId: string, templatePath: string, jsPath: string, targetPlatform: Platform.IPlatform, version?: string): Q.Promise<void>;
        createUpdateArchive(projectDirectory: string, targetPlatform: Platform.IPlatform, isDiff?: boolean): Q.Promise<string>;
        preparePlatform(projectDirectory: string, targetPlatform: Platform.IPlatform): Q.Promise<void>;
        cleanupAfterPlatform(projectDirectory: string, targetPlatform: Platform.IPlatform): Q.Promise<void>;
        runApplication(projectDirectory: string, targetPlatform: Platform.IPlatform): Q.Promise<void>;
    }

    function setupTestRunScenario(projectManager: ProjectManager, targetPlatform: Platform.IPlatform, scenarioJsPath: string, version?: string): Q.Promise<void>;
    function setupUpdateScenario(projectManager: ProjectManager, targetPlatform: Platform.IPlatform, scenarioJsPath: string, version: string): Q.Promise<string>;

    namespace ServerUtil {
        let server: any;
        let updateResponse: any;
        let testMessageResponse: any;
        let testMessageCallback: (requestBody: any) => void;
        let updateCheckCallback: (requestBody: any) => void;
        let updatePackagePath: string;

        function setupServer(targetPlatform: Platform.IPlatform): void;
        function cleanupServer(): void;
        function createDefaultResponse(): any;
        function createUpdateResponse(mandatory?: boolean, targetPlatform?: Platform.IPlatform, randomHash?: boolean): any;
        function expectTestMessages(expectedMessages: any[]): Q.Promise<void>;

        class TestMessage {
            static CHECK_UP_TO_DATE: string;
            static CHECK_UPDATE_AVAILABLE: string;
            static CHECK_ERROR: string;
            static DOWNLOAD_SUCCEEDED: string;
            static DOWNLOAD_ERROR: string;
            static UPDATE_INSTALLED: string;
            static INSTALL_ERROR: string;
            static DEVICE_READY_AFTER_UPDATE: string;
            static UPDATE_FAILED_PREVIOUSLY: string;
            static NOTIFY_APP_READY_SUCCESS: string;
            static NOTIFY_APP_READY_FAILURE: string;
            static SKIPPED_NOTIFY_APPLICATION_READY: string;
            static SYNC_STATUS: string;
            static RESTART_SUCCEEDED: string;
            static RESTART_FAILED: string;
            static PENDING_PACKAGE: string;
            static CURRENT_PACKAGE: string;
            static SYNC_UP_TO_DATE: number;
            static SYNC_UPDATE_INSTALLED: number;
            static SYNC_UPDATE_IGNORED: number;
            static SYNC_ERROR: number;
            static SYNC_IN_PROGRESS: number;
            static SYNC_CHECKING_FOR_UPDATE: number;
            static SYNC_AWAITING_USER_ACTION: number;
            static SYNC_DOWNLOADING_PACKAGE: number;
            static SYNC_INSTALLING_UPDATE: number;
        }

        class TestMessageResponse {
            static SKIP_NOTIFY_APPLICATION_READY: string;
        }

        class AppMessage {
            message: string;
            args: any[];
            constructor(message: string, args: any[]);
            static fromString(message: string): AppMessage;
        }
    }

    class TestBuilder {
        static describe: {
            (description: string, spec: () => void, scenarioPath?: string): void;
            only(description: string, spec: () => void, scenarioPath?: string): void;
            skip(description: string, spec: () => void, scenarioPath?: string): void;
        };
        static it: {
            (expectation: string, isCoreTest: boolean, assertion: (done: Mocha.Done) => void): void;
            only(expectation: string, isCoreTest: boolean, assertion: (done: Mocha.Done) => void): void;
            skip(expectation: string, isCoreTest: boolean, assertion: (done: Mocha.Done) => void): void;
        };
    }

    namespace TestConfig {
        const TestAppName: string;
        const TestNamespace: string;
        const AcquisitionSDKPluginName: string;
        const templatePath: string;
        const thisPluginInstallString: string;
        const testRunDirectory: string;
        const updatesDirectory: string;
        const onlyRunCoreTests: boolean;
        const shouldSetup: boolean;
        const restartEmulators: boolean;
        const isOldArchitecture: boolean;
    }

    class TestUtil {
        static ANDROID_KEY_PLACEHOLDER: string;
        static IOS_KEY_PLACEHOLDER: string;
        static SERVER_URL_PLACEHOLDER: string;
        static INDEX_JS_PLACEHOLDER: string;
        static CODE_PUSH_APP_VERSION_PLACEHOLDER: string;
        static CODE_PUSH_TEST_APP_NAME_PLACEHOLDER: string;
        static CODE_PUSH_APP_ID_PLACEHOLDER: string;
        static PLUGIN_VERSION_PLACEHOLDER: string;

        static readMochaCommandLineOption(optionName: string, defaultValue?: string): string;
        static readMochaCommandLineFlag(optionName: string): boolean;
        static getProcessOutput(command: string, options?: any): Q.Promise<string>;
        static getPluginName(): string;
        static getPluginVersion(): string;
        static replaceString(filePath: string, regex: string, replacement: string): void;
        static copyFile(source: string, destination: string, overwrite: boolean): Q.Promise<void>;
        static archiveFolder(sourceFolder: string, targetFolder: string, archivePath: string, isDiff: boolean): Q.Promise<string>;
        static resolveBooleanVariables(variable: string | undefined): boolean;
    }

    export { Platform, PluginTestingFramework, ProjectManager, setupTestRunScenario, setupUpdateScenario, ServerUtil, TestBuilder, TestConfig, TestUtil };
}
