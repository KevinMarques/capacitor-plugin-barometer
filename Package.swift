// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "CapacitorBarometer",
    platforms: [.iOS(.v15)],
    products: [
        .library(
            name: "CapacitorBarometer",
            targets: ["BarometerPlugin"])
    ],
    dependencies: [
        .package(url: "https://github.com/ionic-team/capacitor-swift-pm.git", from: "8.0.0")
    ],
    targets: [
        .target(
            name: "BarometerPlugin",
            dependencies: [
                .product(name: "Capacitor", package: "capacitor-swift-pm")
            ],
            path: "ios/Sources/BarometerPluginPlugin"),
        .testTarget(
            name: "BarometerPluginTests",
            dependencies: ["BarometerPlugin"],
            path: "ios/Tests/BarometerPluginPluginTests")
    ]
)
