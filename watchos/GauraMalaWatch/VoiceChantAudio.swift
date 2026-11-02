import AVFoundation
import Foundation

/// Records and plays one short chant entirely inside the app's non-backed-up private directory.
final class VoiceChantAudio: NSObject, AVAudioRecorderDelegate, AVAudioPlayerDelegate {
    private let files: VoiceChantFiles
    private var recorder: AVAudioRecorder?
    private var player: AVAudioPlayer?
    private var onRecordingLimitReached: (() -> Void)?
    private var onPlaybackCompleted: (() -> Bool)?

    override init() {
        let baseDirectory = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask).first
            ?? FileManager.default.temporaryDirectory
        let noBackupDirectory = baseDirectory.appendingPathComponent("NoBackup", isDirectory: true)
        try? FileManager.default.createDirectory(at: noBackupDirectory, withIntermediateDirectories: true)
        var mutableDirectory = noBackupDirectory
        var resourceValues = URLResourceValues()
        resourceValues.isExcludedFromBackup = true
        try? mutableDirectory.setResourceValues(resourceValues)

        self.files = VoiceChantFiles(directory: noBackupDirectory)
        super.init()
        self.files.clearPendingTakes()
    }

    func hasRecording() -> Bool {
        files.hasRecording()
    }

    func recordingDurationMillis() -> Int? {
        guard files.hasRecording() else { return nil }
        return durationMillis(of: files.recordingURL)
    }

    func temporaryRecordingDurationMillis() -> Int? {
        let tempURL = files.temporaryRecordingURL()
        guard FileManager.default.fileExists(atPath: tempURL.path) else { return nil }
        return durationMillis(of: tempURL)
    }

    private func durationMillis(of url: URL) -> Int? {
        guard let probePlayer = try? AVAudioPlayer(contentsOf: url) else { return nil }
        let millis = Int((probePlayer.duration * 1_000.0).rounded())
        return millis > 0 ? millis : nil
    }

    func requestMicrophonePermission(_ completion: @escaping @Sendable (Bool) -> Void) {
        AVAudioApplication.requestRecordPermission { granted in
            completion(granted)
        }
    }

    func startRecording(onLimitReached: @escaping () -> Void) -> Bool {
        guard recorder == nil else { return false }
        let tempURL = files.temporaryRecordingURL()
        try? FileManager.default.createDirectory(at: files.directory, withIntermediateDirectories: true)
        try? FileManager.default.removeItem(at: tempURL)

        let settings: [String: Any] = [
            AVFormatIDKey: Int(kAudioFormatMPEG4AAC),
            AVSampleRateKey: 22_050,
            AVNumberOfChannelsKey: 1,
            AVEncoderBitRateKey: 64_000,
            AVEncoderAudioQualityKey: AVAudioQuality.medium.rawValue
        ]

        do {
            let session = AVAudioSession.sharedInstance()
            try session.setCategory(.playAndRecord, mode: .default)
            try session.setActive(true)

            let activeRecorder = try AVAudioRecorder(url: tempURL, settings: settings)
            activeRecorder.delegate = self
            guard activeRecorder.prepareToRecord(),
                  activeRecorder.record(forDuration: Self.maxRecordingSeconds) else {
                try? FileManager.default.removeItem(at: tempURL)
                return false
            }
            recorder = activeRecorder
            onRecordingLimitReached = onLimitReached
            return true
        } catch {
            recorder = nil
            onRecordingLimitReached = nil
            try? FileManager.default.removeItem(at: tempURL)
            return false
        }
    }

    func finishRecording() -> Bool {
        guard let activeRecorder = recorder else { return false }
        recorder = nil
        onRecordingLimitReached = nil
        activeRecorder.stop()
        try? AVAudioSession.sharedInstance().setActive(false, options: .notifyOthersOnDeactivation)
        guard FileManager.default.fileExists(atPath: files.temporaryRecordingURL().path) else {
            files.discardTemporaryRecording()
            return false
        }
        return true
    }

    func publishTemporaryRecording() -> Bool {
        files.publishTemporaryRecording()
    }

    func discardTemporaryRecording() {
        files.discardTemporaryRecording()
    }

    func cancelRecording() {
        if let activeRecorder = recorder {
            recorder = nil
            onRecordingLimitReached = nil
            activeRecorder.stop()
            try? AVAudioSession.sharedInstance().setActive(false, options: .notifyOthersOnDeactivation)
        }
        files.discardTemporaryRecording()
    }

    func deleteRecording() {
        cancelRecording()
        stopPlayback()
        files.deleteAll()
    }

    func preview(onFinished: @escaping () -> Void) -> Bool {
        play {
            onFinished()
            return false
        }
    }

    func playLoop(onChantCompleted: @escaping () -> Bool) -> Bool {
        play(onCompletion: onChantCompleted)
    }

    func stopPlayback() {
        guard let activePlayer = player else { return }
        player = nil
        onPlaybackCompleted = nil
        if activePlayer.isPlaying {
            activePlayer.stop()
        }
        try? AVAudioSession.sharedInstance().setActive(false, options: .notifyOthersOnDeactivation)
    }

    private func play(onCompletion: @escaping () -> Bool) -> Bool {
        guard files.hasRecording() else { return false }
        stopPlayback()
        do {
            let session = AVAudioSession.sharedInstance()
            try session.setCategory(.playback, mode: .spokenAudio)
            try session.setActive(true)

            let activePlayer = try AVAudioPlayer(contentsOf: files.recordingURL)
            activePlayer.delegate = self
            guard activePlayer.prepareToPlay(), activePlayer.play() else {
                return false
            }
            player = activePlayer
            onPlaybackCompleted = onCompletion
            return true
        } catch {
            player = nil
            onPlaybackCompleted = nil
            return false
        }
    }

    func audioRecorderDidFinishRecording(_ recorder: AVAudioRecorder, successfully flag: Bool) {
        guard self.recorder === recorder else { return }
        let callback = onRecordingLimitReached
        callback?()
    }

    func audioPlayerDidFinishPlaying(_ player: AVAudioPlayer, successfully flag: Bool) {
        guard self.player === player else { return }
        guard flag, let onPlaybackCompleted, onPlaybackCompleted() else {
            stopPlayback()
            return
        }
        player.currentTime = 0
        if !player.play() {
            stopPlayback()
        }
    }

    private static let maxRecordingSeconds: TimeInterval = 20.0
}

