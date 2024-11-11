package me.kevinmarques.capacitor.barometer;

import android.util.Log;

public class BarometerPlugin {

    public String echo(String value) {
        Log.i("Echo", value);
        return value;
    }
}
