package app.cashflow.core

import app.cash.sqldelight.driver.native.NativeSqliteDriver
import app.cashflow.db.LedgerDatabase

fun createIosStore(hooks: PlatformHooks): LedgerStore = LedgerStore(NativeSqliteDriver(LedgerDatabase.Schema, "cashflow.db"), hooks)

fun receiveShortcut(store: LedgerStore, text: String, source: String, deliveryKey: String): String? {
    if (!store.state.value.onboardingComplete) return "Open Cashflow and complete or skip setup first."
    if (store.state.value.capturePaused) return "Capture is paused in Cashflow."
    return store.ingest(text, source, deliveryKey)
}

fun undoNotification(store: LedgerStore, id: String, revision: Long): Boolean {
    val entry = store.state.value.entries.firstOrNull { it.id == id } ?: return false
    if (entry.revision != revision || entry.deleted) return false
    return store.delete(id) == null
}
