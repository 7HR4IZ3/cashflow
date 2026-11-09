import SwiftUI
import CashflowKit
import UserNotifications
import UIKit

@MainActor
final class Runtime {
    static let shared = Runtime()
    let hooks = IosHooks()
    lazy var store: LedgerStore = {
        let store = IosStoreKt.createIosStore(hooks: hooks)
        #if DEBUG
        // Development aid: launch with `-replaySetup <step>` to see an intro page again. Data is untouched.
        // Launch arguments reach UserDefaults as strings.
        if let raw = UserDefaults.standard.string(forKey: "replaySetup"), let step = Int(raw) { store.replayOnboarding(step: Int32(step)) }
        // Development aid: `-pipStep <1-3>` floats the setup card shortly after launch.
        if let raw = UserDefaults.standard.string(forKey: "pipStep"), let step = Int(raw) {
            DispatchQueue.main.asyncAfter(deadline: .now() + 2) { SetupGuidePiP.shared.start(step: step) }
        }
        #endif
        return store
    }()
}

final class IosHooks: NSObject, PlatformHooks, UNUserNotificationCenterDelegate {
    override init() {
        super.init()
        let center = UNUserNotificationCenter.current()
        center.delegate = self
        let undo = UNNotificationAction(identifier: "undo", title: "Undo")
        let edit = UNNotificationAction(identifier: "edit", title: "Edit", options: [.foreground])
        center.setNotificationCategories([UNNotificationCategory(identifier: "recorded", actions: [undo, edit], intentIdentifiers: [])])
    }
    func now() -> String {
        let formatter = ISO8601DateFormatter()
        formatter.timeZone = .current
        return formatter.string(from: Date())
    }
    func id() -> String { UUID().uuidString }
    func nowMillis() -> Int64 { Int64(Date().timeIntervalSince1970 * 1000) }
    // Subscriptions and AI help are not available on iOS yet.
    func refreshSubscription() { }
    func purchase(offerId: String) { }
    func manageSubscription() { }
    func analyseAlert(item: ReviewItem, automatic: Bool) { }
    func captureState(selected: Set<String>) -> CaptureState {
        CaptureState(platform: "iOS", accessGranted: false, lastReceived: nil, sources: [])
    }
    func installShortcut() {
        // The shortcut file carries the notification trigger and the Cashflow action. The share sheet offers Shortcuts.
        guard let url = Bundle.main.url(forResource: "Capture transaction notification", withExtension: "shortcut") else { return }
        SetupGuidePiP.shared.start(step: 1)
        DispatchQueue.main.async {
            guard let scene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
                  let root = scene.windows.first(where: { $0.isKeyWindow })?.rootViewController else { return }
            var presenter = root
            while let next = presenter.presentedViewController { presenter = next }
            let share = UIActivityViewController(activityItems: [url], applicationActivities: nil)
            share.popoverPresentationController?.sourceView = presenter.view
            presenter.present(share, animated: true)
        }
    }
    func showSetupGuide(step: Int32) { SetupGuidePiP.shared.start(step: Int(step)) }
    func setupGuideVisible(visible: Bool) { SetupGuidePiP.shared.setAllowed(visible) }
    func openCaptureSettings() {
        // Land on the shortcut itself, where the bank names are. Fall back to the Shortcuts library.
        SetupGuidePiP.shared.whenReady {
            guard let deep = URL(string: "shortcuts://open-shortcut?name=Capture%20transaction%20notification") else { return }
            UIApplication.shared.open(deep) { ok in
                if !ok, let library = URL(string: "shortcuts://") { UIApplication.shared.open(library) }
            }
        }
    }
    func requestNotificationPermission() {
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound, .badge]) { _, _ in }
    }
    func exportCsv(contents: String) -> String? {
        do {
            let url = FileManager.default.temporaryDirectory.appendingPathComponent("cashflow.csv")
            try contents.write(to: url, atomically: true, encoding: .utf8)
            guard let scene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
                  let root = scene.windows.first(where: { $0.isKeyWindow })?.rootViewController else { return "Open Cashflow to export." }
            var presenter = root
            while let next = presenter.presentedViewController { presenter = next }
            let share = UIActivityViewController(activityItems: [url], applicationActivities: nil)
            share.popoverPresentationController?.sourceView = presenter.view
            presenter.present(share, animated: true)
            return nil
        } catch { return "Could not export the ledger." }
    }
    func recorded(entry: Entry) {
        let content = UNMutableNotificationContent()
        content.title = "Transaction recorded"
        content.body = "Open Cashflow to see the entry."
        content.categoryIdentifier = "recorded"
        content.userInfo = ["id": entry.id, "revision": entry.revision]
        UNUserNotificationCenter.current().add(UNNotificationRequest(identifier: entry.id, content: content, trigger: nil))
    }
    func userNotificationCenter(_ center: UNUserNotificationCenter, didReceive response: UNNotificationResponse) async {
        if response.actionIdentifier == "undo",
           let id = response.notification.request.content.userInfo["id"] as? String,
           let revision = response.notification.request.content.userInfo["revision"] as? NSNumber {
            await MainActor.run {
                _ = IosStoreKt.undoNotification(store: Runtime.shared.store, id: id, revision: revision.int64Value)
            }
        } else if let id = response.notification.request.content.userInfo["id"] as? String {
            await MainActor.run { Runtime.shared.store.focusEntry(id: id) }
        }
    }
}

struct CashflowRoot: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController(store: Runtime.shared.store)
    }
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) { }
}

@main
struct CashflowApplication: App {
    var body: some Scene { WindowGroup { CashflowRoot().ignoresSafeArea() } }
}
