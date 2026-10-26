package app.cashflow

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.window.ComposeUIViewController
import app.cashflow.core.LedgerStore
import app.cashflow.ui.CashflowApp

fun MainViewController(store: LedgerStore) = ComposeUIViewController {
    val state by store.state.collectAsState()
    CashflowApp(state, store)
}
