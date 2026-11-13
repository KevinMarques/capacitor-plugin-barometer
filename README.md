# @kevinmarques/capacitor-barometer

A Capacitor plugin that accesses the device's barometric pressure sensor, providing real-time atmospheric pressure data for weather prediction, altitude tracking, or pressure-based triggers.

**Supported platforms:** Android ✅ · iOS ✅ · Web ⚠️ (graceful fallback — `isAvailable()` returns `false`)

## Install

```bash
npm install @kevinmarques/capacitor-barometer
npx cap sync
```

## iOS Setup — NSMotionUsageDescription (required)

iOS requires a privacy description for motion/sensor access. Add the following key to your app's `Info.plist`:

```xml
<key>NSMotionUsageDescription</key>
<string>This app uses the barometric pressure sensor to track altitude and atmospheric conditions.</string>
```

Without this entry the app will crash on iOS when the plugin tries to access `CMAltimeter`.

## Android Setup

No additional permissions are required. The barometric pressure sensor (`Sensor.TYPE_PRESSURE`) is accessible without declaring any `<uses-permission>` in `AndroidManifest.xml`. To advertise that your app requires a barometer, add:

```xml
<!-- Optional: declare hardware requirement -->
<uses-feature android:name="android.hardware.sensor.barometer" android:required="true" />
```

## Units of Measurement

| Platform | Native unit | Plugin output |
|----------|------------|---------------|
| Android  | hPa (already) | **hPa** |
| iOS      | kPa (× 10 conversion applied internally) | **hPa** |

All values returned by this plugin are in **hPa (hectopascals / millibars)**, regardless of platform.

## Quick Start

```typescript
import { Barometer } from '@kevinmarques/capacitor-barometer';

// Check availability
const { available } = await Barometer.isAvailable();
if (!available) {
  console.warn('Barometer not available on this device');
  return;
}

// One-shot reading
const data = await Barometer.getPressure();
console.log(`Pressure:  ${data.pressure} hPa`);
console.log(`Altitude:  ${data.altitude} m`);
console.log(`Timestamp: ${new Date(data.timestamp).toISOString()}`);

// Continuous updates
await Barometer.startListening({ samplingRate: 'normal' });

const listener = await Barometer.addListener('pressureChange', (data) => {
  console.log(`Pressure: ${data.pressure} hPa @ ${data.altitude} m`);
});

// Later, stop listening
listener.remove();
await Barometer.stopListening();
```

## Precision Altitude (QNH Calibration)

By default, altitude is estimated using the standard atmospheric model ($1013.25\text{ hPa}$ at sea level). Natural meteorological variations cause actual sea-level pressure to fluctuate (typically between 990 and 1035 hPa), which can introduce 50–200 m of error in uncalibrated barometric elevation.

For high-precision altimeter applications (mountaineering, aviation, weather tracking), provide the local **QNH** (current sea-level pressure from a nearby meteorological station):

```typescript
// High-accuracy altitude calibrated with local QNH:
const localQNH = 1022.5; // hPa, from a local weather API or METAR report
const { altitude, pressure } = await Barometer.getAltitude({ seaLevelPressure: localQNH });
console.log(`Calibrated altitude: ${altitude.toFixed(1)} m`);

// Or compute altitude from a known pressure value (works on web too without hardware):
const result = await Barometer.getAltitude({
  pressure: 985.0,
  seaLevelPressure: 1020.0,
});
console.log(`Altitude: ${result.altitude.toFixed(1)} m`);
```

## Sampling Rates

The `samplingRate` option in `startListening()` controls Android's sensor update frequency.
iOS (`CMAltimeter`) manages its own update rate independently of this setting.

| Value | Approx. interval | Use case |
|-------|-----------------|----------|
| `'normal'` | ~200 ms (default) | General use, weather apps |
| `'ui'` | ~60 ms | UI updates |
| `'game'` | ~20 ms | Games, drones |
| `'fastest'` | Hardware max | High-frequency logging |

## API

<docgen-index>

* [`isAvailable()`](#isavailable)
* [`getPressure(...)`](#getpressure)
* [`getAltitude(...)`](#getaltitude)
* [`startListening(...)`](#startlistening)
* [`stopListening()`](#stoplistening)
* [`addListener('pressureChange', ...)`](#addlistenerpressurechange-)
* [`addListener('onPressureChange', ...)`](#addlisteneronpressurechange-)
* [`removeAllListeners()`](#removealllisteners)
* [Interfaces](#interfaces)

</docgen-index>

<docgen-api>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

### isAvailable()

```typescript
isAvailable() => Promise<{ available: boolean; }>
```

Checks whether a barometric pressure sensor is available on this device.

Returns `{ available: false }` on the web (no browser API exists for
barometric pressure).

**Returns:** <code>Promise&lt;{ available: boolean; }&gt;</code>

**Since:** 0.1.0

--------------------


### getPressure(...)

```typescript
getPressure(options?: { seaLevelPressure?: number | undefined; } | undefined) => Promise<PressureData>
```

Returns a single pressure reading.

On Android, if no reading has been cached yet the plugin registers a
temporary sensor listener, waits for the first sample (up to 5 s), then
unregisters it automatically.

Rejects with an error when the sensor is unavailable or the 5-second
timeout expires before the first sample arrives.

| Param         | Type                                        | Description                                                                              |
| ------------- | ------------------------------------------- | ---------------------------------------------------------------------------------------- |
| **`options`** | <code>{ seaLevelPressure?: number; }</code> | Optional settings such as `seaLevelPressure` (hPa / QNH) to compute calibrated altitude. |

**Returns:** <code>Promise&lt;<a href="#pressuredata">PressureData</a>&gt;</code>

**Since:** 0.1.0

--------------------


### getAltitude(...)

```typescript
getAltitude(options?: AltitudeOptions | undefined) => Promise<AltitudeData>
```

Computes altitude in metres from barometric pressure.

If `pressure` is provided in `options`, calculates the altitude mathematically
from that value (available on all platforms, including web).
Otherwise, reads the current pressure from the device's sensor.

Pass `seaLevelPressure` (local QNH) to obtain precise elevation above sea level
adjusted for current meteorological conditions.

| Param         | Type                                                        | Description                                                                                                |
| ------------- | ----------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------- |
| **`options`** | <code><a href="#altitudeoptions">AltitudeOptions</a></code> | Optional {@link <a href="#altitudeoptions">AltitudeOptions</a>} with `seaLevelPressure` and/or `pressure`. |

**Returns:** <code>Promise&lt;<a href="#altitudedata">AltitudeData</a>&gt;</code>

**Since:** 0.2.0

--------------------


### startListening(...)

```typescript
startListening(options?: PressureOptions | undefined) => Promise<void>
```

Starts continuous pressure updates.

After calling this method the plugin emits `pressureChange` events until
{@link stopListening} is called. Has no effect if already listening.

On Android the sensor is activated only while listening, preserving
battery. The sensor is automatically paused when the app goes to
background and resumed on foreground.

| Param         | Type                                                        | Description                                                                                 |
| ------------- | ----------------------------------------------------------- | ------------------------------------------------------------------------------------------- |
| **`options`** | <code><a href="#pressureoptions">PressureOptions</a></code> | Optional {@link <a href="#pressureoptions">PressureOptions</a>} to configure sampling rate. |

**Since:** 0.2.0

--------------------


### stopListening()

```typescript
stopListening() => Promise<void>
```

Stops continuous pressure updates started by {@link startListening}.

Has no effect if not currently listening.

**Since:** 0.2.0

--------------------


### addListener('pressureChange', ...)

```typescript
addListener(eventName: 'pressureChange', listenerFunc: (event: PressureData) => void) => Promise<PluginListenerHandle> & PluginListenerHandle
```

Adds a listener for `pressureChange` events.

Call {@link startListening} first to begin receiving events.

| Param              | Type                                                                      | Description                                                                   |
| ------------------ | ------------------------------------------------------------------------- | ----------------------------------------------------------------------------- |
| **`eventName`**    | <code>'pressureChange'</code>                                             | `'pressureChange'`                                                            |
| **`listenerFunc`** | <code>(event: <a href="#pressuredata">PressureData</a>) =&gt; void</code> | Callback receiving a {@link <a href="#pressuredata">PressureData</a>} object. |

**Returns:** <code>Promise&lt;<a href="#pluginlistenerhandle">PluginListenerHandle</a>&gt; & <a href="#pluginlistenerhandle">PluginListenerHandle</a></code>

**Since:** 0.1.0

--------------------


### addListener('onPressureChange', ...)

```typescript
addListener(eventName: 'onPressureChange', listenerFunc: (event: PressureData) => void) => Promise<PluginListenerHandle> & PluginListenerHandle
```

| Param              | Type                                                                      |
| ------------------ | ------------------------------------------------------------------------- |
| **`eventName`**    | <code>'onPressureChange'</code>                                           |
| **`listenerFunc`** | <code>(event: <a href="#pressuredata">PressureData</a>) =&gt; void</code> |

**Returns:** <code>Promise&lt;<a href="#pluginlistenerhandle">PluginListenerHandle</a>&gt; & <a href="#pluginlistenerhandle">PluginListenerHandle</a></code>

--------------------


### removeAllListeners()

```typescript
removeAllListeners() => Promise<void>
```

Removes all listeners for this plugin.

**Since:** 0.2.0

--------------------


### Interfaces


#### PressureData

Data returned from a pressure reading or change event.

| Prop            | Type                | Description                                                                                                                                                                                                                                                                                                         | Since |
| --------------- | ------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----- |
| **`pressure`**  | <code>number</code> | Atmospheric pressure in **hPa** (hectopascals / millibars). Both Android (`Sensor.TYPE_PRESSURE`) and iOS (`CMAltitudeData.pressure` converted from kPa × 10) report values in hPa so they are always comparable across platforms.                                                                                  | 0.1.0 |
| **`timestamp`** | <code>number</code> | Unix timestamp in **milliseconds** of when the reading was taken.                                                                                                                                                                                                                                                   | 0.2.0 |
| **`altitude`**  | <code>number</code> | Estimated altitude in **metres** derived from the barometric pressure. - **Android**: computed via `SensorManager.getAltitude(seaLevelPressure, pressure)` (standard atmosphere model by default). - **iOS**: `CMAltitudeData.relativeAltitude` — relative to the device's location when `CMAltimeter` was started. | 0.2.0 |


#### AltitudeData

Result returned from {@link BarometerPlugin.getAltitude}.

| Prop                   | Type                | Description                                                       | Since |
| ---------------------- | ------------------- | ----------------------------------------------------------------- | ----- |
| **`altitude`**         | <code>number</code> | Estimated altitude in **metres** above sea level.                 | 0.2.0 |
| **`pressure`**         | <code>number</code> | Atmospheric pressure in **hPa** used for the calculation.         | 0.2.0 |
| **`seaLevelPressure`** | <code>number</code> | Sea-level reference pressure in **hPa** used for the calculation. | 0.2.0 |


#### AltitudeOptions

Options for calculating altitude.

| Prop                   | Type                | Description                                                                                                                                                                                                                                                        | Default              | Since |
| ---------------------- | ------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | -------------------- | ----- |
| **`seaLevelPressure`** | <code>number</code> | Reference sea-level atmospheric pressure in **hPa** (standard is `1013.25`). Setting this to the current local sea-level pressure (QNH from a nearby airport or weather station) provides an accurate absolute altitude instead of a standard atmosphere estimate. | <code>1013.25</code> | 0.2.0 |
| **`pressure`**         | <code>number</code> | Atmospheric pressure in **hPa** to convert into altitude. If omitted, the device's current barometric pressure reading is used.                                                                                                                                    |                      | 0.2.0 |


#### PressureOptions

Options for configuring pressure reading sampling rate.

| Prop               | Type                                                 | Description                                                                                                                                                                                                                                                                                                                                                                           | Since |
| ------------------ | ---------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----- |
| **`samplingRate`** | <code>'normal' \| 'ui' \| 'game' \| 'fastest'</code> | The desired sampling rate for pressure updates. Maps to Android `SensorManager` delay constants. iOS (`CMAltimeter`) manages its own update rate independently of this setting. - `'normal'` – ~200 ms (default, suitable for most apps) - `'ui'` – ~60 ms (suitable for UI updates) - `'game'` – ~20 ms (suitable for games / drones) - `'fastest'` – as fast as the hardware allows | 0.2.0 |


#### PluginListenerHandle

| Prop         | Type                                      |
| ------------ | ----------------------------------------- |
| **`remove`** | <code>() =&gt; Promise&lt;void&gt;</code> |

</docgen-api>

## Platform Notes

### Android
- Sensor is activated **on demand** only (when `startListening()` is called or `getPressure()` is awaited), preserving battery.
- The sensor is **paused automatically** when the app goes to background and **resumed** on foreground.
- If `getPressure()` is called before the first sensor event, the plugin waits up to **5 seconds** for the initial reading before rejecting with a timeout error.
- Altitude is calculated with `SensorManager.getAltitude(1013.25, pressure)` (standard atmosphere model).

### iOS
- Uses `CMAltimeter` from the `CoreMotion` framework.
- Requires `NSMotionUsageDescription` in `Info.plist` (see [iOS Setup](#ios-setup--nsmotionusagedescription-required) above).
- Pressure is converted from kPa → hPa (`× 10`) automatically.
- `altitude` reflects **relative altitude** since the altimeter was started, not an absolute elevation.

### Web
- `isAvailable()` returns `{ available: false }`.
- `getPressure()`, `startListening()`, and `stopListening()` throw an `UnavailableException`.
- No browser APIs for barometric pressure exist at this time.

## Changelog

### 0.2.0
- **iOS**: Full CoreMotion (`CMAltimeter`) implementation replacing the placeholder template.
- **Android**: On-demand sensor activation; `handleOnPause`/`handleOnResume` lifecycle; `getPressure()` race-condition fix with 5-second timeout; configurable `samplingRate`.
- **New fields**: `timestamp` (ms) and `altitude` (m) in all pressure data objects.
- **New methods**: `startListening(options?)` and `stopListening()`.
- **Web**: Graceful fallback — no more `does not have web implementation` error.
- **Event rename**: `pressureChange` (was `onPressureChange`; old name still accepted for backwards compatibility).
- **Export rename**: `Barometer` (was `BarometerPlugin`; old name still exported as alias).
- **Fix**: README install command now shows the correct scoped package name `@kevinmarques/capacitor-barometer`.
- **CI**: Added `.github/workflows/verify.yml` for automated verification on pull requests.
