package app.cashflow.android

import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import android.graphics.Color
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import app.cashflow.ui.components.LocalAppIcons
import androidx.compose.runtime.getValue
import app.cashflow.ui.CashflowApp

class MainActivity : ComponentActivity() {
    private val iconCache = java.util.concurrent.ConcurrentHashMap<String, ImageBitmap>()
    private val appIcons: (String) -> ImageBitmap? = { id ->
        iconCache[id] ?: runCatching {
            val d = packageManager.getApplicationIcon(id)
            val size = 144
            val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            d.setBounds(0, 0, size, size)
            d.draw(Canvas(bmp))
            bmp.asImageBitmap().also { iconCache[id] = it }
        }.getOrNull()
    }
    private val app get() = application as CashflowApplication
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        app.activity = this
        intent.getStringExtra("entryId")?.let(app.store::focusEntry)
        setContent {
            val state by app.store.state.collectAsState()
            CompositionLocalProvider(LocalAppIcons provides appIcons) { CashflowApp(state, app.store) }
        }
    }
    override fun onResume() { super.onResume(); app.activity = this; app.store.refreshCapture() }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); intent.getStringExtra("entryId")?.let(app.store::focusEntry) }
    override fun onDestroy() { if (app.activity === this) app.activity = null; super.onDestroy() }
}
