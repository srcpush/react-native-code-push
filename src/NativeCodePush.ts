import type { TurboModule } from 'react-native';
import { TurboModuleRegistry } from 'react-native';

export interface Spec extends TurboModule {
  getConstants(): {
    codePushInstallModeImmediate: number;
    codePushInstallModeOnNextRestart: number;
    codePushInstallModeOnNextResume: number;
    codePushInstallModeOnNextSuspend: number;
    codePushUpdateStateRunning: number;
    codePushUpdateStatePending: number;
    codePushUpdateStateLatest: number;
  };

  allow(): Promise<void>;
  clearPendingRestart(): Promise<void>;
  disallow(): Promise<void>;
  restartApp(onlyIfUpdateIsPending: boolean): Promise<void>;
  downloadUpdate(updatePackage: Object, notifyProgress: boolean): Promise<Object>;
  getConfiguration(): Promise<Object>;
  getUpdateMetadata(updateState: number): Promise<Object>;
  getNewStatusReport(): Promise<Object>;
  installUpdate(updatePackage: Object, installMode: number, minimumBackgroundDuration: number): Promise<void>;
  isFailedUpdate(packageHash: string): Promise<boolean>;
  isFirstRun(packageHash: string): Promise<boolean>;
  notifyApplicationReady(): Promise<void>;
  recordStatusReported(statusReport: Object): void;
  saveStatusReportForRetry(statusReport: Object): void;
  downloadAndReplaceCurrentBundle(remoteBundleUrl: string): void;
  clearUpdates(): void;
  getLatestRollbackInfo(): Promise<Object>;
  setLatestRollbackInfo(packageHash: string): Promise<void>;
  
  // Events
  addListener(eventName: string): void;
  removeListeners(count: number): void;
}

export default TurboModuleRegistry.get<Spec>('CodePush');
