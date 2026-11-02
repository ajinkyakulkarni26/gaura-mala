import SwiftUI
import UserNotifications
import WatchKit

private enum AutoChantColor {
    static let gold = Color(red: 1.0, green: 0.69, blue: 0.12)
    static let secondary = Color(red: 0.20, green: 0.20, blue: 0.20)
    static let muted = Color(red: 0.72, green: 0.72, blue: 0.72)
    static let alertRed = Color(red: 0.90, green: 0.25, blue: 0.25)
}

private enum AutoChantPage {
    case choose
    case options
    case pace
    case voice
    case notifications
}

private enum GuidedStep {
    case intro
    case countdown
    case chanting
    case takeSaved
    case result
}

struct AutoChantSetupView: View {
    @EnvironmentObject private var model: AppModel
    let onBack: () -> Void
    let onStartCount: () -> Void

    @State private var page: AutoChantPage = .choose
    @State private var returnPage: AutoChantPage = .choose
    @State private var notificationReturnPage: AutoChantPage = .choose
    @State private var pendingVoiceGuide: Bool?
    @State private var paceStep: GuidedStep = .intro
    @State private var voiceStep: GuidedStep = .intro
    @State private var countdown = 3
    @State private var chantStartedAtUptime: TimeInterval?
    @State private var elapsedMillis = 0
    @State private var paceTrials: [Int] = []
    @State private var calibrationMessage: String?
    @State private var microphoneMessage: String?
    @State private var confirmVoiceRecordingDeletion = false
    @State private var notificationPermissionMissing = false

    var body: some View {
        ScrollView {
            VStack(spacing: 8) {
                if model.autoCountActive {
                    sessionPage
                } else {
                    switch page {
                    case .choose:
                        choosePage
                    case .notifications:
                        notificationsPage
                    case .options:
                        optionsPage
                    case .pace:
                        pacePage
                    case .voice:
                        voicePage
                    }

                    backControl
                }
            }
            .padding(.horizontal, 10)
            .padding(.vertical, 8)
            .frame(maxWidth: .infinity)
        }
        .background(Color.black)
        .onAppear {
            refreshNotificationStatus()
        }
        .onDisappear {
            model.cancelVoiceChantSetup()
            model.stopVoiceChantPreview()
        }
        .task(id: chantStartedAtUptime) {
            guard let startedAt = chantStartedAtUptime else { return }
            while !Task.isCancelled && chantStartedAtUptime != nil {
                let currentElapsed = Int(((ProcessInfo.processInfo.systemUptime - startedAt) * 1_000.0).rounded())
                elapsedMillis = max(0, currentElapsed)
                try? await Task.sleep(nanoseconds: 100_000_000)
            }
        }
        .task(id: paceStep) {
            guard paceStep == .countdown else { return }
            for number in stride(from: 3, through: 1, by: -1) {
                countdown = number
                try? await Task.sleep(nanoseconds: 800_000_000)
                if Task.isCancelled { return }
            }
            model.autoChantCalibrationCue()
            elapsedMillis = 0
            chantStartedAtUptime = ProcessInfo.processInfo.systemUptime
            paceStep = .chanting
        }
        .task(id: voiceStep) {
            guard voiceStep == .countdown else { return }
            for number in stride(from: 3, through: 1, by: -1) {
                countdown = number
                try? await Task.sleep(nanoseconds: 800_000_000)
                if Task.isCancelled { return }
            }
            model.autoChantCalibrationCue()
            if model.startVoiceRecording() {
                elapsedMillis = 0
                chantStartedAtUptime = ProcessInfo.processInfo.systemUptime
                voiceStep = .chanting
            } else {
                chantStartedAtUptime = nil
                microphoneMessage = "Recording could not start. Try again."
                voiceStep = .intro
            }
        }
        .onChange(of: model.voiceRecordingFinished) { _, finished in
            guard let finished else { return }
            chantStartedAtUptime = nil
            voiceStep = finished ? .result : .intro
        }
    }

    @ViewBuilder
    private var sessionPage: some View {
        pageTitle("Auto Count", hint: model.autoCountRunning ? "Your chant is counting" : "Paused")
        Text(model.autoCountUsesVoiceGuide ? "Following your chant" : paceDescription(model.state.autoChantPaceMillis))
            .font(.system(size: 10))
            .foregroundStyle(AutoChantColor.muted)
            .multilineTextAlignment(.center)
        actionButton(
            text: model.autoCountRunning ? "Pause" : "Resume",
            color: AutoChantColor.gold,
            contentColor: .black
        ) {
            if model.autoCountRunning {
                model.pauseAutoCount()
            } else {
                model.resumeAutoCount()
            }
        }
        actionButton(
            text: "Stop counting",
            color: AutoChantColor.alertRed,
            contentColor: .white
        ) {
            model.stopAutoCount()
            onBack()
        }
    }

    @ViewBuilder
    private var choosePage: some View {
        pageTitle("Auto Count")
        let hasPace = model.state.autoChantPaceMillis != nil
        let hasGuide = model.hasVoiceChantRecording
        if hasPace || hasGuide {
            if hasPace {
                Text("Pace · \(paceDescription(model.state.autoChantPaceMillis))")
                    .font(.system(size: 10))
                    .foregroundStyle(AutoChantColor.muted)
                    .multilineTextAlignment(.center)
                actionButton(text: "Start with my pace") {
                    requestCountStart(useVoiceGuide: false)
                }
            }
            if hasGuide {
                actionButton(text: "Count with my chant") {
                    requestCountStart(useVoiceGuide: true)
                }
            }
            actionButton(text: "Change setup") {
                page = .options
            }
        } else {
            actionButton(text: "Set my pace", action: openPaceSetup)
            actionButton(text: "Record a chant", action: openVoiceSetup)
        }
    }

    @ViewBuilder
    private var notificationsPage: some View {
        pageTitle("Notifications", compact: true)
        Text("Return shortcut for Auto Count.")
            .font(.system(size: 10))
            .foregroundStyle(AutoChantColor.muted)
            .multilineTextAlignment(.center)
        actionButton(text: "Allow notifications") {
            UNUserNotificationCenter.current().requestAuthorization(options: [.alert]) { granted, _ in
                Task { @MainActor in
                    notificationPermissionMissing = !granted
                    if !granted {
                        model.notificationPermissionDeclined = true
                    }
                    if let useVoice = pendingVoiceGuide {
                        pendingVoiceGuide = nil
                        startCount(useVoiceGuide: useVoice)
                    } else {
                        page = notificationReturnPage
                    }
                }
            }
        }
    }

    @ViewBuilder
    private var optionsPage: some View {
        if confirmVoiceRecordingDeletion {
            pageTitle("Delete saved chant?", hint: "Are you sure?")
            HStack(spacing: 12) {
                Button {
                    confirmVoiceRecordingDeletion = false
                } label: {
                    Image(systemName: "xmark")
                        .font(.system(size: 16, weight: .bold))
                        .foregroundStyle(.white)
                        .frame(width: 48, height: 48)
                        .background(AutoChantColor.secondary, in: Circle())
                }
                .buttonStyle(.plain)
                .accessibilityLabel("Cancel chant deletion")

                Button {
                    model.deleteVoiceChantRecording()
                    confirmVoiceRecordingDeletion = false
                } label: {
                    Image(systemName: "checkmark")
                        .font(.system(size: 16, weight: .bold))
                        .foregroundStyle(.white)
                        .frame(width: 48, height: 48)
                        .background(AutoChantColor.alertRed, in: Circle())
                }
                .buttonStyle(.plain)
                .accessibilityLabel("Confirm chant deletion")
            }
        } else {
            pageTitle("Auto Count", hint: "Choose or change a guide")
            actionButton(
                text: model.state.autoChantPaceMillis == nil ? "Set my pace" : "Change my pace",
                action: openPaceSetup
            )
            actionButton(
                text: model.hasVoiceChantRecording ? "Record a new chant" : "Record a chant",
                action: openVoiceSetup
            )
            if model.hasVoiceChantRecording {
                actionButton(
                    text: "Delete saved chant",
                    color: AutoChantColor.alertRed
                ) {
                    confirmVoiceRecordingDeletion = true
                }
            }
            if notificationPermissionMissing {
                actionButton(text: "Notifications") {
                    pendingVoiceGuide = nil
                    notificationReturnPage = page
                    page = .notifications
                }
            }
        }
    }

    @ViewBuilder
    private var pacePage: some View {
        switch paceStep {
        case .intro:
            pageTitle(
                paceTrials.isEmpty
                    ? "Set your pace"
                    : "Chant \(paceTrials.count + 1) of \(AutoChantTiming.calibrationTrials)",
                hint: "Tap timer. Chant on cue.",
                compact: true
            )
            Text("Timing only · no audio.")
                .font(.system(size: 10))
                .foregroundStyle(AutoChantColor.muted)
                .multilineTextAlignment(.center)
            iconActionButton(
                systemImage: "timer",
                description: "Start pace timing; no audio is recorded"
            ) {
                calibrationMessage = nil
                paceStep = .countdown
            }
            if let calibrationMessage {
                Text(calibrationMessage)
                    .font(.system(size: 10))
                    .foregroundStyle(AutoChantColor.muted)
                    .multilineTextAlignment(.center)
            }
        case .countdown:
            countdownPage
        case .chanting:
            pageTitle("Chant now", hint: "Tap stop to finish.")
            Text(formatDuration(elapsedMillis))
                .font(.system(size: 26, weight: .bold, design: .rounded).monospacedDigit())
                .foregroundStyle(AutoChantColor.gold)
            iconActionButton(
                systemImage: "stop.fill",
                description: "Stop pace timing"
            ) {
                guard let startedAt = chantStartedAtUptime else { return }
                let duration = Int(((ProcessInfo.processInfo.systemUptime - startedAt) * 1_000.0).rounded())
                chantStartedAtUptime = nil
                if !AutoChantTiming.isValidPace(duration) {
                    calibrationMessage = "Try again. Chant once at your usual pace."
                    paceStep = .intro
                } else {
                    calibrationMessage = nil
                    paceTrials.append(duration)
                    if paceTrials.count == AutoChantTiming.calibrationTrials {
                        if let middlePace = AutoChantTiming.medianPaceMillis(paceTrials) {
                            model.saveAutoChantPace(middlePace)
                        }
                        paceStep = .result
                    } else {
                        paceStep = .takeSaved
                    }
                }
            }
        case .takeSaved:
            pageTitle("Chant saved", hint: "\(paceTrials.count) of \(AutoChantTiming.calibrationTrials)")
            actionButton(
                text: "Next chant",
                color: AutoChantColor.gold,
                contentColor: .black
            ) {
                paceStep = .countdown
            }
        case .result:
            pageTitle("Pace ready")
            Text(paceDescription(model.state.autoChantPaceMillis))
                .font(.system(size: 14))
                .foregroundStyle(.white)
            actionButton(
                text: "Start counting",
                color: AutoChantColor.gold,
                contentColor: .black
            ) {
                requestCountStart(useVoiceGuide: false)
            }
            actionButton(text: "Try again") {
                paceTrials.removeAll()
                calibrationMessage = nil
                paceStep = .intro
            }
        }
    }

    @ViewBuilder
    private var voicePage: some View {
        switch voiceStep {
        case .intro:
            pageTitle("Record a chant", hint: "Save a guide for Auto Count.")
            Text("Records one chant, up to 20 seconds, as an Auto Count guide. It stays in this watch's private app storage until deleted. Gaura Mala doesn't upload or share it.")
                .font(.system(size: 10))
                .foregroundStyle(AutoChantColor.muted)
                .multilineTextAlignment(.center)
            Text("Only record a voice you have permission to record and use.")
                .font(.system(size: 10))
                .foregroundStyle(AutoChantColor.muted)
                .multilineTextAlignment(.center)
            Text("For a child, a parent or guardian should decide and help with recording and deletion.")
                .font(.system(size: 10))
                .foregroundStyle(AutoChantColor.muted)
                .multilineTextAlignment(.center)
            iconActionButton(
                systemImage: "mic.fill",
                description: "Agree and begin recording a chant",
                action: beginVoiceSetup
            )
            Text("Agree & begin")
                .font(.system(size: 10))
                .foregroundStyle(.white)
            if let message = model.voiceChantMessage {
                Text(message)
                    .font(.system(size: 10))
                    .foregroundStyle(AutoChantColor.muted)
                    .multilineTextAlignment(.center)
            }
            if let microphoneMessage {
                Text(microphoneMessage)
                    .font(.system(size: 10))
                    .foregroundStyle(AutoChantColor.alertRed)
                    .multilineTextAlignment(.center)
            }
        case .countdown:
            countdownPage
        case .chanting:
            pageTitle("Chant now", hint: "Tap stop to finish.")
            Text("Recording")
                .font(.system(size: 12))
                .foregroundStyle(AutoChantColor.alertRed)
            Text(formatDuration(elapsedMillis))
                .font(.system(size: 24, weight: .bold, design: .rounded).monospacedDigit())
                .foregroundStyle(AutoChantColor.gold)
            iconActionButton(
                systemImage: "stop.fill",
                description: "Stop chant recording"
            ) {
                chantStartedAtUptime = nil
                model.finishVoiceRecording()
            }
        case .takeSaved:
            EmptyView()
        case .result:
            pageTitle("Chant ready")
            Text(paceDescription(model.state.autoChantPaceMillis))
                .font(.system(size: 13))
                .foregroundStyle(.white)
            WatchVolumeControl()
                .frame(height: 36)
            HStack(spacing: 16) {
                voiceGuideIconButton(
                    systemImage: model.isVoiceChantPreviewing ? "stop.fill" : "play.fill",
                    description: model.isVoiceChantPreviewing ? "Stop chant preview" : "Play saved chant"
                ) {
                    if model.isVoiceChantPreviewing {
                        model.stopVoiceChantPreview()
                    } else {
                        model.previewVoiceChant()
                    }
                }
                voiceGuideIconButton(
                    systemImage: "mic.fill",
                    description: "Record another chant"
                ) {
                    model.stopVoiceChantPreview()
                    voiceStep = .countdown
                }
            }
            actionButton(
                text: "Count with this chant",
                color: AutoChantColor.gold,
                contentColor: .black
            ) {
                requestCountStart(useVoiceGuide: true)
            }
        }
    }

    private var countdownPage: some View {
        VStack(spacing: 6) {
            pageTitle("Get ready", hint: "Chant when you feel the cue.")
            Text("\(countdown)")
                .font(.system(size: 48, weight: .bold, design: .rounded))
                .foregroundStyle(AutoChantColor.gold)
        }
    }

    private var backControl: some View {
        let isOptOutPrompt = (page == .voice && voiceStep == .intro) || page == .notifications
        return VStack(spacing: 2) {
            Button(action: goBack) {
                Image(systemName: "arrow.left")
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundStyle(AutoChantColor.gold)
                    .frame(width: 48, height: 48)
                    .background(AutoChantColor.secondary, in: Circle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel(isOptOutPrompt ? "Not now" : "Back")

            if isOptOutPrompt {
                Text("Not now")
                    .font(.system(size: 9))
                    .foregroundStyle(.white)
            }
        }
    }

    @ViewBuilder
    private func pageTitle(_ title: String, hint: String? = nil, compact: Bool = false) -> some View {
        Text(title)
            .font(.system(size: compact ? 15 : 16, weight: .bold, design: .rounded))
            .foregroundStyle(AutoChantColor.gold)
            .multilineTextAlignment(.center)
        if let hint, !hint.isEmpty {
            Text(hint)
                .font(.system(size: 10))
                .foregroundStyle(AutoChantColor.muted)
                .multilineTextAlignment(.center)
        }
    }

    private func actionButton(
        text: String,
        color: Color = AutoChantColor.secondary,
        contentColor: Color = .white,
        action: @escaping () -> Void
    ) -> some View {
        Button(action: action) {
            Text(text)
                .font(.system(size: 11, weight: .medium, design: .rounded))
                .foregroundStyle(contentColor)
                .multilineTextAlignment(.center)
                .frame(maxWidth: .infinity, minHeight: 48)
                .padding(.horizontal, 10)
                .background(color, in: Capsule())
        }
        .buttonStyle(.plain)
    }

    private func iconActionButton(
        systemImage: String,
        description: String,
        action: @escaping () -> Void
    ) -> some View {
        Button(action: action) {
            Image(systemName: systemImage)
                .font(.system(size: 22, weight: .semibold))
                .foregroundStyle(AutoChantColor.gold)
                .frame(width: 56, height: 56)
                .background(AutoChantColor.secondary, in: Circle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(description)
    }

    private func voiceGuideIconButton(
        systemImage: String,
        description: String,
        action: @escaping () -> Void
    ) -> some View {
        Button(action: action) {
            Image(systemName: systemImage)
                .font(.system(size: 18, weight: .semibold))
                .foregroundStyle(AutoChantColor.gold)
                .frame(width: 48, height: 48)
                .background(AutoChantColor.secondary, in: Circle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(description)
    }

    private func openPaceSetup() {
        returnPage = page
        paceTrials.removeAll()
        calibrationMessage = nil
        paceStep = .intro
        page = .pace
    }

    private func openVoiceSetup() {
        returnPage = page
        model.prepareVoiceChantSetup()
        voiceStep = .intro
        page = .voice
    }

    private func beginVoiceSetup() {
        microphoneMessage = nil
        model.requestMicrophonePermission { granted in
            if granted {
                microphoneMessage = nil
                voiceStep = .countdown
            } else {
                microphoneMessage = "Microphone access is off. Nothing was recorded."
            }
        }
    }

    private func startCount(useVoiceGuide: Bool) {
        model.startAutoCount(useVoiceGuide: useVoiceGuide)
        if model.autoCountRunning {
            onStartCount()
        }
    }

    private func requestCountStart(useVoiceGuide: Bool) {
        if notificationPermissionMissing && !model.notificationPermissionDeclined {
            pendingVoiceGuide = useVoiceGuide
            notificationReturnPage = page
            page = .notifications
        } else {
            startCount(useVoiceGuide: useVoiceGuide)
        }
    }

    private func declineNotificationPermission() {
        model.notificationPermissionDeclined = true
        if let useVoice = pendingVoiceGuide {
            pendingVoiceGuide = nil
            startCount(useVoiceGuide: useVoice)
        } else {
            page = notificationReturnPage
        }
    }

    private func goBack() {
        if confirmVoiceRecordingDeletion {
            confirmVoiceRecordingDeletion = false
            return
        }
        switch page {
        case .choose:
            onBack()
        case .options:
            page = .choose
        case .pace:
            chantStartedAtUptime = nil
            paceTrials.removeAll()
            calibrationMessage = nil
            paceStep = .intro
            page = returnPage
            returnPage = .choose
        case .voice:
            chantStartedAtUptime = nil
            model.cancelVoiceChantSetup()
            voiceStep = .intro
            page = returnPage
            returnPage = .choose
        case .notifications:
            declineNotificationPermission()
        }
    }

    private func refreshNotificationStatus() {
        UNUserNotificationCenter.current().getNotificationSettings { settings in
            let missing = settings.authorizationStatus == .notDetermined
            Task { @MainActor in
                notificationPermissionMissing = missing
            }
        }
    }

    private func paceDescription(_ paceMillis: Int?) -> String {
        guard let paceMillis else { return "No pace saved" }
        return String(format: "%.1f sec per chant", Double(paceMillis) / 1_000.0)
    }

    private func formatDuration(_ durationMillis: Int) -> String {
        String(format: "%.1f sec", Double(durationMillis) / 1_000.0)
    }
}

private struct WatchVolumeControl: WKInterfaceObjectRepresentable {
    func makeWKInterfaceObject(context: Context) -> WKInterfaceVolumeControl {
        let control = WKInterfaceVolumeControl(origin: .local)
        control.setTintColor(UIColor(red: 1.0, green: 0.69, blue: 0.12, alpha: 1.0))
        return control
    }

    func updateWKInterfaceObject(_ wkInterfaceObject: WKInterfaceVolumeControl, context: Context) {}
}

