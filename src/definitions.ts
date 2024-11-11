import type { PluginListenerHandle } from '@capacitor/core';

export interface BarometerPlugin {
  /**
   * Checks if the barometer sensor is available.
   *
   * @since 0.1.0
   */
  isAvailable(): Promise<{ available: boolean }>;

  /**
   * Gets the current pressure.
   *
   * @since 0.1.0
   */
  getPressure(): Promise<{ pressure: number }>;

  /**
   * Adds a listener for the pressure change event.
   * This event is emitted whenever the barometric pressure changes.
   *
   * @param eventName - Name of the event to listen for (`"onPressureChange"`).
   * @param listenerFunc - Callback function that receives the pressure data.
   * @returns A handle to remove the listener when not needed.
   */
  addListener(
    eventName: 'onPressureChange',
    listenerFunc: (event: PressureChangeEvent) => void,
  ): Promise<PluginListenerHandle> & PluginListenerHandle;
}

export interface PressureChangeEvent {
  pressure: number;
}
