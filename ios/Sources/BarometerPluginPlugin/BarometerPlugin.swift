import Foundation
import CoreMotion

/// Helper class that wraps CMAltimeter for barometric pressure readings.
/// Named `BarometerImpl` to avoid a naming conflict with the Capacitor
/// bridge class `BarometerPlugin` defined in BarometerPluginPlugin.swift.
@objc public class BarometerImpl: NSObject {

    private let altimeter = CMAltimeter()

    // MARK: - Availability

    /// Returns true when the device has a barometric sensor accessible
    /// via CMAltimeter.
    @objc public func isAvailable() -> Bool {
        return CMAltimeter.isRelativeAltitudeAvailable()
    }

    // MARK: - Updates

    /// Starts continuous altitude/pressure updates.
    ///
    /// - Parameters:
    ///   - samplingRate: Unused on iOS (CMAltimeter manages its own rate),
    ///     accepted for API symmetry with Android.
    ///   - handler: Called on the main queue for every new sample.
    ///     Parameters: `(pressureHPa: Double, relativeAltitude: Double,
    ///     timestampMs: Double, error: Error?)`.
    ///     `pressureHPa` is already converted from kPa × 10 → hPa.
    @objc public func startUpdates(
        samplingRate _: String?,
        handler: @escaping (Double, Double, Double, Error?) -> Void
    ) {
        altimeter.startRelativeAltitudeUpdates(to: .main) { data, error in
            if let error = error {
                handler(0, 0, 0, error)
                return
            }
            guard let data = data else { return }

            // CMAltitudeData.pressure is in kPa — multiply by 10 to get hPa.
            let pressureHPa = data.pressure.doubleValue * 10.0
            let relativeAltitude = data.relativeAltitude.doubleValue
            let timestampMs = Date().timeIntervalSince1970 * 1000.0

            handler(pressureHPa, relativeAltitude, timestampMs, nil)
        }
    }

    /// Stops any active altitude updates.
    @objc public func stopUpdates() {
        altimeter.stopRelativeAltitudeUpdates()
    }

    // MARK: - Calculation

    /// Computes estimated altitude in metres using the standard barometric formula.
    ///
    /// - Parameters:
    ///   - pressureHPa: Atmospheric pressure in hPa.
    ///   - seaLevelPressureHPa: Reference sea-level pressure in hPa (standard is 1013.25).
    @objc public func calculateAltitude(pressureHPa: Double, seaLevelPressureHPa: Double) -> Double {
        guard seaLevelPressureHPa > 0 && pressureHPa > 0 else { return 0.0 }
        return 44330.0 * (1.0 - pow(pressureHPa / seaLevelPressureHPa, 1.0 / 5.255))
    }
}
