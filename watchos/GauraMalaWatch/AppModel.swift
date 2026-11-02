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
    @Published private(set) var autoCountActive = false
    @Published private(set) var autoCountRunning = false
    @Published private(set) var autoCountUsesVoiceGuide = false
    @Published private(set) var hasVoiceChantRecording = false
    @Published private(set) var isVoiceChantRecording = false
    @Published private(set) var isVoiceChantPreviewing = false
    @Published private(set) var voiceRecordingFinished: Bool?
    @Published private(set) var voiceChantMessage: String?
    @Published var notificationPermissionDeclined = false

    private let defaults: UserDefaults
    static let storageKey = "gaura-mala.watch.counter-state.v1"
    static let appGroup = "group.com.gauramala.watchos"
    static let reminderCategoryIdentifier = "CHANT_REMINDER"
    static let reminderRequestIdentifier = "gaura-mala.chant-reminder"
    static let actionMarkGoalDone = "ACTION_MARK_GOAL_DONE"
    static let actionOpenRoundPicker = "ACTION_OPEN_ROUND_PICKER"
    private static let autoCountNotificationId = "gaura-mala.auto-count.session"

    private var engine: CounterEngine
    private var autoChantPacer = AutoChantPacer()
    private var autoCountTask: Task<Void, Never>?
    private let voiceChantAudio = VoiceChantAudio()
    private var lastIncrementMillis: Int64 = 0
    private let debounceWindowMillis: Int64 = 280
    private var lastNotificationSessionState: (Bool, Bool)?

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
        hasVoiceChantRecording = voiceChantAudio.hasRecording()
        super.init()
        configureNotificationCategories()
        persist()
    }

    func countBead() {
        incrementBead(applyDebounce: true, source: .tap)
    }

    func countBeadFromGesture() {
        incrementBead(applyDebounce: true, source: .gesture)
    }

    func countBeadFromCrown() {
        incrementBead(applyDebounce: false, source: .rotary)
    }

    private func incrementBead(applyDebounce: Bool, source: BeadInputSource) {
        guard !autoCountRunning else { return }
        let nowMillis = Self.uptimeMillis()
        if applyDebounce {
            if nowMillis - lastIncrementMillis < debounceWindowMillis {
                return
            }
            lastIncrementMillis = nowMillis
        }

        refreshForCurrentDay()
        let transitionCue = engine.countBead()
        state = engine.state
        persist()
        if let cue = selectCounterCue(for: transitionCue, state: state, source: source) {
            play(cue)
        }
        if state.showGoalReached { isShowingGoalAlert = true }
    }

    func undo() {
        guard !autoCountRunning else { return }
        refreshForCurrentDay()
        guard engine.undo() else { return }
        state = engine.state
        if !state.showGoalReached { isShowingGoalAlert = false }
        persist()
        if state.isHapticsEnabled { WKInterfaceDevice.current().play(.click) }
    }

    func setCompletedRounds(_ rounds: Int) {
        stopAutoCount()
        refreshForCurrentDay()
        guard engine.setCompletedRounds(rounds) else { return }
        state = engine.state
        isShowingGoalAlert = false
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
        isScreenTapEnabled: Bool? = nil,
        isHapticsEnabled: Bool? = nil,
        areMilestonesEnabled: Bool? = nil,
        areRemindersEnabled: Bool? = nil
    ) {
        refreshForCurrentDay()
        engine.updateSettings(
            isDoubleTapEnabled: isDoubleTapEnabled,
            isScreenTapEnabled: isScreenTapEnabled,
            isHapticsEnabled: isHapticsEnabled,
            areMilestonesEnabled: areMilestonesEnabled,
            areRemindersEnabled: areRemindersEnabled
        )
        state = engine.state
        persist()
        if areRemindersEnabled == true {
            Task {
                let center = UNUserNotificationCenter.current()
                _ = try? await center.requestAuthorization(options: [.alert, .sound])
                self.syncReminderNotification()
            }
        }
    }

    func markDailyGoalCompleted() {
        refreshForCurrentDay()
        engine.markDailyGoalCompleted()
        state = engine.state
        persist()
    }

    func resetCurrentRound() {
        pauseAutoCount()
        refreshForCurrentDay()
        engine.resetCurrentRound()
        state = engine.state
        isShowingGoalAlert = false
        persist()
        if state.isHapticsEnabled { WKInterfaceDevice.current().play(.click) }
    }

    func resetToday() {
        stopAutoCount()
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
        if engine.rollOverIfNeeded(to: Self.localDateString()) {
            state = engine.state
            isShowingGoalAlert = false
            persist()
        } else {
            syncReminderNotification()
        }
    }

    func roundAdjustmentSelectionTick() {
        guard state.isHapticsEnabled else { return }
        WKInterfaceDevice.current().play(.click)
    }

    func autoChantCalibrationCue() {
        guard state.isHapticsEnabled else { return }
        WKInterfaceDevice.current().play(.start)
    }

    func saveAutoChantPace(_ paceMillis: Int) {
        guard autoChantPacer.configure(paceMillis: paceMillis),
              engine.setAutoChantPaceMillis(paceMillis) else { return }
        stopAutoCount()
        state = engine.state
        persist()
    }

    func prepareVoiceChantSetup() {
        voiceRecordingFinished = nil
        voiceChantMessage = nil
    }

    func startAutoCount(useVoiceGuide: Bool = false) {
        if useVoiceGuide {
            guard hasVoiceChantRecording, voiceChantAudio.recordingDurationMillis() != nil else { return }
        } else {
            guard let paceMillis = state.autoChantPaceMillis,
                  autoChantPacer.configure(paceMillis: paceMillis) else { return }
        }

        autoCountTask?.cancel()
        autoCountTask = nil
        let nowMillis = Self.uptimeMillis()
        if !useVoiceGuide && !autoChantPacer.start(nowMillis: nowMillis) {
            return
        }

        autoCountActive = true
        autoCountRunning = true
        autoCountUsesVoiceGuide = useVoiceGuide
        isVoiceChantPreviewing = false
        updateAutoCountNotification()
        if state.isHapticsEnabled { WKInterfaceDevice.current().play(.start) }

        if useVoiceGuide {
            let started = voiceChantAudio.playLoop { [weak self] in
                guard let self else { return false }
                return MainActor.assumeIsolated {
                    self.onVoiceChantCompleted()
                }
            }
            if !started {
                stopAutoCount()
                voiceChantMessage = "Couldn't play the recording. Record it again."
            }
            return
        }

        autoCountTask = Task { [weak self] in
            while !Task.isCancelled {
                guard let self, self.autoCountRunning else { break }
                let now = Self.uptimeMillis()
                guard let waitMillis = self.autoChantPacer.timeUntilNextCount(nowMillis: now) else { break }
                let nanoseconds = UInt64(max(waitMillis, 1)) * 1_000_000
                try? await Task.sleep(nanoseconds: nanoseconds)
                guard !Task.isCancelled, self.autoCountRunning else { break }
                if self.autoChantPacer.consumeDueCount(nowMillis: Self.uptimeMillis()),
                   !self.incrementBeadFromAutoCount() {
                    self.stopAutoCount()
                    break
                }
            }
        }
    }

    func pauseAutoCount() {
        guard autoCountActive, autoCountRunning else { return }
        autoCountTask?.cancel()
        autoCountTask = nil
        autoChantPacer.pause()
        voiceChantAudio.stopPlayback()
        autoCountRunning = false
        updateAutoCountNotification()
    }

    func resumeAutoCount() {
        guard autoCountActive, !autoCountRunning else { return }
        startAutoCount(useVoiceGuide: autoCountUsesVoiceGuide)
    }

    func stopAutoCount() {
        autoCountTask?.cancel()
        autoCountTask = nil
        autoChantPacer.pause()
        voiceChantAudio.stopPlayback()
        autoCountActive = false
        autoCountRunning = false
        autoCountUsesVoiceGuide = false
        updateAutoCountNotification()
    }

    private func incrementBeadFromAutoCount() -> Bool {
        refreshForCurrentDay()
        let transitionCue = engine.countBead()
        state = engine.state
        persist()
        if let cue = selectCounterCue(for: transitionCue, state: state, source: .auto) {
            play(cue)
        }
        if state.showGoalReached {
            isShowingGoalAlert = true
            return false
        }
        return true
    }

    private func onVoiceChantCompleted() -> Bool {
        let shouldContinue = incrementBeadFromAutoCount() && autoCountRunning
        if !shouldContinue {
            autoCountRunning = false
            autoCountActive = false
            updateAutoCountNotification()
        }
        return shouldContinue
    }

    func requestMicrophonePermission(_ completion: @escaping (Bool) -> Void) {
        voiceChantAudio.requestMicrophonePermission { granted in
            Task { @MainActor in
                completion(granted)
            }
        }
    }

    func startVoiceRecording() -> Bool {
        guard !isVoiceChantRecording else { return false }
        voiceChantMessage = nil
        voiceRecordingFinished = nil
        isVoiceChantPreviewing = false
        let started = voiceChantAudio.startRecording { [weak self] in
            Task { @MainActor in
                self?.finishVoiceRecording()
            }
        }
        isVoiceChantRecording = started
        if !started {
            voiceChantMessage = "Recording couldn't start. Check microphone access and try again."
        }
        return started
    }

    func finishVoiceRecording() {
        let stopped = voiceChantAudio.finishRecording()
        let duration = stopped ? voiceChantAudio.temporaryRecordingDurationMillis() : nil
        let validDuration = duration.flatMap { AutoChantTiming.isValidPace($0) ? $0 : nil }
        let saved = validDuration != nil && voiceChantAudio.publishTemporaryRecording()
        if !saved {
            voiceChantAudio.discardTemporaryRecording()
        }
        if let validDuration, saved {
            saveAutoChantPace(validDuration)
        }
        isVoiceChantRecording = false
        voiceRecordingFinished = saved
        hasVoiceChantRecording = hasVoiceChantRecording || saved
        if saved {
            voiceChantMessage = "Chant saved on this watch."
        } else if duration != nil {
            voiceChantMessage = "Try a chant between 1 and 15 seconds."
        } else {
            voiceChantMessage = "Couldn't save that chant. Please try again."
        }
    }

    func cancelVoiceChantSetup() {
        cancelVoiceRecording()
        voiceRecordingFinished = nil
        voiceChantMessage = nil
    }

    func cancelVoiceRecording() {
        voiceChantAudio.cancelRecording()
        if isVoiceChantPreviewing {
            voiceChantAudio.stopPlayback()
        }
        isVoiceChantRecording = false
        isVoiceChantPreviewing = false
    }

    func previewVoiceChant() {
        guard hasVoiceChantRecording else { return }
        voiceChantMessage = nil
        isVoiceChantPreviewing = true
        let started = voiceChantAudio.preview { [weak self] in
            Task { @MainActor in
                self?.isVoiceChantPreviewing = false
            }
        }
        if !started {
            isVoiceChantPreviewing = false
            voiceChantMessage = "Couldn't play the recording. Record it again."
        }
    }

    func stopVoiceChantPreview() {
        guard isVoiceChantPreviewing else { return }
        voiceChantAudio.stopPlayback()
        isVoiceChantPreviewing = false
    }

    func deleteVoiceChantRecording() {
        if autoCountUsesVoiceGuide {
            stopAutoCount()
        }
        voiceChantAudio.deleteRecording()
        hasVoiceChantRecording = false
        isVoiceChantRecording = false
        isVoiceChantPreviewing = false
        voiceRecordingFinished = nil
        voiceChantMessage = "Recording deleted from this watch."
    }

    private func updateAutoCountNotification() {
        syncReminderNotification()
        let current = (autoCountActive, autoCountRunning)
        if let last = lastNotificationSessionState, last.0 == current.0 && last.1 == current.1 {
            return
        }
        lastNotificationSessionState = current

        let center = UNUserNotificationCenter.current()
        if !autoCountActive {
            center.removePendingNotificationRequests(withIdentifiers: [Self.autoCountNotificationId])
            center.removeDeliveredNotifications(withIdentifiers: [Self.autoCountNotificationId])
            return
        }

        center.getNotificationSettings { settings in
            guard settings.authorizationStatus == .authorized || settings.authorizationStatus == .provisional else {
                center.removePendingNotificationRequests(withIdentifiers: [Self.autoCountNotificationId])
                center.removeDeliveredNotifications(withIdentifiers: [Self.autoCountNotificationId])
                return
            }
            let content = UNMutableNotificationContent()
            content.title = "Auto Count"
            content.body = current.1 ? "Counting · tap to return" : "Paused · tap to return"
            content.sound = nil
            let request = UNNotificationRequest(
                identifier: Self.autoCountNotificationId,
                content: content,
                trigger: nil
            )
            center.add(request)
        }
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
        syncReminderNotification()
        guard let data = try? JSONEncoder().encode(engine.state) else { return }
        state = engine.state
        defaults.set(data, forKey: Self.storageKey)
        WidgetCenter.shared.reloadAllTimelines()
    }

    func syncReminderNotification() {
        let center = UNUserNotificationCenter.current()
        let reminderIds = [Self.reminderRequestIdentifier] + (0..<ChantReminderPolicy.templateCount).map {
            "\(Self.reminderRequestIdentifier).\($0)"
        }
        center.removePendingNotificationRequests(withIdentifiers: reminderIds)
        center.removeDeliveredNotifications(withIdentifiers: reminderIds)

        let currentState = engine.state
        guard ChantReminderPolicy.isEligibleForReminder(
            remindersEnabled: currentState.areRemindersEnabled,
            completedRounds: currentState.completedRounds,
            beadCount: currentState.beadCount,
            dailyGoalRounds: currentState.dailyGoalRounds,
            isAutoChanting: autoCountActive
        ),
        let targetDate = ChantReminderPolicy.nextReminderDate(from: .now) else {
            return
        }

        let contentSpec = ChantReminderPolicy.buildReminderContent(
            completedRounds: currentState.completedRounds,
            dailyGoalRounds: currentState.dailyGoalRounds,
            lastTemplateIndex: currentState.lastReminderTemplateIndex
        )
        engine.updateSettings(lastReminderTemplateIndex: contentSpec.templateIndex)
        state = engine.state
        if let data = try? JSONEncoder().encode(engine.state) {
            defaults.set(data, forKey: Self.storageKey)
        }

        let delay = max(targetDate.timeIntervalSinceNow, 60)
        let requestId = "\(Self.reminderRequestIdentifier).\(contentSpec.templateIndex)"
        center.getNotificationSettings { settings in
            guard settings.authorizationStatus == .authorized || settings.authorizationStatus == .provisional else {
                center.removePendingNotificationRequests(withIdentifiers: reminderIds)
                center.removeDeliveredNotifications(withIdentifiers: reminderIds)
                return
            }
            let content = UNMutableNotificationContent()
            content.title = contentSpec.title
            content.body = contentSpec.body
            content.sound = .default
            content.categoryIdentifier = Self.reminderCategoryIdentifier

            let trigger = UNTimeIntervalNotificationTrigger(timeInterval: delay, repeats: false)
            let request = UNNotificationRequest(
                identifier: requestId,
                content: content,
                trigger: trigger
            )
            center.add(request)
        }
    }

    private func configureNotificationCategories() {
        let center = UNUserNotificationCenter.current()
        center.delegate = self
        let goalDoneAction = UNNotificationAction(
            identifier: Self.actionMarkGoalDone,
            title: "All Rounds Complete",
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

    private static func uptimeMillis() -> Int64 {
        Int64(ProcessInfo.processInfo.systemUptime * 1_000.0)
    }

    private static func localDateString(_ date: Date = .now) -> String {
        let components = Calendar.current.dateComponents([.year, .month, .day], from: date)
        return String(format: "%04d-%02d-%02d", components.year ?? 0, components.month ?? 0, components.day ?? 0)
    }
}
