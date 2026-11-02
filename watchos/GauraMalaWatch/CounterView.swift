import SwiftUI
import WatchKit

private enum MalaColor {
    static let gold = Color(red: 1.0, green: 0.69, blue: 0.12)
    static let brightGold = Color(red: 1.0, green: 0.84, blue: 0.42)
    static let muted = Color(red: 0.45, green: 0.43, blue: 0.39)
    static let panel = Color(red: 0.12, green: 0.12, blue: 0.12)
}

struct CounterView: View {
    @EnvironmentObject private var model: AppModel
    @Environment(\.scenePhase) private var scenePhase
    @Environment(\.isLuminanceReduced) private var isAmbient
    @State private var showingSettings = false
    @State private var showingAutoChantSetup = false
    @State private var showingRoundPicker = false
    @State private var crownValue = 0.0

    var body: some View {
        GeometryReader { proxy in
            let ringDiameter = min(proxy.size.width * 0.84, proxy.size.height * 0.68)

            VStack(spacing: 2) {
                if isAmbient {
                    Text(model.state.isGoalAchieved ? "Goal" : "R \(model.state.currentRound)")
                        .font(.system(size: 12, weight: .semibold, design: .rounded))
                        .foregroundStyle(.white)
                } else {
                    Button {
                        showingRoundPicker = true
                    } label: {
                        HStack(spacing: 4) {
                            Text(model.state.isGoalAchieved ? "Daily goal reached" : "Round \(model.state.currentRound) of \(model.state.dailyGoalRounds)")
                                .lineLimit(1)
                                .minimumScaleFactor(0.72)
                            Image(systemName: "pencil")
                                .font(.system(size: 9, weight: .semibold))
                        }
                        .font(.system(size: 11, weight: .semibold, design: .rounded))
                        .foregroundStyle(MalaColor.brightGold)
                        .padding(.horizontal, 8)
                        .frame(minHeight: 32)
                        .background(MalaColor.panel, in: Capsule())
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel("Adjust completed rounds today")
                }

                countButton(ringDiameter: ringDiameter)

                if !isAmbient {
                    HStack {
                        if model.autoCountActive {
                            Button {
                                if model.autoCountRunning {
                                    model.pauseAutoCount()
                                } else {
                                    model.resumeAutoCount()
                                }
                            } label: {
                                Image(systemName: model.autoCountRunning ? "pause.fill" : "play.fill")
                                    .font(.system(size: 15, weight: .semibold))
                                    .foregroundStyle(MalaColor.gold)
                                    .frame(width: 48, height: 48)
                                    .background(MalaColor.panel, in: Circle())
                            }
                            .buttonStyle(.plain)
                            .accessibilityLabel(model.autoCountRunning ? "Pause auto count" : "Resume auto count")
                        } else {
                            Button(action: model.undo) {
                                Image(systemName: "arrow.uturn.backward")
                                    .font(.system(size: 15, weight: .semibold))
                                    .foregroundStyle(model.state.canUndo ? MalaColor.gold : .white.opacity(0.35))
                                    .frame(width: 48, height: 48)
                                    .background(MalaColor.panel, in: Circle())
                            }
                            .buttonStyle(.plain)
                            .disabled(!model.state.canUndo)
                            .accessibilityLabel("Undo last bead")
                        }

                        Spacer(minLength: 8)

                        Button { showingSettings = true } label: {
                            Image(systemName: "gearshape.fill")
                                .font(.system(size: 15, weight: .semibold))
                                .foregroundStyle(MalaColor.brightGold)
                                .frame(width: 48, height: 48)
                                .background(MalaColor.panel, in: Circle())
                        }
                        .buttonStyle(.plain)
                        .accessibilityLabel("Open Settings")
                    }
                    .padding(.horizontal, 12)
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(Color.black)
            .digitalCrownRotation(
                $crownValue,
                from: -100_000.0,
                through: 100_000.0,
                by: 1.0,
                sensitivity: .low,
                isContinuous: false,
                isHapticFeedbackEnabled: true
            )
            .onChange(of: crownValue) { oldValue, newValue in
                guard !isAmbient, !model.autoCountRunning else { return }
                let steps = Int(abs(newValue - oldValue).rounded())
                for _ in 0..<steps { model.countBeadFromCrown() }
            }
        }
        .background(Color.black)
        .onChange(of: scenePhase) { _, phase in
            if phase == .active {
                model.refreshForCurrentDay()
            } else if phase == .background {
                model.pauseAutoCount()
                model.cancelVoiceRecording()
            }
        }
        .sheet(isPresented: $showingSettings) {
            SettingsView(onOpenAutoCount: {
                showingSettings = false
                showingAutoChantSetup = true
            })
            .environmentObject(model)
        }
        .sheet(isPresented: $showingAutoChantSetup) {
            AutoChantSetupView(
                onBack: { showingAutoChantSetup = false },
                onStartCount: { showingAutoChantSetup = false }
            )
            .environmentObject(model)
        }
        .sheet(isPresented: $showingRoundPicker) {
            RoundPickerView()
                .environmentObject(model)
        }
        .alert("Daily goal reached", isPresented: $model.isShowingGoalAlert) {
            Button("Continue", role: .cancel) { model.dismissGoalAlert() }
        } message: {
            Text("You completed \(model.state.dailyGoalRounds) rounds today. You can keep chanting.")
        }
    }

    private var inputHint: String {
        if model.autoCountRunning && model.autoCountUsesVoiceGuide {
            return "Following your chant"
        }
        if model.autoCountRunning {
            if let paceMillis = model.state.autoChantPaceMillis {
                return String(format: "Auto count · %.1f sec", Double(paceMillis) / 1_000.0)
            }
            return "Auto count running"
        }
        if model.autoCountActive {
            return "Auto count paused"
        }
        if model.state.isDoubleTapEnabled && model.state.isScreenTapEnabled {
            return "Tap or double tap to count"
        }
        if model.state.isDoubleTapEnabled {
            return "Double tap to count"
        }
        if model.state.isScreenTapEnabled {
            return "Tap to count"
        }
        return "Turn the crown to count"
    }

    @ViewBuilder
    private func countButton(ringDiameter: CGFloat) -> some View {
        let content = ZStack {
            BeadProgressRing(
                beadCount: model.state.beadCount,
                completedRounds: model.state.completedRounds,
                dailyGoal: model.state.dailyGoalRounds,
                isAmbient: isAmbient
            )
            VStack(spacing: 1) {
                Text("\(model.state.beadCount)")
                    .font(.system(size: ringDiameter * 0.28, weight: .bold, design: .rounded).monospacedDigit())
                    .foregroundStyle(isAmbient ? .white : MalaColor.gold)
                    .contentTransition(.numericText())
                Text("/ 108")
                    .font(.system(size: 11, weight: .medium, design: .rounded))
                    .foregroundStyle(isAmbient ? .gray : .white.opacity(0.68))
                if !isAmbient {
                    Text(inputHint)
                        .font(.system(size: 9, weight: .regular, design: .rounded))
                        .foregroundStyle(.white.opacity(0.72))
                        .lineLimit(1)
                        .minimumScaleFactor(0.75)
                }
            }
            .padding(.horizontal, 20)
        }
        .frame(width: ringDiameter, height: ringDiameter)
        .contentShape(Circle())

        let button = Button {
            guard !isAmbient, !model.autoCountRunning else { return }
            if model.state.isScreenTapEnabled {
                model.countBead()
            } else if model.state.isDoubleTapEnabled {
                model.countBeadFromGesture()
            }
        } label: {
            content
        }
        .buttonStyle(.plain)
        .disabled(model.autoCountRunning || (!model.state.isScreenTapEnabled && !model.state.isDoubleTapEnabled))
        .accessibilityLabel("Count a bead. Bead \(model.state.beadCount) of 108")
        .accessibilityHint("Tap or double-tap to count. Rotate the Digital Crown to count as well.")

        if #available(watchOS 11.0, *) {
            button.handGestureShortcut(
                .primaryAction,
                isEnabled: model.state.isDoubleTapEnabled && !model.autoCountRunning
            )
        } else {
            button
        }
    }
}

private struct BeadProgressRing: View {
    let beadCount: Int
    let completedRounds: Int
    let dailyGoal: Int
    let isAmbient: Bool

    var body: some View {
        Canvas { context, size in
            let center = CGPoint(x: size.width / 2, y: size.height / 2)
            let radius = min(size.width, size.height) * 0.44
            let baseBeadRadius = max(1.15, min(size.width, size.height) * 0.012)

            for index in 0..<108 {
                let angle = (Double(index) / 108.0) * 2.0 * Double.pi - Double.pi / 2.0
                let point = CGPoint(
                    x: center.x + CGFloat(cos(angle)) * radius,
                    y: center.y + CGFloat(sin(angle)) * radius
                )
                let isMeru = index == 0
                let isReached = isMeru ? (beadCount >= 107) : (index <= beadCount && beadCount > 0)
                let isMilestone = index == 27 || index == 54 || index == 81
                let beadRadius: CGFloat = if isMeru {
                    baseBeadRadius * 1.45
                } else if isReached && isMilestone {
                    baseBeadRadius * 1.3
                } else if isReached {
                    baseBeadRadius * 1.1
                } else {
                    baseBeadRadius * 0.9
                }
                let rect = CGRect(
                    x: point.x - beadRadius,
                    y: point.y - beadRadius,
                    width: beadRadius * 2,
                    height: beadRadius * 2
                )
                let bead = Path(ellipseIn: rect)
                let color: Color = if isAmbient && isReached {
                    .white
                } else if isAmbient {
                    Color(white: 0.27)
                } else if isMeru {
                    isReached ? MalaColor.brightGold : MalaColor.muted
                } else if isReached && isMilestone {
                    Color(red: 0.25, green: 0.82, blue: 0.88)
                } else if isReached {
                    MalaColor.gold
                } else if isMilestone {
                    Color(red: 0.17, green: 0.42, blue: 0.45)
                } else {
                    MalaColor.muted.opacity(0.48)
                }
                context.fill(bead, with: .color(color))
            }

            guard !isAmbient, dailyGoal > 0 else { return }
            let totalDots = min(max(dailyGoal, 1), 24)
            let innerRadius = radius * 0.72
            let dotRadius = max(0.75, baseBeadRadius * 0.62)
            for index in 0..<totalDots {
                let angle = (Double(index) / Double(totalDots)) * 2.0 * Double.pi - Double.pi / 2.0
                let point = CGPoint(
                    x: center.x + CGFloat(cos(angle)) * innerRadius,
                    y: center.y + CGFloat(sin(angle)) * innerRadius
                )
                let rect = CGRect(x: point.x - dotRadius, y: point.y - dotRadius, width: dotRadius * 2, height: dotRadius * 2)
                let dot = Path(ellipseIn: rect)
                let completed = Double(completedRounds) / Double(max(dailyGoal, 1)) >= Double(index + 1) / Double(totalDots)
                context.fill(dot, with: .color(completed ? MalaColor.gold : MalaColor.muted.opacity(0.42)))
            }
        }
        .accessibilityHidden(true)
    }
}
