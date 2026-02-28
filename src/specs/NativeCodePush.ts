import type { TurboModule } from 'react-native';
import { TurboModuleRegistry } from 'react-native';
import type { UnsafeObject } from 'react-native/Libraries/Types/CodegenTypes';

export type CodePushConstants = {
  codePushInstallModeImmediate: number;
  codePushInstallModeOnNextRestart: number;
  codePushInstallModeOnNextResume: number;
  codePushInstallModeOnNextSuspend: number;

  codePushUpdateStateRunning: number;
  codePushUpdateStatePending: number;
  codePushUpdateStateLatest: number;
};

export interface Spec extends TurboModule {
  getConstants(): CodePushConstants;

  allow(): Promise<void>;
  clearPendingRestart(): Promise<void>;
  disallow(): Promise<void>;
  restartApp(onlyIfUpdateIsPending: boolean): Promise<void>;

  downloadUpdate(
    updatePackage: UnsafeObject,
    notifyProgress: boolean,
  ): Promise<UnsafeObject>;

  getConfiguration(): Promise<UnsafeObject>;
  getUpdateMetadata(updateState: number): Promise<UnsafeObject | null>;
  getNewStatusReport(): Promise<UnsafeObject | null>;
  installUpdate(
    updatePackage: UnsafeObject,
    installMode: number,
    minimumBackgroundDuration: number,
  ): Promise<void>;

  isFailedUpdate(packageHash: string): Promise<boolean>;
  getLatestRollbackInfo(): Promise<UnsafeObject | null>;
  setLatestRollbackInfo(packageHash: string): Promise<void>;
  isFirstRun(packageHash: string): Promise<boolean>;

  notifyApplicationReady(): Promise<void>;

  recordStatusReported(statusReport: UnsafeObject): void;
  saveStatusReportForRetry(statusReport: UnsafeObject): void;

  downloadAndReplaceCurrentBundle(remoteBundleUrl: string): void;
  clearUpdates(): void;

  addListener(eventName: string): void;
  removeListeners(count: number): void;
}

export default TurboModuleRegistry.getEnforcing<Spec>('CodePush');

