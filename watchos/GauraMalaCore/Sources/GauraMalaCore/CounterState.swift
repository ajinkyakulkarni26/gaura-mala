import Foundation

public struct CounterState: Codable, Equatable, Sendable {
    public var beadCount: Int
    public var completedRounds: Int
    public var dailyGoalRounds: Int
    public var isDoubleTapEnabled: Bool
    public var isScreenTapEnabled: Bool
    public var isHapticsEnabled: Bool
    public var areMilestonesEnabled: Bool
    public var areRemindersEnabled: Bool
    public var lastReminderTemplateIndex: Int
    public var autoChantPaceMillis: Int?
    public var lastRecordedDate: String
    public var canUndo: Bool
    public var showGoalReached: Bool

    public init(
        beadCount: Int = 0,
        completedRounds: Int = 0,
        dailyGoalRounds: Int = 16,
        isDoubleTapEnabled: Bool = true,
        isScreenTapEnabled: Bool = true,
        isHapticsEnabled: Bool = true,
        areMilestonesEnabled: Bool = true,
        areRemindersEnabled: Bool = false,
        lastReminderTemplateIndex: Int = -1,
        autoChantPaceMillis: Int? = nil,
        lastRecordedDate: String = "",
        canUndo: Bool = false,
        showGoalReached: Bool = false
    ) {
        self.beadCount = min(max(beadCount, 0), 107)
        self.completedRounds = min(max(completedRounds, 0), 999)
        self.dailyGoalRounds = min(max(dailyGoalRounds, 1), 64)
        self.isDoubleTapEnabled = isDoubleTapEnabled
        self.isScreenTapEnabled = isScreenTapEnabled
        self.isHapticsEnabled = isHapticsEnabled
        self.areMilestonesEnabled = areMilestonesEnabled
        self.areRemindersEnabled = areRemindersEnabled
        self.lastReminderTemplateIndex = lastReminderTemplateIndex
        self.autoChantPaceMillis = autoChantPaceMillis.flatMap {
            AutoChantTiming.isValidPace($0) ? $0 : nil
        }
        self.lastRecordedDate = lastRecordedDate
        self.canUndo = canUndo
        self.showGoalReached = showGoalReached
    }

    private enum CodingKeys: String, CodingKey {
        case beadCount
        case completedRounds
        case dailyGoalRounds
        case isDoubleTapEnabled
        case isScreenTapEnabled
        case isHapticsEnabled
        case areMilestonesEnabled
        case areRemindersEnabled
        case lastReminderTemplateIndex
        case autoChantPaceMillis
        case lastRecordedDate
        case canUndo
        case showGoalReached
    }

    public init(from decoder: Decoder) throws {
        let values = try decoder.container(keyedBy: CodingKeys.self)
        self.init(
            beadCount: try values.decodeIfPresent(Int.self, forKey: .beadCount) ?? 0,
            completedRounds: try values.decodeIfPresent(Int.self, forKey: .completedRounds) ?? 0,
            dailyGoalRounds: try values.decodeIfPresent(Int.self, forKey: .dailyGoalRounds) ?? 16,
            isDoubleTapEnabled: try values.decodeIfPresent(Bool.self, forKey: .isDoubleTapEnabled) ?? true,
            isScreenTapEnabled: try values.decodeIfPresent(Bool.self, forKey: .isScreenTapEnabled) ?? true,
            isHapticsEnabled: try values.decodeIfPresent(Bool.self, forKey: .isHapticsEnabled) ?? true,
            areMilestonesEnabled: try values.decodeIfPresent(Bool.self, forKey: .areMilestonesEnabled) ?? true,
            areRemindersEnabled: try values.decodeIfPresent(Bool.self, forKey: .areRemindersEnabled) ?? false,
            lastReminderTemplateIndex: try values.decodeIfPresent(Int.self, forKey: .lastReminderTemplateIndex) ?? -1,
            autoChantPaceMillis: try values.decodeIfPresent(Int.self, forKey: .autoChantPaceMillis),
            lastRecordedDate: try values.decodeIfPresent(String.self, forKey: .lastRecordedDate) ?? "",
            canUndo: false,
            showGoalReached: false
        )
    }

    public var currentRound: Int { completedRounds + 1 }
    public var totalMantrasChanted: Int { (completedRounds * 108) + beadCount }
    public var isGoalAchieved: Bool { completedRounds >= dailyGoalRounds }
    public var roundProgress: Double { min(max(Double(beadCount) / 108.0, 0.0), 1.0) }
    public var dailyProgress: Double {
        guard dailyGoalRounds > 0 else { return 0.0 }
        return min(max(Double(completedRounds) / Double(dailyGoalRounds), 0.0), 1.0)
    }
}

public enum BeadInputSource: Equatable, Sendable {
    case tap
    case rotary
    case gesture
    case auto
}

public enum CounterCue: Equatable, Sendable {
    case bead
    case milestone
    case roundCompleted
    case dailyGoalAchieved
}

/// Selects the app haptic cue for an input source. Double Tap relies on system feedback for ordinary beads.
public func selectCounterCue(
    for transitionCue: CounterCue,
    state: CounterState,
    source: BeadInputSource
) -> CounterCue? {
    guard state.isHapticsEnabled else { return nil }
    switch transitionCue {
    case .dailyGoalAchieved, .roundCompleted, .milestone:
        return transitionCue
    case .bead:
        return source == .gesture ? nil : .bead
    }
}

/// Platform-independent counter transitions, shared by the watch UI and unit tests.
public struct CounterEngine: Sendable {
    public private(set) var state: CounterState
    private var previousState: CounterState?

    public init(state: CounterState = CounterState()) {
        self.state = state
        self.previousState = nil
    }

    @discardableResult
    public mutating func countBead() -> CounterCue {
        let hadReachedGoal = state.isGoalAchieved
        previousState = state
        state.canUndo = true

        guard state.beadCount == 107 else {
            state.beadCount += 1
            state.showGoalReached = false
            if state.areMilestonesEnabled && [27, 54, 81].contains(state.beadCount) {
                return .milestone
            }
            return .bead
        }

        state.beadCount = 0
        state.completedRounds += 1
        let reachedGoal = !hadReachedGoal && state.completedRounds >= state.dailyGoalRounds
        state.showGoalReached = reachedGoal
        return reachedGoal ? .dailyGoalAchieved : .roundCompleted
    }

    @discardableResult
    public mutating func undo() -> Bool {
        guard let previousState else { return false }
        state.beadCount = previousState.beadCount
        state.completedRounds = previousState.completedRounds
        state.showGoalReached = state.showGoalReached && previousState.completedRounds >= state.dailyGoalRounds
        state.canUndo = false
        self.previousState = nil
        return true
    }

    @discardableResult
    public mutating func setCompletedRounds(_ rounds: Int) -> Bool {
        guard (0...999).contains(rounds) else { return false }
        state.completedRounds = rounds
        state.beadCount = 0
        state.canUndo = false
        state.showGoalReached = false
        previousState = nil
        return true
    }

    @discardableResult
    public mutating func setDailyGoal(_ rounds: Int) -> Bool {
        guard (1...64).contains(rounds) else { return false }
        state.dailyGoalRounds = rounds
        return true
    }

    @discardableResult
    public mutating func setAutoChantPaceMillis(_ paceMillis: Int?) -> Bool {
        if let paceMillis {
            guard AutoChantTiming.isValidPace(paceMillis) else { return false }
            state.autoChantPaceMillis = paceMillis
            return true
        }
        state.autoChantPaceMillis = nil
        return true
    }

    public mutating func updateSettings(
        isDoubleTapEnabled: Bool? = nil,
        isScreenTapEnabled: Bool? = nil,
        isHapticsEnabled: Bool? = nil,
        areMilestonesEnabled: Bool? = nil,
        areRemindersEnabled: Bool? = nil,
        lastReminderTemplateIndex: Int? = nil
    ) {
        if let isDoubleTapEnabled { state.isDoubleTapEnabled = isDoubleTapEnabled }
        if let isScreenTapEnabled { state.isScreenTapEnabled = isScreenTapEnabled }
        if let isHapticsEnabled { state.isHapticsEnabled = isHapticsEnabled }
        if let areMilestonesEnabled { state.areMilestonesEnabled = areMilestonesEnabled }
        if let areRemindersEnabled { state.areRemindersEnabled = areRemindersEnabled }
        if let lastReminderTemplateIndex { state.lastReminderTemplateIndex = lastReminderTemplateIndex }
    }

    public mutating func markDailyGoalCompleted() {
        state.completedRounds = max(state.completedRounds, state.dailyGoalRounds)
        state.beadCount = 0
        state.canUndo = false
        state.showGoalReached = false
        previousState = nil
    }

    public mutating func resetCurrentRound() {
        state.beadCount = 0
        state.canUndo = false
        state.showGoalReached = false
        previousState = nil
    }

    public mutating func resetToday(date: String) {
        state.beadCount = 0
        state.completedRounds = 0
        state.lastRecordedDate = date
        state.canUndo = false
        state.showGoalReached = false
        previousState = nil
    }

    public mutating func rollOverIfNeeded(to date: String) -> Bool {
        guard state.lastRecordedDate != date else { return false }
        resetToday(date: date)
        return true
    }

    public mutating func dismissGoalReached() {
        state.showGoalReached = false
    }
}

public struct ChantReminderContent: Equatable, Sendable {
    public let templateIndex: Int
    public let title: String
    public let body: String

    public init(templateIndex: Int, title: String, body: String) {
        self.templateIndex = templateIndex
        self.title = title
        self.body = body
    }
}

public enum ChantReminderPolicy {
    public static let inactivityMinutes: Int = 150
    public static let quietHoursStartHour: Int = 21
    public static let quietHoursEndHour: Int = 6
    public static let templateCount: Int = 5

    public static func isEligibleForReminder(
        remindersEnabled: Bool,
        completedRounds: Int,
        beadCount: Int,
        dailyGoalRounds: Int,
        isAutoChanting: Bool = false
    ) -> Bool {
        if !remindersEnabled || isAutoChanting {
            return false
        }
        let hasStartedToday = completedRounds > 0 || beadCount > 0
        let hasRemainingGoal = completedRounds < dailyGoalRounds
        return hasStartedToday && hasRemainingGoal
    }

    public static func isWithinQuietHours(hour: Int) -> Bool {
        hour >= quietHoursStartHour || hour < quietHoursEndHour
    }

    public static func nextReminderDate(from now: Date, calendar: Calendar = .current) -> Date? {
        guard let candidate = calendar.date(byAdding: .minute, value: inactivityMinutes, to: now) else {
            return nil
        }
        let nowDay = calendar.startOfDay(for: now)
        let candidateDay = calendar.startOfDay(for: candidate)
        guard nowDay == candidateDay else {
            return nil
        }
        let candidateHour = calendar.component(.hour, from: candidate)
        if isWithinQuietHours(hour: candidateHour) {
            return nil
        }
        return candidate
    }

    public static func nextTemplateIndex(lastTemplateIndex: Int) -> Int {
        guard lastTemplateIndex >= 0 else { return 0 }
        return (lastTemplateIndex + 1) % templateCount
    }

    public static func buildReminderContent(
        completedRounds: Int,
        dailyGoalRounds: Int,
        lastTemplateIndex: Int
    ) -> ChantReminderContent {
        let index = nextTemplateIndex(lastTemplateIndex: lastTemplateIndex)
        let remaining = max(dailyGoalRounds - completedRounds, 1)
        let nextRound = completedRounds + 1
        let remainingLabel = remaining == 1 ? "1 round left" : "\(remaining) rounds left"

        switch index {
        case 0:
            return ChantReminderContent(
                templateIndex: 0,
                title: "Ready for Round \(nextRound)?",
                body: "You're at \(completedRounds) of \(dailyGoalRounds) today—let's chant a peaceful round."
            )
        case 1:
            return ChantReminderContent(
                templateIndex: 1,
                title: "Time for a quick round?",
                body: "\(remainingLabel) for your \(dailyGoalRounds)-round goal. Every mantra brings steady joy."
            )
        case 2:
            return ChantReminderContent(
                templateIndex: 2,
                title: "Keep your mala moving?",
                body: "Nice start with \(completedRounds) done! Tap to chant Round \(nextRound) or log bead rounds."
            )
        case 3:
            return ChantReminderContent(
                templateIndex: 3,
                title: "A gentle japa pause?",
                body: "Have a few quiet minutes for Round \(nextRound)? You're \(remainingLabel) from today's goal."
            )
        default:
            return ChantReminderContent(
                templateIndex: 4,
                title: "Chanted on your beads?",
                body: "Let's keep the enthusiasm flowing—chant Round \(nextRound) now or mark your goal done."
            )
        }
    }
}

/// Validates a measured chant pace and schedules one count per beat without catch-up bursts.
public enum AutoChantTiming {
    public static let minPaceMillis = 750
    public static let maxPaceMillis = 15_000
    public static let calibrationTrials = 3

    public static func isValidPace(_ paceMillis: Int) -> Bool {
        (minPaceMillis...maxPaceMillis).contains(paceMillis)
    }

    /// Uses the middle of three measured chants so one unusually fast or slow take has less effect.
    public static func medianPaceMillis(_ trials: [Int]) -> Int? {
        guard trials.count == calibrationTrials, trials.allSatisfy(isValidPace) else {
            return nil
        }
        return trials.sorted()[calibrationTrials / 2]
    }

    public static func averagePaceMillis(totalElapsedMillis: Int, chantCount: Int) -> Int? {
        guard totalElapsedMillis > 0, (1...10).contains(chantCount) else {
            return nil
        }
        let average = totalElapsedMillis / chantCount
        return min(max(average, minPaceMillis), maxPaceMillis)
    }
}

public struct AutoChantPacer: Sendable {
    private var paceMillis: Int64?
    private var nextCountAtMillis: Int64?

    public init() {}

    public var isRunning: Bool {
        nextCountAtMillis != nil
    }

    @discardableResult
    public mutating func configure(paceMillis: Int) -> Bool {
        guard AutoChantTiming.isValidPace(paceMillis) else {
            return false
        }
        self.paceMillis = Int64(paceMillis)
        self.nextCountAtMillis = nil
        return true
    }

    @discardableResult
    public mutating func start(nowMillis: Int64) -> Bool {
        guard let pace = paceMillis else { return false }
        nextCountAtMillis = nowMillis + pace
        return true
    }

    public mutating func pause() {
        nextCountAtMillis = nil
    }

    public func timeUntilNextCount(nowMillis: Int64) -> Int64? {
        guard let nextCountAt = nextCountAtMillis else { return nil }
        return max(nextCountAt - nowMillis, 0)
    }

    /// Counts at most once per call; late timer delivery never produces rapid catch-up counts.
    public mutating func consumeDueCount(nowMillis: Int64) -> Bool {
        guard let pace = paceMillis, let nextCountAt = nextCountAtMillis else {
            return false
        }
        guard nowMillis >= nextCountAt else {
            return false
        }
        nextCountAtMillis = nowMillis + pace
        return true
    }
}

/// App-private, non-backed-up files used only for the user's optional local chant guide.
public struct VoiceChantFiles: Sendable {
    public let directory: URL

    public init(directory: URL) {
        self.directory = directory
    }

    public var recordingURL: URL {
        directory.appendingPathComponent(Self.recordingName)
    }

    public func temporaryRecordingURL() -> URL {
        directory.appendingPathComponent(Self.tempRecordingName)
    }

    public func hasRecording() -> Bool {
        Self.isNonEmptyFile(recordingURL)
    }

    @discardableResult
    public func publishTemporaryRecording() -> Bool {
        let tempURL = temporaryRecordingURL()
        guard Self.isNonEmptyFile(tempURL) else { return false }
        let fileManager = FileManager.default
        do {
            try fileManager.createDirectory(at: directory, withIntermediateDirectories: true)
            if fileManager.fileExists(atPath: recordingURL.path) {
                _ = try fileManager.replaceItemAt(recordingURL, withItemAt: tempURL)
            } else {
                try fileManager.moveItem(at: tempURL, to: recordingURL)
            }
            excludeFromBackup(recordingURL)
            excludeFromBackup(directory)
            return true
        } catch {
            return false
        }
    }

    public func discardTemporaryRecording() {
        try? FileManager.default.removeItem(at: temporaryRecordingURL())
    }

    public func clearPendingTakes() {
        discardTemporaryRecording()
        try? FileManager.default.removeItem(at: directory.appendingPathComponent(Self.selectingName))
        for index in 1...Self.legacyTrialCount {
            try? FileManager.default.removeItem(at: directory.appendingPathComponent("chant-guide-trial-\(index).m4a"))
        }
    }

    public func deleteAll() {
        clearPendingTakes()
        try? FileManager.default.removeItem(at: recordingURL)
    }

    private func excludeFromBackup(_ url: URL) {
        var mutableURL = url
        var resourceValues = URLResourceValues()
        resourceValues.isExcludedFromBackup = true
        try? mutableURL.setResourceValues(resourceValues)
    }

    private static func isNonEmptyFile(_ url: URL) -> Bool {
        var isDirectory: ObjCBool = false
        guard FileManager.default.fileExists(atPath: url.path, isDirectory: &isDirectory),
              !isDirectory.boolValue,
              let attributes = try? FileManager.default.attributesOfItem(atPath: url.path),
              let fileSize = attributes[.size] as? NSNumber else {
            return false
        }
        return fileSize.int64Value > 0
    }

    private static let legacyTrialCount = 3
    private static let recordingName = "chant-guide.m4a"
    private static let tempRecordingName = "chant-guide-recording.tmp"
    private static let selectingName = "chant-guide-selecting.tmp"
}
