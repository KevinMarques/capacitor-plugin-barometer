# capacitor-barometer

A Capacitor plugin that allows you to access barometric pressure data from the device's sensors. This plugin provides real-time updates on atmospheric pressure and enables your app to make weather predictions, track altitude changes, or create pressure-based triggers.

## Install

```bash
npm install capacitor-barometer
npx cap sync
```

## API

<docgen-index>

* [`isAvailable()`](#isavailable)
* [`getPressure()`](#getpressure)
* [`addListener('onPressureChange', ...)`](#addlisteneronpressurechange-)
* [Interfaces](#interfaces)

</docgen-index>

<docgen-api>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

### isAvailable()

```typescript
isAvailable() => Promise<{ available: boolean; }>
```

Checks if the barometer sensor is available.

**Returns:** <code>Promise&lt;{ available: boolean; }&gt;</code>

**Since:** 0.1.0

--------------------


### getPressure()

```typescript
getPressure() => Promise<{ pressure: number; }>
```

Gets the current pressure.

**Returns:** <code>Promise&lt;{ pressure: number; }&gt;</code>

**Since:** 0.1.0

--------------------


### addListener('onPressureChange', ...)

```typescript
addListener(eventName: 'onPressureChange', listenerFunc: (event: PressureChangeEvent) => void) => Promise<PluginListenerHandle> & PluginListenerHandle
```

Adds a listener for the pressure change event.
This event is emitted whenever the barometric pressure changes.

| Param              | Type                                                                                    | Description                                               |
| ------------------ | --------------------------------------------------------------------------------------- | --------------------------------------------------------- |
| **`eventName`**    | <code>'onPressureChange'</code>                                                         | - Name of the event to listen for (`"onPressureChange"`). |
| **`listenerFunc`** | <code>(event: <a href="#pressurechangeevent">PressureChangeEvent</a>) =&gt; void</code> | - Callback function that receives the pressure data.      |

**Returns:** <code>Promise&lt;<a href="#pluginlistenerhandle">PluginListenerHandle</a>&gt; & <a href="#pluginlistenerhandle">PluginListenerHandle</a></code>

--------------------


### Interfaces


#### PluginListenerHandle

| Prop         | Type                                      |
| ------------ | ----------------------------------------- |
| **`remove`** | <code>() =&gt; Promise&lt;void&gt;</code> |


#### PressureChangeEvent

| Prop           | Type                |
| -------------- | ------------------- |
| **`pressure`** | <code>number</code> |

</docgen-api>
