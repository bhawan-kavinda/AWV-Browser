package com.awv.browser

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.awv.browser.browser.BrowserController
import com.awv.browser.ui.browser.BrowserApp
import com.awv.browser.ui.theme.AwvTheme

class MainActivity : ComponentActivity() {

    private lateinit var browser: BrowserController

    override fun onCreate(savedInstanceState: Bundle?) {
        AwvTheme.init(this) // saved light/dark choice, otherwise the device default
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)
        browser = BrowserController(this)
        setContent { BrowserApp(browser) }
    }

    override fun onResume() {
        super.onResume()
        browser.onResume()
    }

    override fun onPause() {
        browser.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        browser.destroy()
        super.onDestroy()
    }
}
