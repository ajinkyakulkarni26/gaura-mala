// swift-tools-version: 6.0
import PackageDescription

let package = Package(
    name: "GauraMalaCore",
    platforms: [.macOS(.v13), .watchOS(.v10)],
    products: [
        .library(name: "GauraMalaCore", targets: ["GauraMalaCore"])
    ],
    targets: [
        .target(name: "GauraMalaCore"),
        .testTarget(name: "GauraMalaCoreTests", dependencies: ["GauraMalaCore"])
    ]
)
