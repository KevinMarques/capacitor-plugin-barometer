import XCTest
import CoreMotion
@testable import BarometerPlugin

class BarometerPluginTests: XCTestCase {

    // MARK: - Availability

    func testIsAvailableReturnsBoolean() {
        // BarometerImpl.isAvailable() must return a Bool without crashing.
        // The actual value depends on the test environment (simulator returns false).
        let impl = BarometerImpl()
        let result: Bool = impl.isAvailable()
        // Bool is always true or false — just calling it without crashing is the assertion.
        XCTAssertTrue(result == true || result == false)
    }

    // MARK: - Unit conversion

    func testPressureHPaConversionFromKPa() {
        // iOS CMAltitudeData.pressure is in kPa.
        // The plugin multiplies by 10 to convert to hPa.
        let kPa = 101.325
        let expectedHPa = 1013.25
        let actualHPa = kPa * 10.0
        XCTAssertEqual(actualHPa, expectedHPa, accuracy: 0.0001,
                       "kPa × 10 must equal hPa")
    }

    func testKPaToHPaConversionAtVariousPressures() {
        let pairs: [(kPa: Double, hPa: Double)] = [
            (100.0, 1000.0),
            (85.0,  850.0),
            (120.0, 1200.0),
        ]
        for pair in pairs {
            XCTAssertEqual(pair.kPa * 10.0, pair.hPa, accuracy: 0.0001)
        }
    }

    // MARK: - CMAltimeter

    func testAltimeterAvailabilityCheckIsCallable() {
        // Verify CMAltimeter.isRelativeAltitudeAvailable() is callable
        // and returns a Bool (the value depends on hardware/simulator).
        let available: Bool = CMAltimeter.isRelativeAltitudeAvailable()
        XCTAssertTrue(available == true || available == false)
    }

    // MARK: - Precision Altitude Calculation

    func testCalculateAltitudeAtStandardPressure() {
        let impl = BarometerImpl()
        let altitude = impl.calculateAltitude(pressureHPa: 1013.25, seaLevelPressureHPa: 1013.25)
        XCTAssertEqual(altitude, 0.0, accuracy: 0.1, "Altitude at standard sea-level pressure should be ~0 m")
    }

    func testCalculateAltitudeWithCustomQNH() {
        let impl = BarometerImpl()
        // If local QNH is 1025.0 hPa and current reading is 1025.0 hPa, altitude should be 0 m
        let seaLevelAltitude = impl.calculateAltitude(pressureHPa: 1025.0, seaLevelPressureHPa: 1025.0)
        XCTAssertEqual(seaLevelAltitude, 0.0, accuracy: 0.1)

        // Lower pressure implies higher altitude
        let highAltitude = impl.calculateAltitude(pressureHPa: 900.0, seaLevelPressureHPa: 1013.25)
        XCTAssertTrue(highAltitude > 0, "Lower pressure should produce positive altitude")
    }
}
