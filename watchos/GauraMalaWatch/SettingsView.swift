import SwiftUI

struct SettingsView: View {
    @EnvironmentObject private var model: AppModel
    @Environment(\.dismiss) private var dismiss
    @State private var showingRoundReset = false
    @State private var showingTodayReset = false

    var body: some View {
        NavigationStack {
            List {
                Section("Counting") {
                    Toggle("Double Tap", isOn: Binding(
                        get: { model.state.isDoubleTapEnabled },
                        set: { model.updateSettings(isDoubleTapEnabled: $0) }
                    ))
                    Text("Uses the system primary action on supported Apple Watch models.")
                        .font(.footnote)
                        .foregroundStyle(.secondary)

                    HStack {
                        Text("Daily goal")
                        Spacer()
                        Button {
                            model.setDailyGoal(model.state.dailyGoalRounds - 1)
                        } label: {
                            Image(systemName: "minus.circle.fill")
                        }
                        .disabled(model.state.dailyGoalRounds <= 1)
                        .buttonStyle(.plain)
                        Text("\(model.state.dailyGoalRounds)")
                            .monospacedDigit()
                            .frame(minWidth: 22)
                        Button {
                            model.setDailyGoal(model.state.dailyGoalRounds + 1)
                        } label: {
                            Image(systemName: "plus.circle.fill")
                        }
                        .disabled(model.state.dailyGoalRounds >= 64)
                        .buttonStyle(.plain)
                    }
                }

                Section("Feedback") {
                    Toggle("Haptic feedback", isOn: Binding(
                        get: { model.state.isHapticsEnabled },
                        set: { model.updateSettings(isHapticsEnabled: $0) }
                    ))
                    Toggle("Milestone cues", isOn: Binding(
                        get: { model.state.areMilestonesEnabled },
                        set: { model.updateSettings(areMilestonesEnabled: $0) }
                    ))
                    .disabled(!model.state.isHapticsEnabled)
                }

                Section("Progress") {
                    Button("Reset this round", role: .destructive) {
                        showingRoundReset = true
                    }
                    .alert("Reset this round?", isPresented: $showingRoundReset) {
                        Button("Cancel", role: .cancel) {}
                        Button("Reset", role: .destructive) { model.resetCurrentRound() }
                    } message: {
                        Text("This clears bead progress for the current round.")
                    }

                    Button("Reset today's count", role: .destructive) {
                        showingTodayReset = true
                    }
                    .alert("Reset today's count?", isPresented: $showingTodayReset) {
                        Button("Cancel", role: .cancel) {}
                        Button("Reset", role: .destructive) { model.resetToday() }
                    } message: {
                        Text("This clears completed rounds and bead progress for today.")
                    }
                }

                Section("About") {
                    NavigationLink("Privacy policy") {
                        PrivacyPolicyView()
                    }
                    Link(destination: URL(string: "https://github.com/ajinkyakulkarni26/gaura-mala/issues")!) {
                        Label("Contact developer", systemImage: "bubble.left")
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
