// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "CapacitorBarometer",
    platforms: [.iOS(.v13)],
    products: [
        .library(
            name: "CapacitorBarometer",
            targets: ["BarometerPluginPlugin"])
    ],
    dependencies: [
        .package(url: "https://github.com/ionic-team/capacitor-swift-pm.git", branch: "main")
    ],
    targets: [
        .target(
            name: "BarometerPluginPlugin",
            dependencies: [
                .product(name: "Capacitor", package: "capacitor-swift-pm"),
                .product(name: "Cordova", package: "capacitor-swift-pm")
            ],
            path: "ios/Sources/BarometerPluginPlugin"),
        .testTarget(
            name: "BarometerPluginPluginTests",
            dependencies: ["BarometerPluginPlugin"],
            path: "ios/Tests/BarometerPluginPluginTests")
    ]
)