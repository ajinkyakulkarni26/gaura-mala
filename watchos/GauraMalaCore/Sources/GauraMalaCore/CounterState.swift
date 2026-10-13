import Foundation

public struct CounterState: Codable, Equatable, Sendable {
    public var beadCount: Int
    public var completedRounds: Int
    public var dailyGoalRounds: Int
    public var isDoubleTapEnabled: Bool
    public var isHapticsEnabled: Bool
    public var areMilestonesEnabled: Bool
    public var lastRecordedDate: String
    public var canUndo: Bool
    public var showGoalReached: Bool

    public init(
        beadCount: Int = 0,
        completedRounds: Int = 0,
        dailyGoalRounds: Int = 16,
        isDoubleTapEnabled: Bool = true,
        isHapticsEnabled: Bool = true,
        areMilestonesEnabled: Bool = true,
        lastRecordedDate: String = "",
        canUndo: Bool = false,
        showGoalReached: Bool = false
    ) {
        self.beadCount = min(max(beadCount, 0), 107)
        self.completedRounds = min(max(completedRounds, 0), 999)
        self.dailyGoalRounds = min(max(dailyGoalRounds, 1), 64)
        self.isDoubleTapEnabled = isDoubleTapEnabled
        self.isHapticsEnabled = isHapticsEnabled
        self.areMilestonesEnabled = areMilestonesEnabled
        self.lastRecordedDate = lastRecordedDate
        self.canUndo = canUndo
        self.showGoalReached = showGoalReached
    }

    private enum CodingKeys: String, CodingKey {
        case beadCount
        case completedRounds
        case dailyGoalRounds
        case isDoubleTapEnabled
        case isHapticsEnabled
        case areMilestonesEnabled
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
            isHapticsEnabled: try values.decodeIfPresent(Bool.self, forKey: .isHapticsEnabled) ?? true,
            areMilestonesEnabled: try values.decodeIfPresent(Bool.self, forKey: .areMilestonesEnabled) ?? true,
            lastRecordedDate: try values.decodeIfPresent(String.self, forKey: .lastRecordedDate) ?? "",
            canUndo: false,
            showGoalReached: false
        )
    }

    public var currentRound: Int { completedRounds + 1 }
    public var isGoalAchieved: Bool { completedRounds >= dailyGoalRounds }
    public var roundProgress: Double { Double(beadCount) / 108.0 }
}

public enum CounterCue: Equatable, Sendable {
    case bead
    case milestone
    case roundCompleted
    case dailyGoalAchieved
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
        state.showGoalReached = previousState.showGoalReached
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

    public mutating func updateSettings(
        isDoubleTapEnabled: Bool? = nil,
        isHapticsEnabled: Bool? = nil,
        areMilestonesEnabled: Bool? = nil
    ) {
        if let isDoubleTapEnabled { state.isDoubleTapEnabled = isDoubleTapEnabled }
        if let isHapticsEnabled { state.isHapticsEnabled = isHapticsEnabled }
        if let areMilestonesEnabled { state.areMilestonesEnabled = areMilestonesEnabled }
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
