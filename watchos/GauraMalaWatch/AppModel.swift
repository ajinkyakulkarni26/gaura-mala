import Combine
import Foundation
import WatchKit
import WidgetKit

@MainActor
final class AppModel: ObservableObject {
    @Published private(set) var state: CounterState
    @Published var isShowingGoalAlert = false

    private let defaults: UserDefaults
    static let storageKey = "gaura-mala.watch.counter-state.v1"
    static let appGroup = "group.com.gauramala.watchos"
    private var engine: CounterEngine

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults === UserDefaults.standard
            ? (UserDefaults(suiteName: Self.appGroup) ?? .standard)
            : defaults
        var restored = self.defaults.data(forKey: Self.storageKey)
            .flatMap { try? JSONDecoder().decode(CounterState.self, from: $0) } ?? CounterState()
        // Undo history belongs to the active process, and a dismissed goal alert should not reappear on relaunch.
        restored.canUndo = false
        restored.showGoalReached = false
        engine = CounterEngine(state: restored)
        _ = engine.rollOverIfNeeded(to: Self.localDateString())
        state = engine.state
        persist()
    }

    func countBead() {
        refreshForCurrentDay()
        let cue = engine.countBead()
        state = engine.state
        persist()
        play(cue)
        if state.showGoalReached { isShowingGoalAlert = true }
    }

    func undo() {
        refreshForCurrentDay()
        guard engine.undo() else { return }
        state = engine.state
        persist()
        if state.isHapticsEnabled { WKInterfaceDevice.current().play(.click) }
    }

    func setCompletedRounds(_ rounds: Int) {
        refreshForCurrentDay()
        guard engine.setCompletedRounds(rounds) else { return }
        state = engine.state
        persist()
    }

    func setDailyGoal(_ rounds: Int) {
        refreshForCurrentDay()
        guard engine.setDailyGoal(rounds) else { return }
        state = engine.state
        persist()
    }

    func updateSettings(
        isDoubleTapEnabled: Bool? = nil,
        isHapticsEnabled: Bool? = nil,
        areMilestonesEnabled: Bool? = nil
    ) {
        refreshForCurrentDay()
        engine.updateSettings(
            isDoubleTapEnabled: isDoubleTapEnabled,
            isHapticsEnabled: isHapticsEnabled,
            areMilestonesEnabled: areMilestonesEnabled
        )
        state = engine.state
        persist()
    }

    func resetCurrentRound() {
        refreshForCurrentDay()
        engine.resetCurrentRound()
        state = engine.state
        persist()
    }

    func resetToday() {
        engine.resetToday(date: Self.localDateString())
        state = engine.state
        isShowingGoalAlert = false
        persist()
    }

    func dismissGoalAlert() {
        refreshForCurrentDay()
        engine.dismissGoalReached()
        state = engine.state
        isShowingGoalAlert = false
        persist()
    }

    func refreshForCurrentDay() {
        guard engine.rollOverIfNeeded(to: Self.localDateString()) else { return }
        state = engine.state
        isShowingGoalAlert = false
        persist()
    }

    private func play(_ cue: CounterCue) {
        guard state.isHapticsEnabled else { return }
        switch cue {
        case .bead:
            WKInterfaceDevice.current().play(.click)
        case .milestone:
            WKInterfaceDevice.current().play(.directionUp)
        case .roundCompleted:
            WKInterfaceDevice.current().play(.success)
        case .dailyGoalAchieved:
            WKInterfaceDevice.current().play(.notification)
        }
    }

    private func persist() {
        guard let data = try? JSONEncoder().encode(engine.state) else { return }
        defaults.set(data, forKey: Self.storageKey)
        WidgetCenter.shared.reloadAllTimelines()
    }

    private static func localDateString(_ date: Date = .now) -> String {
        let components = Calendar.current.dateComponents([.year, .month, .day], from: date)
        return String(format: "%04d-%02d-%02d", components.year ?? 0, components.month ?? 0, components.day ?? 0)
    }
}
