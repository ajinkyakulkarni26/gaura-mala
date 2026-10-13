import SwiftUI

struct PrivacyPolicyView: View {
    private let policy = """
    Gaura Mala stores your bead count, completed rounds, daily goal, and settings on this watch so they are available when you reopen the app.

    The app does not send this information to a server. It has no account, ads, or analytics.

    Counts reset when the local calendar date changes. Preferences remain until you clear the app's data or uninstall it.

    The app uses Apple Watch haptics for counting feedback. It does not request location, contacts, microphone, camera, or health data.

    Privacy questions: github.com/ajinkyakulkarni26/gaura-mala/issues
    """

    var body: some View {
        ScrollView {
            Text(policy)
                .font(.footnote)
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.horizontal, 8)
                .padding(.vertical, 4)
        }
        .navigationTitle("Privacy")
    }
}
