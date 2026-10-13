import SwiftUI
import WatchKit

struct RoundPickerView: View {
    @EnvironmentObject private var model: AppModel
    @Environment(\.dismiss) private var dismiss
    @State private var selectedRound: Double

    init() {
        _selectedRound = State(initialValue: 0)
    }

    var body: some View {
        VStack(spacing: 8) {
            Text("ROUNDS TODAY")
                .font(.system(size: 11, weight: .semibold, design: .rounded))
                .tracking(1)
                .foregroundStyle(.secondary)

            HStack(spacing: 18) {
                Button {
                    selectedRound = max(0, selectedRound - 1)
                    selectionTick()
                } label: {
                    Image(systemName: "minus.circle.fill")
                        .font(.system(size: 29))
                }
                .buttonStyle(.plain)
                .accessibilityLabel("Decrease rounds")

                Text("\(Int(selectedRound))")
                    .font(.system(size: 34, weight: .light, design: .rounded).monospacedDigit())
                    .contentTransition(.numericText())
                    .frame(minWidth: 48)
                    .accessibilityLabel("\(Int(selectedRound)) completed rounds")

                Button {
                    selectedRound = min(999, selectedRound + 1)
                    selectionTick()
                } label: {
                    Image(systemName: "plus.circle.fill")
                        .font(.system(size: 29))
                }
                .buttonStyle(.plain)
                .accessibilityLabel("Increase rounds")
            }
            .foregroundStyle(Color(red: 1.0, green: 0.69, blue: 0.12))

            Text("Set rounds chanted today, including with physical beads.")
                .font(.system(size: 11))
                .multilineTextAlignment(.center)
                .foregroundStyle(.secondary)

            HStack(spacing: 28) {
                Button {
                    dismiss()
                } label: {
                    Image(systemName: "xmark.circle.fill")
                        .font(.system(size: 27))
                        .foregroundStyle(.red.opacity(0.85))
                }
                .buttonStyle(.plain)
                .accessibilityLabel("Cancel round adjustment")

                Button {
                    model.setCompletedRounds(Int(selectedRound))
                    dismiss()
                } label: {
                    Image(systemName: "checkmark.circle.fill")
                        .font(.system(size: 27))
                        .foregroundStyle(.green)
                }
                .buttonStyle(.plain)
                .accessibilityLabel("Save completed rounds")
            }
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 8)
        .onAppear { selectedRound = Double(model.state.completedRounds) }
        .digitalCrownRotation(
            $selectedRound,
            from: 0.0,
            through: 999.0,
            by: 1.0,
            sensitivity: .medium,
            isContinuous: false,
            isHapticFeedbackEnabled: model.state.isHapticsEnabled
        )
    }

    private func selectionTick() {
        guard model.state.isHapticsEnabled else { return }
        WKInterfaceDevice.current().play(.click)
    }
}
