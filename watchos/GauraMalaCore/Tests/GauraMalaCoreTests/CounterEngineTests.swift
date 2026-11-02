import XCTest
@testable import GauraMalaCore

final class CounterEngineTests: XCTestCase {
    func testCounterStartsAtFirstBeadOfRoundOne() {
        let engine = CounterEngine()
        XCTAssertEqual(engine.state.beadCount, 0)
        XCTAssertEqual(engine.state.currentRound, 1)
        XCTAssertEqual(engine.state.dailyGoalRounds, 16)
        XCTAssertTrue(engine.state.isDoubleTapEnabled)
        XCTAssertTrue(engine.state.isScreenTapEnabled)
        XCTAssertNil(engine.state.autoChantPaceMillis)
        XCTAssertEqual(engine.state.totalMantrasChanted, 0)
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

    func testUndoRestoresCompletedRoundAndClearsGoalAlertWhenSteppingBelowGoal() {
        var engine = CounterEngine(state: CounterState(beadCount: 107, completedRounds: 15, dailyGoalRounds: 16))
        XCTAssertEqual(engine.countBead(), .dailyGoalAchieved)
        XCTAssertTrue(engine.state.showGoalReached)
        XCTAssertTrue(engine.undo())
        XCTAssertEqual(engine.state.beadCount, 107)
        XCTAssertEqual(engine.state.completedRounds, 15)
        XCTAssertFalse(engine.state.showGoalReached)
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

    func testSettingsAndAutoChantPaceCanBeUpdatedWithoutLosingProgress() {
        var engine = CounterEngine(state: CounterState(beadCount: 41, completedRounds: 3))
        engine.updateSettings(
            isDoubleTapEnabled: false,
            isScreenTapEnabled: false,
            isHapticsEnabled: false,
            areMilestonesEnabled: false
        )
        XCTAssertTrue(engine.setAutoChantPaceMillis(4_500))
        XCTAssertFalse(engine.setAutoChantPaceMillis(200))
        XCTAssertEqual(engine.state.beadCount, 41)
        XCTAssertEqual(engine.state.completedRounds, 3)
        XCTAssertFalse(engine.state.isDoubleTapEnabled)
        XCTAssertFalse(engine.state.isScreenTapEnabled)
        XCTAssertFalse(engine.state.isHapticsEnabled)
        XCTAssertFalse(engine.state.areMilestonesEnabled)
        XCTAssertEqual(engine.state.autoChantPaceMillis, 4_500)
        XCTAssertTrue(engine.setAutoChantPaceMillis(nil))
        XCTAssertNil(engine.state.autoChantPaceMillis)
    }

    func testResetTodayKeepsPreferencesAndUpdatesDate() {
        var engine = CounterEngine(state: CounterState(
            beadCount: 70,
            completedRounds: 5,
            dailyGoalRounds: 20,
            isScreenTapEnabled: false,
            isHapticsEnabled: false,
            autoChantPaceMillis: 3_000
        ))
        engine.resetToday(date: "2026-10-12")
        XCTAssertEqual(engine.state.beadCount, 0)
        XCTAssertEqual(engine.state.completedRounds, 0)
        XCTAssertEqual(engine.state.dailyGoalRounds, 20)
        XCTAssertFalse(engine.state.isScreenTapEnabled)
        XCTAssertFalse(engine.state.isHapticsEnabled)
        XCTAssertEqual(engine.state.autoChantPaceMillis, 3_000)
        XCTAssertEqual(engine.state.lastRecordedDate, "2026-10-12")
    }

    func testDayRolloverOnlyResetsOncePerLocalDate() {
        var engine = CounterEngine(state: CounterState(beadCount: 17, completedRounds: 2, lastRecordedDate: "2026-10-11"))
        XCTAssertTrue(engine.rollOverIfNeeded(to: "2026-10-12"))
        XCTAssertEqual(engine.state.beadCount, 0)
        XCTAssertEqual(engine.state.completedRounds, 0)
        XCTAssertFalse(engine.rollOverIfNeeded(to: "2026-10-12"))
    }

    func testStateClampsMalformedPersistedCountsAndDropsInvalidPace() {
        let state = CounterState(
            beadCount: 300,
            completedRounds: -1,
            dailyGoalRounds: 500,
            autoChantPaceMillis: 99_999
        )
        XCTAssertEqual(state.beadCount, 107)
        XCTAssertEqual(state.completedRounds, 0)
        XCTAssertEqual(state.dailyGoalRounds, 64)
        XCTAssertNil(state.autoChantPaceMillis)
    }

    func testDecodedStateClampsCountsAndDoesNotRestoreTransientUndoOrAlert() throws {
        let data = Data(#"{"beadCount":900,"completedRounds":-4,"dailyGoalRounds":100,"autoChantPaceMillis":5500,"canUndo":true,"showGoalReached":true}"#.utf8)
        let state = try JSONDecoder().decode(CounterState.self, from: data)
        XCTAssertEqual(state.beadCount, 107)
        XCTAssertEqual(state.completedRounds, 0)
        XCTAssertEqual(state.dailyGoalRounds, 64)
        XCTAssertEqual(state.autoChantPaceMillis, 5_500)
        XCTAssertFalse(state.canUndo)
        XCTAssertFalse(state.showGoalReached)
        XCTAssertTrue(state.isDoubleTapEnabled)
        XCTAssertTrue(state.isScreenTapEnabled)
        XCTAssertTrue(state.isHapticsEnabled)
    }

    func testHapticCuePolicyMatchesInputSourceRules() {
        var ordinaryEngine = CounterEngine(state: CounterState(beadCount: 10))
        let ordinaryCue = ordinaryEngine.countBead()
        XCTAssertNil(selectCounterCue(for: ordinaryCue, state: ordinaryEngine.state, source: .gesture))
        XCTAssertEqual(selectCounterCue(for: ordinaryCue, state: ordinaryEngine.state, source: .tap), .bead)
        XCTAssertEqual(selectCounterCue(for: ordinaryCue, state: ordinaryEngine.state, source: .rotary), .bead)
        XCTAssertEqual(selectCounterCue(for: ordinaryCue, state: ordinaryEngine.state, source: .auto), .bead)

        var milestoneEngine = CounterEngine(state: CounterState(beadCount: 26))
        let milestoneCue = milestoneEngine.countBead()
        XCTAssertEqual(selectCounterCue(for: milestoneCue, state: milestoneEngine.state, source: .gesture), .milestone)
        XCTAssertEqual(selectCounterCue(for: milestoneCue, state: milestoneEngine.state, source: .auto), .milestone)

        var roundEngine = CounterEngine(state: CounterState(beadCount: 107, completedRounds: 2))
        let roundCue = roundEngine.countBead()
        XCTAssertEqual(selectCounterCue(for: roundCue, state: roundEngine.state, source: .gesture), .roundCompleted)

        var goalEngine = CounterEngine(state: CounterState(beadCount: 107, dailyGoalRounds: 1))
        let goalCue = goalEngine.countBead()
        XCTAssertEqual(selectCounterCue(for: goalCue, state: goalEngine.state, source: .gesture), .dailyGoalAchieved)

        let mutedState = CounterState(isHapticsEnabled: false)
        XCTAssertNil(selectCounterCue(for: .dailyGoalAchieved, state: mutedState, source: .tap))
    }

    func testAutoChantTimingValidatesAndCalculatesMedianPace() {
        XCTAssertNil(AutoChantTiming.averagePaceMillis(totalElapsedMillis: 0, chantCount: 1))
        XCTAssertNil(AutoChantTiming.averagePaceMillis(totalElapsedMillis: -1, chantCount: 1))
        XCTAssertNil(AutoChantTiming.averagePaceMillis(totalElapsedMillis: 1_000, chantCount: 0))
        XCTAssertNil(AutoChantTiming.averagePaceMillis(totalElapsedMillis: 1_000, chantCount: 11))

        XCTAssertEqual(AutoChantTiming.averagePaceMillis(totalElapsedMillis: 10_000, chantCount: 5), 2_000)
        XCTAssertEqual(
            AutoChantTiming.averagePaceMillis(totalElapsedMillis: 100, chantCount: 1),
            AutoChantTiming.minPaceMillis
        )
        XCTAssertEqual(
            AutoChantTiming.averagePaceMillis(totalElapsedMillis: 100_000, chantCount: 1),
            AutoChantTiming.maxPaceMillis
        )

        XCTAssertTrue(AutoChantTiming.isValidPace(AutoChantTiming.minPaceMillis))
        XCTAssertTrue(AutoChantTiming.isValidPace(AutoChantTiming.maxPaceMillis))
        XCTAssertFalse(AutoChantTiming.isValidPace(AutoChantTiming.minPaceMillis - 1))
        XCTAssertFalse(AutoChantTiming.isValidPace(AutoChantTiming.maxPaceMillis + 1))

        XCTAssertEqual(AutoChantTiming.medianPaceMillis([9_000, 6_000, 3_000]), 6_000)
        XCTAssertNil(AutoChantTiming.medianPaceMillis([3_000, 6_000]))
        XCTAssertNil(AutoChantTiming.medianPaceMillis([3_000, 6_000, 16_000]))
    }

    func testAutoChantPacerCountsAtIntervalWithoutCatchUpBursts() {
        var pacer = AutoChantPacer()
        XCTAssertFalse(pacer.start(nowMillis: 1_000))
        XCTAssertNil(pacer.timeUntilNextCount(nowMillis: 1_000))
        XCTAssertFalse(pacer.consumeDueCount(nowMillis: 1_000))
        XCTAssertFalse(pacer.configure(paceMillis: AutoChantTiming.minPaceMillis - 1))
        XCTAssertFalse(pacer.configure(paceMillis: AutoChantTiming.maxPaceMillis + 1))

        XCTAssertTrue(pacer.configure(paceMillis: 2_000))
        XCTAssertTrue(pacer.start(nowMillis: 5_000))
        XCTAssertEqual(pacer.timeUntilNextCount(nowMillis: 5_000), 2_000)
        XCTAssertFalse(pacer.consumeDueCount(nowMillis: 6_999))
        XCTAssertTrue(pacer.consumeDueCount(nowMillis: 7_500))
        XCTAssertEqual(pacer.timeUntilNextCount(nowMillis: 7_500), 2_000)
        XCTAssertFalse(pacer.consumeDueCount(nowMillis: 9_499))
        XCTAssertTrue(pacer.consumeDueCount(nowMillis: 9_500))

        pacer.pause()
        XCTAssertFalse(pacer.isRunning)
        XCTAssertNil(pacer.timeUntilNextCount(nowMillis: 10_000))
        XCTAssertFalse(pacer.consumeDueCount(nowMillis: 10_000))

        XCTAssertTrue(pacer.start(nowMillis: 10_000))
        XCTAssertTrue(pacer.configure(paceMillis: 3_000))
        XCTAssertFalse(pacer.isRunning)
    }

    func testVoiceChantFilesManagesPublishedAndTemporaryRecordings() throws {
        let directory = FileManager.default.temporaryDirectory
            .appendingPathComponent("gaura-chant-files-\(UUID().uuidString)")
        try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        defer { try? FileManager.default.removeItem(at: directory) }

        let files = VoiceChantFiles(directory: directory)
        XCTAssertFalse(files.hasRecording())
        XCTAssertFalse(files.publishTemporaryRecording())

        let original = Data([1, 2, 3])
        try original.write(to: files.temporaryRecordingURL())
        XCTAssertTrue(files.publishTemporaryRecording())
        XCTAssertTrue(files.hasRecording())
        XCTAssertFalse(FileManager.default.fileExists(atPath: files.temporaryRecordingURL().path))

        // An empty temporary file must not overwrite the saved recording.
        try Data().write(to: files.temporaryRecordingURL())
        XCTAssertFalse(files.publishTemporaryRecording())
        XCTAssertEqual(try Data(contentsOf: files.recordingURL), original)

        // A valid replacement replaces the saved recording and consumes the temp file.
        let replacement = Data([4, 5, 6, 7])
        try replacement.write(to: files.temporaryRecordingURL())
        XCTAssertTrue(files.publishTemporaryRecording())
        XCTAssertEqual(try Data(contentsOf: files.recordingURL), replacement)
        XCTAssertFalse(FileManager.default.fileExists(atPath: files.temporaryRecordingURL().path))

        // Deleting all removes both saved and temporary recordings.
        try Data([8, 9]).write(to: files.temporaryRecordingURL())
        files.deleteAll()
        XCTAssertFalse(files.hasRecording())
        XCTAssertFalse(FileManager.default.fileExists(atPath: files.temporaryRecordingURL().path))
    }
}
