import SwiftUI

private enum ResetTarget {
    case currentRound
    case dailyCount
}

struct SettingsView: View {
    @EnvironmentObject private var model: AppModel
    @Environment(\.dismiss) private var dismiss
    var onOpenAutoCount: () -> Void = {}
    @State private var pendingReset: ResetTarget?

    var body: some View {
        NavigationStack {
            if let pendingReset {
                resetConfirmationView(for: pendingReset)
            } else {
                List {
                    Section {
                        Button(action: onOpenAutoCount) {
                            VStack(alignment: .leading, spacing: 2) {
                                Text("Auto Count")
                                    .font(.headline)
                                Text("Set pace or record a chant")
                                    .font(.footnote)
                                    .foregroundStyle(.secondary)
                            }
                        }
                    }

                    Section("Counting") {
                        HStack {
                            Text("Daily goal")
                            Spacer()
                            Button {
                                model.setDailyGoal(model.state.dailyGoalRounds - 1)
                            } label: {
                                Image(systemName: "minus.circle.fill")
                                    .font(.system(size: 22))
                                    .frame(width: 44, height: 44)
                            }
                            .disabled(model.state.dailyGoalRounds <= 1)
                            .buttonStyle(.plain)
                            .accessibilityLabel("Decrease daily goal")

                            Text("\(model.state.dailyGoalRounds)")
                                .monospacedDigit()
                                .frame(minWidth: 24)

                            Button {
                                model.setDailyGoal(model.state.dailyGoalRounds + 1)
                            } label: {
                                Image(systemName: "plus.circle.fill")
                                    .font(.system(size: 22))
                                    .frame(width: 44, height: 44)
                            }
                            .disabled(model.state.dailyGoalRounds >= 64)
                            .buttonStyle(.plain)
                            .accessibilityLabel("Increase daily goal")
                        }

                        Toggle("Double Tap", isOn: Binding(
                            get: { model.state.isDoubleTapEnabled },
                            set: { model.updateSettings(isDoubleTapEnabled: $0) }
                        ))
                        Text("Uses the system primary action on supported Apple Watch models.")
                            .font(.footnote)
                            .foregroundStyle(.secondary)

                        Toggle("Screen Tap", isOn: Binding(
                            get: { model.state.isScreenTapEnabled },
                            set: { model.updateSettings(isScreenTapEnabled: $0) }
                        ))
                        Text("Tap to count.")
                            .font(.footnote)
                            .foregroundStyle(.secondary)
                    }

                    Section("Feedback") {
                        Toggle("Haptics", isOn: Binding(
                            get: { model.state.isHapticsEnabled },
                            set: { model.updateSettings(isHapticsEnabled: $0) }
                        ))
                        Toggle("Milestones", isOn: Binding(
                            get: { model.state.areMilestonesEnabled },
                            set: { model.updateSettings(areMilestonesEnabled: $0) }
                        ))
                        .disabled(!model.state.isHapticsEnabled)
                        Text("Ticks at 27, 54, 81.")
                            .font(.footnote)
                            .foregroundStyle(.secondary)
                    }

                    Section("Progress") {
                        Button("Reset Round") {
                            pendingReset = .currentRound
                        }

                        Button("Reset Today", role: .destructive) {
                            pendingReset = .dailyCount
                        }
                    }

                    Section("About") {
                        NavigationLink("Privacy Policy") {
                            PrivacyPolicyView()
                        }
                        Link(destination: URL(string: "https://github.com/ajinkyakulkarni26/gaura-mala/issues")!) {
                            Label("Contact Developer", systemImage: "bubble.left")
                        }
                    }
                }
                .navigationTitle("Settings")
                .toolbar {
                    ToolbarItem(placement: .topBarTrailing) {
                        Button("Done") { dismiss() }
                    }
                }
            }
        }
    }

    @ViewBuilder
    private func resetConfirmationView(for target: ResetTarget) -> some View {
        let isDaily = target == .dailyCount
        VStack(spacing: 10) {
            Text(isDaily ? "Reset today's count?" : "Reset this round?")
                .font(.headline)
                .foregroundStyle(.red)
                .multilineTextAlignment(.center)
            Text(isDaily ? "Clears today's rounds and beads." : "Clears beads in this round.")
                .font(.footnote)
                .multilineTextAlignment(.center)
            HStack(spacing: 16) {
                Button {
                    pendingReset = nil
                } label: {
                    Image(systemName: "xmark")
                        .font(.system(size: 18, weight: .bold))
                        .foregroundStyle(.white)
                        .frame(width: 48, height: 48)
                        .background(Color(white: 0.2), in: Circle())
                }
                .buttonStyle(.plain)
                .accessibilityLabel("Cancel reset")

                Button {
                    if isDaily {
                        model.resetToday()
                    } else {
                        model.resetCurrentRound()
                    }
                    pendingReset = nil
                    dismiss()
                } label: {
                    Image(systemName: "checkmark")
                        .font(.system(size: 18, weight: .bold))
                        .foregroundStyle(.white)
                        .frame(width: 48, height: 48)
                        .background(Color.red, in: Circle())
                }
                .buttonStyle(.plain)
                .accessibilityLabel("Confirm reset")
            }
        }
        .padding(12)
    }
}
