// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "CapacitorBarometer",
    platforms: [.iOS(.v13)],
    products: [
        .library(
            name: "CapacitorBarometer",
            targets: ["BarometerPlugin"])
    ],
    dependencies: [
        .package(url: "https://github.com/ionic-team/capacitor-swift-pm.git", branch: "main")
    ],
    targets: [
        .target(
            name: "BarometerPlugin",
            dependencies: [
                .product(name: "Capacitor", package: "capacitor-swift-pm"),
                .product(name: "Cordova", package: "capacitor-swift-pm")
            ],
            path: "ios/Sources/BarometerPlugin"),
        .testTarget(
            name: "BarometerPluginTests",
            dependencies: ["BarometerPlugin"],
            path: "ios/Tests/BarometerPluginTests")
    ]
)
