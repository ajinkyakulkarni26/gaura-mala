import SwiftUI

struct PrivacyPolicyView: View {
    private static let gold = Color(red: 1.0, green: 0.69, blue: 0.12)

    private let sections: [(title: String, body: String)] = [
        (
            "What stays on this watch",
            "Gaura Mala has no account, ads, or analytics. Your counts, goal, settings, and saved Auto Count pace stay on this watch and are not sent to a developer-operated server."
        ),
        (
            "Optional chant recording",
            "If you choose to record, the microphone captures one chant, up to 20 seconds, as an Auto Count guide. Pace setup only measures time; it records no audio. The saved chant stays in private app storage on this watch, is not uploaded or shared by Gaura Mala, and is excluded from backup. You can play, replace, or delete it in Auto Count settings. Canceling an unfinished take discards it. A saved take remains until you delete it, clear app storage, or uninstall Gaura Mala. Record only a voice you have permission to record and use."
        ),
        (
            "Daily reset and deletion",
            "Counts reset each day. Settings, saved pace, and a saved chant remain until changed or deleted, app storage is cleared, or the app is uninstalled. Reset today's count in Settings or delete a chant in Auto Count settings."
        ),
        (
            "Permissions and sharing",
            "Microphone access is requested only after you choose to record and agree to the in-app disclosure. Apple Watch haptics support counting cues. Gaura Mala does not request location, contacts, camera, or health data. If you open GitHub, GitHub's privacy practices apply to that service."
        ),
        (
            "Children and voice recordings",
            "A recording contains the speaker's voice. For a child, a parent or guardian should decide and help with recording and deletion. Gaura Mala does not ask for a child's age or a parent's contact details. The recording stays on the watch until deleted, app storage is cleared, or the app is uninstalled."
        ),
        (
            "Policy updates",
            "If Gaura Mala's privacy practices change, we will update this policy and its date before those changes take effect."
        ),
        (
            "Contact",
            "For privacy questions, open a GitHub issue at github.com/ajinkyakulkarni26/gaura-mala/issues. Don't post private details in public issues."
        )
    ]

    var body: some View {
        ScrollView {
            VStack(spacing: 10) {
                ForEach(sections, id: \.title) { section in
                    VStack(spacing: 4) {
                        Text(section.title)
                            .font(.system(size: 12, weight: .semibold, design: .rounded))
                            .foregroundStyle(Self.gold)
                            .multilineTextAlignment(.center)
                        Text(section.body)
                            .font(.system(size: 10))
                            .foregroundStyle(.white)
                            .multilineTextAlignment(.center)
                    }
                    .padding(.horizontal, 6)
                }
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 8)
        }
        .navigationTitle("Privacy Policy")
    }
}
