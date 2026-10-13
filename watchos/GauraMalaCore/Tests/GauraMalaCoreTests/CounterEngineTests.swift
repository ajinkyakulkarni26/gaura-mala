import XCTest
@testable import GauraMalaCore

final class CounterEngineTests: XCTestCase {
    func testCounterStartsAtFirstBeadOfRoundOne() {
        let engine = CounterEngine()
        XCTAssertEqual(engine.state.beadCount, 0)
        XCTAssertEqual(engine.state.currentRound, 1)
        XCTAssertEqual(engine.state.dailyGoalRounds, 16)
    }

    func testOrdinaryCountAdvancesOneBeadAndEnablesUndo() {
        var engine = CounterEngine()
        XCTAssertEqual(engine.countBead(), .bead)
        XCTAssertEqual(engine.state.beadCount, 1)
        XCTAssertTrue(engine.state.canUndo)
    }

    func testMilestonesAreAtQuarterMalaIntervals() {
        for milestone in [27, 54, 81] {
            var engine = CounterEngine(state: CounterState(beadCount: milestone - 1))
            XCTAssertEqual(engine.countBead(), .milestone)
            XCTAssertEqual(engine.state.beadCount, milestone)
        }
    }

    func testMilestoneCueCanBeDisabled() {
        var engine = CounterEngine(state: CounterState(beadCount: 26, areMilestonesEnabled: false))
        XCTAssertEqual(engine.countBead(), .bead)
    }

    func test108thBeadCompletesRoundAndResetsBeads() {
        var engine = CounterEngine(state: CounterState(beadCount: 107, dailyGoalRounds: 16))
        XCTAssertEqual(engine.countBead(), .roundCompleted)
        XCTAssertEqual(engine.state.beadCount, 0)
        XCTAssertEqual(engine.state.completedRounds, 1)
        XCTAssertEqual(engine.state.currentRound, 2)
    }

    func testDailyGoalCueTakesPriorityOnLastRound() {
        var engine = CounterEngine(state: CounterState(beadCount: 107, completedRounds: 15, dailyGoalRounds: 16))
        XCTAssertEqual(engine.countBead(), .dailyGoalAchieved)
        XCTAssertTrue(engine.state.isGoalAchieved)
        XCTAssertTrue(engine.state.showGoalReached)
    }

    func testRoundsAfterDailyGoalDoNotRepeatGoalCue() {
        var engine = CounterEngine(state: CounterState(beadCount: 107, completedRounds: 16, dailyGoalRounds: 16))
        XCTAssertEqual(engine.countBead(), .roundCompleted)
        XCTAssertFalse(engine.state.showGoalReached)
    }

    func testGoalAlertCanBeDismissed() {
        var engine = CounterEngine(state: CounterState(beadCount: 107, completedRounds: 15, dailyGoalRounds: 16))
        _ = engine.countBead()
        engine.dismissGoalReached()
        XCTAssertFalse(engine.state.showGoalReached)
        XCTAssertTrue(engine.state.isGoalAchieved)
    }

    func testUndoRestoresBeadProgress() {
        var engine = CounterEngine(state: CounterState(beadCount: 20))
        _ = engine.countBead()
        XCTAssertTrue(engine.undo())
        XCTAssertEqual(engine.state.beadCount, 20)
        XCTAssertFalse(engine.state.canUndo)
    }

    func testUndoRestoresCompletedRound() {
        var engine = CounterEngine(state: CounterState(beadCount: 107, completedRounds: 3))
        _ = engine.countBead()
        XCTAssertTrue(engine.undo())
        XCTAssertEqual(engine.state.beadCount, 107)
        XCTAssertEqual(engine.state.completedRounds, 3)
    }

    func testUndoCanOnlyBeUsedOncePerCount() {
        var engine = CounterEngine()
        _ = engine.countBead()
        XCTAssertTrue(engine.undo())
        XCTAssertFalse(engine.undo())
    }

    func testUndoIsUnavailableAfterResettingRound() {
        var engine = CounterEngine(state: CounterState(beadCount: 4))
        _ = engine.countBead()
        engine.resetCurrentRound()
        XCTAssertFalse(engine.undo())
        XCTAssertEqual(engine.state.beadCount, 0)
    }

    func testResetCurrentRoundKeepsCompletedRounds() {
        var engine = CounterEngine(state: CounterState(beadCount: 56, completedRounds: 7))
        engine.resetCurrentRound()
        XCTAssertEqual(engine.state.completedRounds, 7)
        XCTAssertEqual(engine.state.beadCount, 0)
    }

    func testManualRoundAdjustmentClearsPartialProgressAndUndo() {
        var engine = CounterEngine(state: CounterState(beadCount: 38, completedRounds: 2))
        _ = engine.countBead()
        XCTAssertTrue(engine.setCompletedRounds(6))
        XCTAssertEqual(engine.state.completedRounds, 6)
        XCTAssertEqual(engine.state.beadCount, 0)
        XCTAssertFalse(engine.state.canUndo)
    }

    func testManualRoundAdjustmentRejectsOutOfRangeValues() {
        var engine = CounterEngine(state: CounterState(beadCount: 12, completedRounds: 2))
        XCTAssertFalse(engine.setCompletedRounds(-1))
        XCTAssertFalse(engine.setCompletedRounds(1_000))
        XCTAssertEqual(engine.state.completedRounds, 2)
        XCTAssertEqual(engine.state.beadCount, 12)
    }

    func testDailyGoalAcceptsOneThrough64AndRejectsInvalidValues() {
        var engine = CounterEngine()
        XCTAssertTrue(engine.setDailyGoal(1))
        XCTAssertTrue(engine.setDailyGoal(64))
        XCTAssertFalse(engine.setDailyGoal(0))
        XCTAssertFalse(engine.setDailyGoal(65))
        XCTAssertEqual(engine.state.dailyGoalRounds, 64)
    }

    func testSettingsCanBeUpdatedWithoutLosingProgress() {
        var engine = CounterEngine(state: CounterState(beadCount: 41, completedRounds: 3))
        engine.updateSettings(isDoubleTapEnabled: false, isHapticsEnabled: false, areMilestonesEnabled: false)
        XCTAssertEqual(engine.state.beadCount, 41)
        XCTAssertEqual(engine.state.completedRounds, 3)
        XCTAssertFalse(engine.state.isDoubleTapEnabled)
        XCTAssertFalse(engine.state.isHapticsEnabled)
        XCTAssertFalse(engine.state.areMilestonesEnabled)
    }

    func testResetTodayKeepsPreferencesAndUpdatesDate() {
        var engine = CounterEngine(state: CounterState(
            beadCount: 70,
            completedRounds: 5,
            dailyGoalRounds: 20,
            isHapticsEnabled: false
        ))
        engine.resetToday(date: "2026-10-12")
        XCTAssertEqual(engine.state.beadCount, 0)
        XCTAssertEqual(engine.state.completedRounds, 0)
        XCTAssertEqual(engine.state.dailyGoalRounds, 20)
        XCTAssertFalse(engine.state.isHapticsEnabled)
        XCTAssertEqual(engine.state.lastRecordedDate, "2026-10-12")
    }

    func testDayRolloverOnlyResetsOncePerLocalDate() {
        var engine = CounterEngine(state: CounterState(beadCount: 17, completedRounds: 2, lastRecordedDate: "2026-10-11"))
        XCTAssertTrue(engine.rollOverIfNeeded(to: "2026-10-12"))
        XCTAssertEqual(engine.state.beadCount, 0)
        XCTAssertEqual(engine.state.completedRounds, 0)
        XCTAssertFalse(engine.rollOverIfNeeded(to: "2026-10-12"))
    }

    func testStateClampsMalformedPersistedCounts() {
        let state = CounterState(beadCount: 300, completedRounds: -1, dailyGoalRounds: 500)
        XCTAssertEqual(state.beadCount, 107)
        XCTAssertEqual(state.completedRounds, 0)
        XCTAssertEqual(state.dailyGoalRounds, 64)
    }

    func testDecodedStateClampsCountsAndDoesNotRestoreTransientUndoOrAlert() throws {
        let data = Data(#"{"beadCount":900,"completedRounds":-4,"dailyGoalRounds":100,"canUndo":true,"showGoalReached":true}"#.utf8)
        let state = try JSONDecoder().decode(CounterState.self, from: data)
        XCTAssertEqual(state.beadCount, 107)
        XCTAssertEqual(state.completedRounds, 0)
        XCTAssertEqual(state.dailyGoalRounds, 64)
        XCTAssertFalse(state.canUndo)
        XCTAssertFalse(state.showGoalReached)
        XCTAssertTrue(state.isDoubleTapEnabled)
        XCTAssertTrue(state.isHapticsEnabled)
    }
}
