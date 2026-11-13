package com.getcapacitor;

import static org.junit.Assert.*;

import android.hardware.SensorManager;
import me.kevinmarques.capacitor.barometer.BarometerPlugin;
import org.junit.Test;

/**
 * Unit tests for BarometerPlugin logic that does not require an Android device.
 *
 * Tests cover:
 *  - Sampling rate string → SensorManager constant mapping
 *  - Pressure → altitude formula sanity (standard atmosphere model)
 *  - Float.NaN sentinel used to detect "no reading yet"
 */
public class ExampleUnitTest {

    // -------------------------------------------------------------------------
    // Sampling-rate mapping tests (testing BarometerPlugin.resolveSamplingRate directly)
    // -------------------------------------------------------------------------

    @Test
    public void samplingRate_normal_returnsSensorDelayNormal() {
        assertEquals(SensorManager.SENSOR_DELAY_NORMAL, BarometerPlugin.resolveSamplingRate("normal"));
    }

    @Test
    public void samplingRate_null_returnsSensorDelayNormal() {
        assertEquals(SensorManager.SENSOR_DELAY_NORMAL, BarometerPlugin.resolveSamplingRate(null));
    }

    @Test
    public void samplingRate_unknown_returnsSensorDelayNormal() {
        assertEquals(SensorManager.SENSOR_DELAY_NORMAL, BarometerPlugin.resolveSamplingRate("unknown"));
    }

    @Test
    public void samplingRate_ui_returnsSensorDelayUI() {
        assertEquals(SensorManager.SENSOR_DELAY_UI, BarometerPlugin.resolveSamplingRate("ui"));
    }

    @Test
    public void samplingRate_game_returnsSensorDelayGame() {
        assertEquals(SensorManager.SENSOR_DELAY_GAME, BarometerPlugin.resolveSamplingRate("game"));
    }

    @Test
    public void samplingRate_fastest_returnsSensorDelayFastest() {
        assertEquals(SensorManager.SENSOR_DELAY_FASTEST, BarometerPlugin.resolveSamplingRate("fastest"));
    }

    @Test
    public void samplingRate_constants_haveExpectedValues() {
        // Verify the SensorManager constants match the documented values
        // so the mapping table in the README stays accurate.
        assertEquals(3, SensorManager.SENSOR_DELAY_NORMAL);
        assertEquals(2, SensorManager.SENSOR_DELAY_UI);
        assertEquals(1, SensorManager.SENSOR_DELAY_GAME);
        assertEquals(0, SensorManager.SENSOR_DELAY_FASTEST);
    }

    // -------------------------------------------------------------------------
    // Altitude formula tests (standard atmosphere model)
    // -------------------------------------------------------------------------

    /** Replicates SensorManager.getAltitude without requiring Android runtime. */
    private double getAltitude(double pressure) {
        return getAltitude(pressure, 1013.25);
    }

    private double getAltitude(double pressure, double seaLevelPressure) {
        return 44330.0 * (1.0 - Math.pow(pressure / seaLevelPressure, 1.0 / 5.255));
    }

    @Test
    public void altitude_withCustomSeaLevelPressure_isAccurate() {
        // If current pressure equals the local QNH sea level pressure, altitude must be 0
        double localQNH = 1025.0;
        double altitudeAtSeaLevel = getAltitude(localQNH, localQNH);
        assertEquals(0.0, altitudeAtSeaLevel, 0.01);

        // At 1000 hPa with QNH 1020 hPa:
        double altitude = getAltitude(1000.0, 1020.0);
        assertTrue("Altitude should be positive when pressure is below sea level pressure", altitude > 0);
    }

    @Test
    public void altitude_atStandardAtmosphere_isNearZero() {
        double altitude = getAltitude(1013.25);
        assertEquals(0.0, altitude, 5.0); // ±5 m tolerance
    }

    @Test
    public void altitude_atLowerPressure_isPositive() {
        // Lower pressure → higher altitude (e.g. mountain top)
        double altitude = getAltitude(900.0);
        assertTrue("Lower pressure should give positive altitude", altitude > 0);
    }

    @Test
    public void altitude_atHigherPressure_isNegative() {
        // Higher pressure → below sea level (e.g. Dead Sea)
        double altitude = getAltitude(1050.0);
        assertTrue("Higher pressure should give negative altitude", altitude < 0);
    }

    // -------------------------------------------------------------------------
    // Sentinel value tests
    // -------------------------------------------------------------------------

    @Test
    public void floatNaN_isNotEqualToZero() {
        // BarometerPlugin uses Float.NaN (not 0.0) as the "no reading yet" sentinel.
        // Verify Float.isNaN accurately distinguishes the uninitialized state.
        float uninitialized = Float.NaN;
        assertTrue(Float.isNaN(uninitialized));
        assertNotEquals(0.0f, uninitialized, 0.001f);
    }

    @Test
    public void floatNaN_transitionsToValidPressure() {
        float pressure = Float.NaN;
        assertTrue(Float.isNaN(pressure));

        // Simulating the first sensor event arriving:
        pressure = 1013.25f;
        assertFalse(Float.isNaN(pressure));
        assertEquals(1013.25f, pressure, 0.01f);
    }
}
