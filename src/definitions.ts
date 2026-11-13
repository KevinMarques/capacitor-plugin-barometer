import type { PluginListenerHandle } from '@capacitor/core';

/**
 * Options for configuring pressure reading sampling rate.
 * @since 0.2.0
 */
export interface PressureOptions {
  /**
   * The desired sampling rate for pressure updates.
   *
   * Maps to Android `SensorManager` delay constants. iOS (`CMAltimeter`)
   * manages its own update rate independently of this setting.
   *
   * - `'normal'`   – ~200 ms (default, suitable for most apps)
   * - `'ui'`       – ~60 ms  (suitable for UI updates)
   * - `'game'`     – ~20 ms  (suitable for games / drones)
   * - `'fastest'`  – as fast as the hardware allows
   *
   * @since 0.2.0
   */
  samplingRate?: 'normal' | 'ui' | 'game' | 'fastest';
}

/**
 * Options for calculating altitude.
 * @since 0.2.0
 */
export interface AltitudeOptions {
  /**
   * Reference sea-level atmospheric pressure in **hPa** (standard is `1013.25`).
   *
   * Setting this to the current local sea-level pressure (QNH from a nearby airport
   * or weather station) provides an accurate absolute altitude instead of a standard
   * atmosphere estimate.
   *
   * @default 1013.25
   * @since 0.2.0
   */
  seaLevelPressure?: number;

  /**
   * Atmospheric pressure in **hPa** to convert into altitude.
   *
   * If omitted, the device's current barometric pressure reading is used.
   *
   * @since 0.2.0
   */
  pressure?: number;
}

/**
 * Result returned from {@link BarometerPlugin.getAltitude}.
 * @since 0.2.0
 */
export interface AltitudeData {
  /**
   * Estimated altitude in **metres** above sea level.
   *
   * @since 0.2.0
   */
  altitude: number;

  /**
   * Atmospheric pressure in **hPa** used for the calculation.
   *
   * @since 0.2.0
   */
  pressure: number;

  /**
   * Sea-level reference pressure in **hPa** used for the calculation.
   *
   * @since 0.2.0
   */
  seaLevelPressure: number;
}

/**
 * Data returned from a pressure reading or change event.
 * @since 0.1.0
 */
export interface PressureData {
  /**
   * Atmospheric pressure in **hPa** (hectopascals / millibars).
   *
   * Both Android (`Sensor.TYPE_PRESSURE`) and iOS (`CMAltitudeData.pressure`
   * converted from kPa × 10) report values in hPa so they are always
   * comparable across platforms.
   *
   * @since 0.1.0
   */
  pressure: number;

  /**
   * Unix timestamp in **milliseconds** of when the reading was taken.
   *
   * @since 0.2.0
   */
  timestamp: number;

  /**
   * Estimated altitude in **metres** derived from the barometric pressure.
   *
   * - **Android**: computed via `SensorManager.getAltitude(seaLevelPressure, pressure)`
   *   (standard atmosphere model by default).
   * - **iOS**: `CMAltitudeData.relativeAltitude` — relative to the device's
   *   location when `CMAltimeter` was started.
   *
   * @since 0.2.0
   */
  altitude: number;
}

/**
 * @deprecated Use {@link PressureData} instead.
 */
export type PressureChangeEvent = PressureData;

export interface BarometerPlugin {
  /**
   * Checks whether a barometric pressure sensor is available on this device.
   *
   * Returns `{ available: false }` on the web (no browser API exists for
   * barometric pressure).
   *
   * @since 0.1.0
   */
  isAvailable(): Promise<{ available: boolean }>;

  /**
   * Returns a single pressure reading.
   *
   * On Android, if no reading has been cached yet the plugin registers a
   * temporary sensor listener, waits for the first sample (up to 5 s), then
   * unregisters it automatically.
   *
   * Rejects with an error when the sensor is unavailable or the 5-second
   * timeout expires before the first sample arrives.
   *
   * @param options Optional settings such as `seaLevelPressure` (hPa / QNH) to compute
   *   calibrated altitude.
   * @returns A {@link PressureData} object with `pressure` (hPa),
   *   `timestamp` (ms) and `altitude` (m).
   * @since 0.1.0
   */
  getPressure(options?: { seaLevelPressure?: number }): Promise<PressureData>;

  /**
   * Computes altitude in metres from barometric pressure.
   *
   * If `pressure` is provided in `options`, calculates the altitude mathematically
   * from that value (available on all platforms, including web).
   * Otherwise, reads the current pressure from the device's sensor.
   *
   * Pass `seaLevelPressure` (local QNH) to obtain precise elevation above sea level
   * adjusted for current meteorological conditions.
   *
   * @param options Optional {@link AltitudeOptions} with `seaLevelPressure` and/or `pressure`.
   * @returns An {@link AltitudeData} object with `altitude` (m), `pressure` (hPa), and `seaLevelPressure` (hPa).
   * @since 0.2.0
   */
  getAltitude(options?: AltitudeOptions): Promise<AltitudeData>;

  /**
   * Starts continuous pressure updates.
   *
   * After calling this method the plugin emits `pressureChange` events until
   * {@link stopListening} is called. Has no effect if already listening.
   *
   * On Android the sensor is activated only while listening, preserving
   * battery. The sensor is automatically paused when the app goes to
   * background and resumed on foreground.
   *
   * @param options Optional {@link PressureOptions} to configure sampling rate.
   * @since 0.2.0
   */
  startListening(options?: PressureOptions): Promise<void>;

  /**
   * Stops continuous pressure updates started by {@link startListening}.
   *
   * Has no effect if not currently listening.
   *
   * @since 0.2.0
   */
  stopListening(): Promise<void>;

  /**
   * Adds a listener for `pressureChange` events.
   *
   * Call {@link startListening} first to begin receiving events.
   *
   * @param eventName `'pressureChange'`
   * @param listenerFunc Callback receiving a {@link PressureData} object.
   * @since 0.1.0
   */
  addListener(
    eventName: 'pressureChange',
    listenerFunc: (event: PressureData) => void,
  ): Promise<PluginListenerHandle> & PluginListenerHandle;

  /**
   * @deprecated Use `addListener('pressureChange', ...)` instead.
   */
  addListener(
    eventName: 'onPressureChange',
    listenerFunc: (event: PressureData) => void,
  ): Promise<PluginListenerHandle> & PluginListenerHandle;

  /**
   * Removes all listeners for this plugin.
   *
   * @since 0.2.0
   */
  removeAllListeners(): Promise<void>;
}
