import Foundation
import UIKit
import Capacitor

/**
 * Capacitor plugin bridge for barometric pressure readings.
 *
 * Uses `CMAltimeter` (CoreMotion) on iOS.
 * iOS returns pressure in kPa; this bridge converts to hPa (× 10) so the
 * values match Android's `Sensor.TYPE_PRESSURE` output.
 *
 * **Required in Info.plist:**
 * ```xml
 * <key>NSMotionUsageDescription</key>
 * <string>Used for barometric pressure and altitude readings.</string>
 * ```
 */
@objc(BarometerPlugin)
public class BarometerPlugin: CAPPlugin, CAPBridgedPlugin {

    public let identifier = "BarometerPlugin"
    public let jsName = "BarometerPlugin"
    public let pluginMethods: [CAPPluginMethod] = [
        CAPPluginMethod(name: "isAvailable", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getPressure", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getAltitude", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "startListening", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "stopListening", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "removeAllListeners", returnType: CAPPluginReturnPromise)
    ]

    private let impl = BarometerImpl()

    // Tracks whether we are currently streaming events to JS listeners.
    private var isListening = false
    // Remember listener state across app background/foreground.
    private var wasListeningBeforeBackground = false

    // Cached last reading
    private var lastPressure: Double?
    private var lastAltitude: Double?
    private var lastTimestampMs: Double?

    // Pending one-shot getPressure and getAltitude calls waiting for first reading
    private var pendingCalls: [CAPPluginCall] = []
    private var pendingTimeoutItem: DispatchWorkItem?

    // MARK: - Lifecycle

    override public func load() {
        super.load()
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(handleDidEnterBackground),
            name: UIApplication.didEnterBackgroundNotification,
            object: nil
        )
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(handleWillEnterForeground),
            name: UIApplication.willEnterForegroundNotification,
            object: nil
        )
    }

    deinit {
        NotificationCenter.default.removeObserver(self)
    }

    @objc private func handleDidEnterBackground() {
        if isListening || !pendingCalls.isEmpty {
            wasListeningBeforeBackground = true
            impl.stopUpdates()
        } else {
            wasListeningBeforeBackground = false
        }
    }

    @objc private func handleWillEnterForeground() {
        if wasListeningBeforeBackground && impl.isAvailable() {
            wasListeningBeforeBackground = false
            impl.startUpdates(samplingRate: nil) { [weak self] pressure, altitude, timestampMs, error in
                self?.handleReading(pressure: pressure, altitude: altitude, timestampMs: timestampMs, error: error)
            }
        }
    }

    // MARK: - Plugin Methods

    @objc func isAvailable(_ call: CAPPluginCall) {
        call.resolve(["available": impl.isAvailable()])
    }

    @objc func getPressure(_ call: CAPPluginCall) {
        guard impl.isAvailable() else {
            call.unavailable("No barometer sensor found on this device")
            return
        }

        let seaLevelPressure = call.getDouble("seaLevelPressure") ?? 1013.25

        // Return immediately if we already have a cached reading
        if let pressure = lastPressure, let timestampMs = lastTimestampMs {
            let altitude = impl.calculateAltitude(pressureHPa: pressure, seaLevelPressureHPa: seaLevelPressure)
            call.resolve([
                "pressure": pressure,
                "altitude": altitude,
                "timestamp": timestampMs,
                "seaLevelPressure": seaLevelPressure
            ])
            return
        }

        queuePendingCall(call)
    }

    @objc func getAltitude(_ call: CAPPluginCall) {
        let seaLevelPressure = call.getDouble("seaLevelPressure") ?? 1013.25

        // If a specific pressure was provided in options, calculate directly:
        if let customPressure = call.getDouble("pressure") {
            let altitude = impl.calculateAltitude(pressureHPa: customPressure, seaLevelPressureHPa: seaLevelPressure)
            call.resolve([
                "altitude": altitude,
                "pressure": customPressure,
                "seaLevelPressure": seaLevelPressure
            ])
            return
        }

        guard impl.isAvailable() else {
            call.unavailable("No barometer sensor found on this device")
            return
        }

        // Return immediately if cached pressure exists:
        if let pressure = lastPressure {
            let altitude = impl.calculateAltitude(pressureHPa: pressure, seaLevelPressureHPa: seaLevelPressure)
            call.resolve([
                "altitude": altitude,
                "pressure": pressure,
                "seaLevelPressure": seaLevelPressure
            ])
            return
        }

        queuePendingCall(call)
    }

    @objc func startListening(_ call: CAPPluginCall) {
        guard impl.isAvailable() else {
            call.unavailable("No barometer sensor found on this device")
            return
        }

        guard !isListening else {
            call.resolve()
            return
        }

        isListening = true
        let samplingRate = call.getString("samplingRate")

        // If updates were not already started by pendingCalls, start them
        if pendingCalls.isEmpty {
            impl.startUpdates(samplingRate: samplingRate) { [weak self] pressure, altitude, timestampMs, error in
                self?.handleReading(pressure: pressure, altitude: altitude, timestampMs: timestampMs, error: error)
            }
        }

        call.resolve()
    }

    @objc func stopListening(_ call: CAPPluginCall) {
        isListening = false
        // Only stop hardware updates if there are no pending one-shot calls waiting
        if pendingCalls.isEmpty {
            impl.stopUpdates()
        }
        call.resolve()
    }

    // MARK: - Private Helpers

    private func queuePendingCall(_ call: CAPPluginCall) {
        let needsStart = !isListening && pendingCalls.isEmpty
        pendingCalls.append(call)

        if needsStart {
            impl.startUpdates(samplingRate: nil) { [weak self] pressure, altitude, timestampMs, error in
                self?.handleReading(pressure: pressure, altitude: altitude, timestampMs: timestampMs, error: error)
            }
        }

        if pendingTimeoutItem == nil {
            let timeoutItem = DispatchWorkItem { [weak self] in
                guard let self = self else { return }
                self.pendingTimeoutItem = nil
                let timedOut = self.pendingCalls
                self.pendingCalls.removeAll()

                if !self.isListening {
                    self.impl.stopUpdates()
                }

                for c in timedOut {
                    c.reject("Timeout: no pressure reading received within 5 seconds")
                }
            }
            pendingTimeoutItem = timeoutItem
            DispatchQueue.main.asyncAfter(deadline: .now() + 5.0, execute: timeoutItem)
        }
    }

    private func handleReading(pressure: Double, altitude: Double, timestampMs: Double, error: Error?) {
        if let error = error {
            if isListening {
                let errObj = ["error": error.localizedDescription]
                notifyListeners("pressureChange", data: errObj)
                notifyListeners("onPressureChange", data: errObj)
            }
            if !pendingCalls.isEmpty {
                cancelPendingTimeout()
                let calls = pendingCalls
                pendingCalls.removeAll()
                if !isListening {
                    impl.stopUpdates()
                }
                for call in calls {
                    call.reject(error.localizedDescription)
                }
            }
            return
        }

        lastPressure = pressure
        lastAltitude = altitude
        lastTimestampMs = timestampMs

        let data: [String: Any] = [
            "pressure": pressure,
            "altitude": altitude,
            "timestamp": timestampMs
        ]

        if isListening {
            notifyListeners("pressureChange", data: data)
            notifyListeners("onPressureChange", data: data)
        }

        if !pendingCalls.isEmpty {
            cancelPendingTimeout()
            let calls = pendingCalls
            pendingCalls.removeAll()
            if !isListening {
                impl.stopUpdates()
            }
            for call in calls {
                let callSeaLevel = call.getDouble("seaLevelPressure") ?? 1013.25
                let calibratedAltitude = impl.calculateAltitude(pressureHPa: pressure, seaLevelPressureHPa: callSeaLevel)
                call.resolve([
                    "pressure": pressure,
                    "altitude": calibratedAltitude,
                    "timestamp": timestampMs,
                    "seaLevelPressure": callSeaLevel
                ])
            }
        }
    }

    private func cancelPendingTimeout() {
        pendingTimeoutItem?.cancel()
        pendingTimeoutItem = nil
    }
}
