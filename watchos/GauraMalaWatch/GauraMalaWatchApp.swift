import SwiftUI

@main
struct GauraMalaWatchApp: App {
    @StateObject private var model = AppModel()

    var body: some Scene {
        WindowGroup {
            CounterView()
                .environmentObject(model)
        }
    }
}
