import { registerPlugin } from '@capacitor/core';

import type { BarometerPlugin as IBarometerPlugin } from './definitions';

/**
 * The Barometer plugin instance.
 *
 * @example
 * ```typescript
 * import { Barometer } from '@kevinmarques/capacitor-barometer';
 *
 * const { available } = await Barometer.isAvailable();
 * if (available) {
 *   const data = await Barometer.getPressure();
 *   console.log(`${data.pressure} hPa @ ${data.altitude} m`);
 * }
 * ```
 */
export const Barometer = registerPlugin<IBarometerPlugin>('BarometerPlugin', {
  web: () => import('./web').then((m) => new m.BarometerWeb()),
});

/**
 * @deprecated Use {@link Barometer} instead.
 */
export const BarometerPlugin = Barometer;

export * from './definitions';
export * from './web';
