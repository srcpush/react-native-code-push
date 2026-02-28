import type { TurboModule } from 'react-native';
import { TurboModuleRegistry } from 'react-native';

export interface Spec extends TurboModule {
  showDialog(
    title: string | null,
    message: string | null,
    button1Text: string | null,
    button2Text: string | null,
    successCallback: (buttonId: number) => void,
    errorCallback: (error: string) => void,
  ): void;

  addListener(eventName: string): void;
  removeListeners(count: number): void;
}

export default TurboModuleRegistry.getEnforcing<Spec>('CodePushDialog');

