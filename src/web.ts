import { WebPlugin } from '@capacitor/core';
import type { PluginListenerHandle } from '@capacitor/core';

import type { AltitudeData, AltitudeOptions, BarometerPlugin, PressureData, PressureOptions } from './definitions';

/**
 * Web fallback for BarometerPlugin.
 *
 * Barometric pressure requires native hardware; no standard browser API provides it.
 * This implementation prevents "does not have web implementation" errors
 * during `ionic serve` or end-to-end testing on the web.
 */
export class BarometerWeb extends WebPlugin implements BarometerPlugin {
  async isAvailable(): Promise<{ available: boolean }> {
    return { available: false };
  }

  async getPressure(_options?: { seaLevelPressure?: number }): Promise<PressureData> {
    throw this.unavailable(
      'The Barometer plugin is not available on the web. ' + 'Barometric pressure requires native device hardware.',
    );
  }

  async getAltitude(options?: AltitudeOptions): Promise<AltitudeData> {
    const seaLevelPressure = options?.seaLevelPressure ?? 1013.25;
    if (options?.pressure !== undefined) {
      const pressure = options.pressure;
      const altitude = 44330.0 * (1.0 - Math.pow(pressure / seaLevelPressure, 1.0 / 5.255));
      return {
        altitude,
        pressure,
        seaLevelPressure,
      };
    }
    throw this.unavailable(
      'The Barometer plugin is not available on the web. ' +
        'Pass { pressure: ... } in options to compute altitude mathematically from a known reading.',
    );
  }

  async startListening(_options?: PressureOptions): Promise<void> {
    throw this.unavailable('The Barometer plugin is not available on the web.');
  }

  async stopListening(): Promise<void> {
    throw this.unavailable('The Barometer plugin is not available on the web.');
  }

  // addListener is inherited from WebPlugin which returns Promise<PluginListenerHandle>.
  // The interface requires the intersection type Promise<Handle> & Handle;
  // we cast here following the same pattern used in official Capacitor plugins.
  addListener(
    eventName: 'pressureChange' | 'onPressureChange',
    listenerFunc: (event: PressureData) => void,
  ): Promise<PluginListenerHandle> & PluginListenerHandle {
    return super.addListener(eventName, listenerFunc) as Promise<PluginListenerHandle> & PluginListenerHandle;
  }
}
