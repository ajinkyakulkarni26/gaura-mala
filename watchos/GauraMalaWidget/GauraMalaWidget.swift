import SwiftUI
import WidgetKit

private struct GauraMalaEntry: TimelineEntry {
    let date: Date
    let beadCount: Int
    let completedRounds: Int
    let dailyGoalRounds: Int

    static let empty = GauraMalaEntry(date: .now, beadCount: 0, completedRounds: 0, dailyGoalRounds: 16)

    var isGoalAchieved: Bool { completedRounds >= dailyGoalRounds }
    var roundLabel: String { isGoalAchieved ? "Goal" : "R\(completedRounds + 1)" }
}

private struct GauraMalaProvider: TimelineProvider {
    func placeholder(in context: Context) -> GauraMalaEntry { .empty }

    func getSnapshot(in context: Context, completion: @escaping (GauraMalaEntry) -> Void) {
        completion(readEntry())
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<GauraMalaEntry>) -> Void) {
        let entry = readEntry()
        let nextDay = Calendar.current.date(byAdding: .day, value: 1, to: Calendar.current.startOfDay(for: .now))
            ?? .now.addingTimeInterval(3_600)
        completion(Timeline(entries: [entry], policy: .after(nextDay)))
    }

    private func readEntry() -> GauraMalaEntry {
        let defaults = UserDefaults(suiteName: "group.com.gauramala.watchos")
        guard let data = defaults?.data(forKey: "gaura-mala.watch.counter-state.v1"),
              let state = try? JSONDecoder().decode(CounterState.self, from: data) else {
            return .empty
        }
        let isCurrentLocalDay = state.lastRecordedDate == Self.localDateString()
        return GauraMalaEntry(
            date: .now,
            beadCount: isCurrentLocalDay ? min(max(state.beadCount, 0), 107) : 0,
            completedRounds: isCurrentLocalDay ? min(max(state.completedRounds, 0), 999) : 0,
            dailyGoalRounds: min(max(state.dailyGoalRounds, 1), 64)
        )
    }

    private static func localDateString(_ date: Date = .now) -> String {
        let components = Calendar.current.dateComponents([.year, .month, .day], from: date)
        return String(format: "%04d-%02d-%02d", components.year ?? 0, components.month ?? 0, components.day ?? 0)
    }
}

struct GauraMalaWidget: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: "GauraMalaComplication", provider: GauraMalaProvider()) { entry in
            GauraMalaWidgetView(entry: entry)
        }
        .configurationDisplayName("Gaura Mala")
        .description("See today's rounds and mala progress.")
        .supportedFamilies([.accessoryCircular, .accessoryCorner, .accessoryInline, .accessoryRectangular])
    }
}

private struct GauraMalaWidgetView: View {
    let entry: GauraMalaEntry
    @Environment(\.widgetFamily) private var family

    private var progress: Double { Double(entry.beadCount) / 108.0 }

    var body: some View {
        Group {
            switch family {
            case .accessoryInline:
                Label("\(entry.roundLabel) · \(entry.beadCount)/108", systemImage: "circle.grid.3x3.fill")
            case .accessoryCorner:
                VStack(alignment: .leading, spacing: 2) {
                    Text(entry.roundLabel)
                        .font(.system(.caption2, design: .rounded).weight(.semibold))
                    Gauge(value: progress) { Text("Mala") }
                        .gaugeStyle(.accessoryLinearCapacity)
                }
                .widgetLabel(entry.isGoalAchieved ? "Goal reached · \(entry.beadCount)/108" : "\(entry.completedRounds) rounds · \(entry.beadCount)/108")
            case .accessoryRectangular:
                VStack(alignment: .leading, spacing: 3) {
                    Text(entry.isGoalAchieved ? "Daily goal reached" : "Round \(entry.completedRounds + 1) of \(entry.dailyGoalRounds)")
                        .font(.system(.caption, design: .rounded).weight(.semibold))
                    ProgressView(value: progress)
                        .tint(Color(red: 1.0, green: 0.69, blue: 0.12))
                    Text("\(entry.beadCount) / 108 beads")
                        .font(.system(.caption2, design: .rounded))
                        .foregroundStyle(.secondary)
                }
            default:
                ZStack {
                    AccessoryWidgetBackground()
                    Gauge(value: progress) { Text(entry.roundLabel) }
                        .gaugeStyle(.accessoryCircularCapacity)
                    VStack(spacing: 0) {
                        Text(entry.roundLabel)
                            .font(.system(.caption2, design: .rounded).weight(.semibold))
                        Text("\(entry.beadCount)")
                            .font(.system(.caption2, design: .rounded).monospacedDigit())
                    }
                }
            }
        }
        .widgetURL(URL(string: "gauramala://counter"))
    }
}

@main
struct GauraMalaWidgetBundle: WidgetBundle {
    var body: some Widget {
        GauraMalaWidget()
    }
}
