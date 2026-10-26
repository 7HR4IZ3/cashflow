package app.cashflow.android

import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import app.cashflow.ui.CashflowApp

class MainActivity : ComponentActivity() {
    private val app get() = application as CashflowApplication
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        app.activity = this
        intent.getStringExtra("entryId")?.let(app.store::focusEntry)
        setContent {
            val state by app.store.state.collectAsState()
            CashflowApp(state, app.store)
        }
    }
    override fun onResume() { super.onResume(); app.activity = this; app.store.refreshCapture() }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); intent.getStringExtra("entryId")?.let(app.store::focusEntry) }
    override fun onDestroy() { if (app.activity === this) app.activity = null; super.onDestroy() }
}
