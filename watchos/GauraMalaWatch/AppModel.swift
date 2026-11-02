import Combine
import Foundation
import UserNotifications
import WatchKit
import WidgetKit

@MainActor
final class AppModel: NSObject, ObservableObject, UNUserNotificationCenterDelegate {
    @Published private(set) var state: CounterState
    @Published var isShowingGoalAlert = false
    @Published var isShowingRoundPicker = false

    private let defaults: UserDefaults
    static let storageKey = "gaura-mala.watch.counter-state.v1"
    static let appGroup = "group.com.gauramala.watchos"
    static let reminderCategoryIdentifier = "CHANT_REMINDER"
    static let reminderRequestIdentifier = "gaura-mala.chant-reminder"
    static let actionMarkGoalDone = "ACTION_MARK_GOAL_DONE"
    static let actionOpenRoundPicker = "ACTION_OPEN_ROUND_PICKER"
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
        super.init()
        configureNotificationCategories()
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
        areMilestonesEnabled: Bool? = nil,
        areRemindersEnabled: Bool? = nil
    ) {
        refreshForCurrentDay()
        if areRemindersEnabled == true {
            Task {
                let center = UNUserNotificationCenter.current()
                let granted = (try? await center.requestAuthorization(options: [.alert, .sound])) ?? false
                engine.updateSettings(
                    isDoubleTapEnabled: isDoubleTapEnabled,
                    isHapticsEnabled: isHapticsEnabled,
                    areMilestonesEnabled: areMilestonesEnabled,
                    areRemindersEnabled: granted
                )
                state = engine.state
                persist()
            }
            return
        }
        engine.updateSettings(
            isDoubleTapEnabled: isDoubleTapEnabled,
            isHapticsEnabled: isHapticsEnabled,
            areMilestonesEnabled: areMilestonesEnabled,
            areRemindersEnabled: areRemindersEnabled
        )
        state = engine.state
        persist()
    }

    func markDailyGoalCompleted() {
        refreshForCurrentDay()
        engine.markDailyGoalCompleted()
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
        guard let data = try? JSONEncoder().decodeStateAndReencode(&engine, syncReminder: true) else { return }
        state = engine.state
        defaults.set(data, forKey: Self.storageKey)
        WidgetCenter.shared.reloadAllTimelines()
    }

    private func configureNotificationCategories() {
        let center = UNUserNotificationCenter.current()
        center.delegate = self
        let goalDoneAction = UNNotificationAction(
            identifier: Self.actionMarkGoalDone,
            title: "Goal done",
            options: []
        )
        let logRoundsAction = UNNotificationAction(
            identifier: Self.actionOpenRoundPicker,
            title: "Log rounds",
            options: [.foreground]
        )
        let category = UNNotificationCategory(
            identifier: Self.reminderCategoryIdentifier,
            actions: [goalDoneAction, logRoundsAction],
            intentIdentifiers: [],
            options: []
        )
        center.setNotificationCategories([category])
    }

    nonisolated func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        let actionIdentifier = response.actionIdentifier
        Task { @MainActor in
            switch actionIdentifier {
            case Self.actionMarkGoalDone:
                self.markDailyGoalCompleted()
            case Self.actionOpenRoundPicker:
                self.isShowingRoundPicker = true
            default:
                break
            }
            completionHandler()
        }
    }

    private static func localDateString(_ date: Date = .now) -> String {
        let components = Calendar.current.dateComponents([.year, .month, .day], from: date)
        return String(format: "%04d-%02d-%02d", components.year ?? 0, components.month ?? 0, components.day ?? 0)
    }
}

private extension JSONEncoder {
    func decodeStateAndReencode(_ engine: inout CounterEngine, syncReminder: Bool) throws -> Data {
        let center = UNUserNotificationCenter.current()
        center.removePendingNotificationRequests(withIdentifiers: [AppModel.reminderRequestIdentifier])
        center.removeDeliveredNotifications(withIdentifiers: [AppModel.reminderRequestIdentifier])

        let currentState = engine.state
        if syncReminder,
           ChantReminderPolicy.isEligibleForReminder(
               remindersEnabled: currentState.areRemindersEnabled,
               completedRounds: currentState.completedRounds,
               beadCount: currentState.beadCount,
               dailyGoalRounds: currentState.dailyGoalRounds
           ),
           let targetDate = ChantReminderPolicy.nextReminderDate(from: .now) {
            let contentSpec = ChantReminderPolicy.buildReminderContent(
                completedRounds: currentState.completedRounds,
                dailyGoalRounds: currentState.dailyGoalRounds,
                lastTemplateIndex: currentState.lastReminderTemplateIndex
            )
            engine.updateSettings(lastReminderTemplateIndex: contentSpec.templateIndex)
            let content = UNMutableNotificationContent()
            content.title = contentSpec.title
            content.body = contentSpec.body
            content.sound = .default
            content.categoryIdentifier = AppModel.reminderCategoryIdentifier

            let delay = max(targetDate.timeIntervalSinceNow, 60)
            let trigger = UNTimeIntervalNotificationTrigger(timeInterval: delay, repeats: false)
            let request = UNNotificationRequest(
                identifier: AppModel.reminderRequestIdentifier,
                content: content,
                trigger: trigger
            )
            center.add(request)
        }
        return try encode(engine.state)
    }
}
