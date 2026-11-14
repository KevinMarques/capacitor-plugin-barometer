package me.kevinmarques.capacitor.barometer;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Looper;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import java.util.ArrayList;
import java.util.List;

@CapacitorPlugin(name = "BarometerPlugin")
public class BarometerPlugin extends Plugin implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor barometerSensor;

    // Float.NaN signals "no reading received yet" (distinct from 0.0).
    private float currentPressure = Float.NaN;
    private long currentTimestampMs = 0;

    // Whether JS listeners are actively streaming events.
    private boolean isListening = false;
    // Remember listener state across pause/resume.
    private boolean wasListeningBeforePause = false;

    // One-shot getPressure() / getAltitude() support with queue.
    private final List<PluginCall> pendingPressureCalls = new ArrayList<>();
    private final Handler timeoutHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingTimeout = null;

    // Current sensor delay (configurable via startListening).
    private int currentSensorDelay = SensorManager.SENSOR_DELAY_NORMAL;

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    @Override
    public void load() {
        super.load();
        // Only obtain references — do NOT register the listener here.
        sensorManager = (SensorManager) getContext().getSystemService(Context.SENSOR_SERVICE);
        barometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
    }

    @Override
    public void handleOnPause() {
        super.handleOnPause();
        if (isListening || !pendingPressureCalls.isEmpty()) {
            wasListeningBeforePause = true;
            if (sensorManager != null) {
                sensorManager.unregisterListener(this);
            }
        } else {
            wasListeningBeforePause = false;
        }
    }

    @Override
    public void handleOnResume() {
        super.handleOnResume();
        if (wasListeningBeforePause && barometerSensor != null && sensorManager != null) {
            sensorManager.registerListener(this, barometerSensor, currentSensorDelay);
            wasListeningBeforePause = false;
        }
    }

    @Override
    public void handleOnDestroy() {
        super.handleOnDestroy();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        if (pendingTimeout != null) {
            timeoutHandler.removeCallbacks(pendingTimeout);
            pendingTimeout = null;
        }
        pendingPressureCalls.clear();
    }

    // -------------------------------------------------------------------------
    // SensorEventListener
    // -------------------------------------------------------------------------

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_PRESSURE) return;

        currentPressure = event.values[0]; // already in hPa
        currentTimestampMs = System.currentTimeMillis();

        float standardAltitude = SensorManager.getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE, currentPressure);

        // Resolve pending one-shot getPressure() and getAltitude() calls.
        if (!pendingPressureCalls.isEmpty()) {
            if (pendingTimeout != null) {
                timeoutHandler.removeCallbacks(pendingTimeout);
                pendingTimeout = null;
            }
            List<PluginCall> calls = new ArrayList<>(pendingPressureCalls);
            pendingPressureCalls.clear();

            if (!isListening && sensorManager != null) {
                sensorManager.unregisterListener(this);
            }

            for (PluginCall call : calls) {
                float callSeaLevel = getSeaLevelPressure(call);
                float altitude = SensorManager.getAltitude(callSeaLevel, currentPressure);

                JSObject ret = new JSObject();
                ret.put("pressure", currentPressure);
                ret.put("timestamp", currentTimestampMs);
                ret.put("altitude", altitude);
                ret.put("seaLevelPressure", callSeaLevel);
                call.resolve(ret);
            }
        }

        // Emit event to JS listeners.
        if (isListening) {
            JSObject ret = new JSObject();
            ret.put("pressure", currentPressure);
            ret.put("timestamp", currentTimestampMs);
            ret.put("altitude", standardAltitude);
            notifyListeners("pressureChange", ret);
            notifyListeners("onPressureChange", ret);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // No action needed.
    }

    // -------------------------------------------------------------------------
    // Plugin Methods
    // -------------------------------------------------------------------------

    @PluginMethod
    public void isAvailable(PluginCall call) {
        JSObject ret = new JSObject();
        ret.put("available", barometerSensor != null);
        call.resolve(ret);
    }

    @PluginMethod
    public void getPressure(PluginCall call) {
        if (barometerSensor == null) {
            call.unavailable("No barometer sensor found on this device");
            return;
        }

        float seaLevelPressure = getSeaLevelPressure(call);

        // If we already have a valid cached reading, return it immediately.
        if (!Float.isNaN(currentPressure)) {
            float altitude = SensorManager.getAltitude(seaLevelPressure, currentPressure);
            JSObject ret = new JSObject();
            ret.put("pressure", currentPressure);
            ret.put("timestamp", currentTimestampMs);
            ret.put("altitude", altitude);
            ret.put("seaLevelPressure", seaLevelPressure);
            call.resolve(ret);
            return;
        }

        // No reading yet — queue call and register listener if not already active.
        boolean needsRegistration = !isListening && pendingPressureCalls.isEmpty();
        pendingPressureCalls.add(call);
        if (needsRegistration) {
            sensorManager.registerListener(this, barometerSensor, currentSensorDelay);
        }

        schedulePendingTimeoutIfNeeded();
    }

    @PluginMethod
    public void getAltitude(PluginCall call) {
        float seaLevelPressure = getSeaLevelPressure(call);

        // If a specific pressure value was provided, compute directly:
        Double customPressure = call.getDouble("pressure");
        if (customPressure != null) {
            float pressure = customPressure.floatValue();
            float altitude = SensorManager.getAltitude(seaLevelPressure, pressure);
            JSObject ret = new JSObject();
            ret.put("altitude", altitude);
            ret.put("pressure", pressure);
            ret.put("seaLevelPressure", seaLevelPressure);
            call.resolve(ret);
            return;
        }

        if (barometerSensor == null) {
            call.unavailable("No barometer sensor found on this device");
            return;
        }

        // If cached reading exists, return immediately:
        if (!Float.isNaN(currentPressure)) {
            float altitude = SensorManager.getAltitude(seaLevelPressure, currentPressure);
            JSObject ret = new JSObject();
            ret.put("altitude", altitude);
            ret.put("pressure", currentPressure);
            ret.put("seaLevelPressure", seaLevelPressure);
            call.resolve(ret);
            return;
        }

        // Otherwise queue call to resolve once the first sample arrives:
        boolean needsRegistration = !isListening && pendingPressureCalls.isEmpty();
        pendingPressureCalls.add(call);
        if (needsRegistration) {
            sensorManager.registerListener(this, barometerSensor, currentSensorDelay);
        }

        schedulePendingTimeoutIfNeeded();
    }

    @PluginMethod
    public void startListening(PluginCall call) {
        if (barometerSensor == null) {
            call.unavailable("No barometer sensor found on this device");
            return;
        }

        String samplingRate = call.getString("samplingRate", "normal");
        int newDelay = resolveSamplingRate(samplingRate);

        if (!isListening || newDelay != currentSensorDelay) {
            currentSensorDelay = newDelay;
            sensorManager.registerListener(this, barometerSensor, currentSensorDelay);
            isListening = true;
        }

        call.resolve();
    }

    @PluginMethod
    public void stopListening(PluginCall call) {
        if (isListening) {
            isListening = false;
            // Keep the listener registered only if one-shot calls are pending.
            if (pendingPressureCalls.isEmpty() && sensorManager != null) {
                sensorManager.unregisterListener(this);
            }
        }
        call.resolve();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private float getSeaLevelPressure(PluginCall call) {
        Double seaLevel = call.getDouble("seaLevelPressure", (double) SensorManager.PRESSURE_STANDARD_ATMOSPHERE);
        return seaLevel != null ? seaLevel.floatValue() : SensorManager.PRESSURE_STANDARD_ATMOSPHERE;
    }

    private void schedulePendingTimeoutIfNeeded() {
        if (pendingTimeout == null) {
            pendingTimeout = () -> {
                if (pendingPressureCalls.isEmpty()) return;
                if (!isListening && sensorManager != null) {
                    sensorManager.unregisterListener(this);
                }
                List<PluginCall> timedOutCalls = new ArrayList<>(pendingPressureCalls);
                pendingPressureCalls.clear();
                pendingTimeout = null;
                for (PluginCall timedOutCall : timedOutCalls) {
                    timedOutCall.reject("Timeout: no pressure reading received within 5 seconds");
                }
            };
            timeoutHandler.postDelayed(pendingTimeout, 5000);
        }
    }

    public static int resolveSamplingRate(String rate) {
        if (rate == null) return SensorManager.SENSOR_DELAY_NORMAL;
        switch (rate) {
            case "ui":
                return SensorManager.SENSOR_DELAY_UI;
            case "game":
                return SensorManager.SENSOR_DELAY_GAME;
            case "fastest":
                return SensorManager.SENSOR_DELAY_FASTEST;
            default:
                return SensorManager.SENSOR_DELAY_NORMAL;
        }
    }
}
