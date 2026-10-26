import AppIntents
import CashflowKit
import Foundation

struct CaptureTransactionIntent: AppIntent {
    static let title: LocalizedStringResource = "Capture transaction notification"
    static let description = IntentDescription("Pass selected payment alert text to Cashflow. Clear debit and credit alerts are recorded; uncertain details go to Review.")
    static let openAppWhenRun = false

    @Parameter(title: "Notification text") var text: String
    @Parameter(title: "Source label", default: "Shortcuts") var source: String
    @Parameter(title: "Event identifier") var eventIdentifier: String?

    @MainActor
    func perform() async throws -> some IntentResult {
        let key = eventIdentifier?.isEmpty == false ? "shortcut:\(source):\(eventIdentifier!)" : "shortcut:\(UUID().uuidString)"
        if let error = IosStoreKt.receiveShortcut(store: Runtime.shared.store, text: text, source: source, deliveryKey: key) {
            throw CaptureError.failed(error)
        }
        return .result()
    }
}

enum CaptureError: Error, LocalizedError {
    case failed(String)
    var errorDescription: String? { if case .failed(let message) = self { return message }; return nil }
}

struct CashflowShortcuts: AppShortcutsProvider {
    static var appShortcuts: [AppShortcut] {
        AppShortcut(intent: CaptureTransactionIntent(), phrases: ["Capture a payment in \(.applicationName)"], shortTitle: "Capture payment", systemImageName: "tray.and.arrow.down")
    }
}
