package me.kevinmarques.capacitor.barometer;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

@CapacitorPlugin(name = "BarometerPlugin")
public class BarometerPlugin extends Plugin implements SensorEventListener {
    private SensorManager sensorManager;
    private Sensor barometerSensor;
    private float currentPressure;

    @Override
    public void load() {
        super.load();

        sensorManager = (SensorManager) getContext().getSystemService(Context.SENSOR_SERVICE);
        barometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);

        if (barometerSensor != null) {
            sensorManager.registerListener(this, barometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    public void handleOnDestroy() {
        super.handleOnDestroy();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_PRESSURE) {
            currentPressure = event.values[0]; // hPa

            JSObject ret = new JSObject();
            ret.put("pressure", currentPressure);
            notifyListeners("onPressureChange", ret);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Manage pressure changes
    }

    @PluginMethod
    public void isAvailable(PluginCall call) {
        JSObject ret = new JSObject();
        ret.put("available", barometerSensor != null);
        call.resolve(ret);
    }

    @PluginMethod
    public void getPressure(PluginCall call) {
        if (barometerSensor == null) {
            call.unavailable("No barometer sensor found");
        } else {
            // Return the last pressure reading
            JSObject ret = new JSObject();
            ret.put("pressure", currentPressure);
            call.resolve(ret);
        }
    }
}
