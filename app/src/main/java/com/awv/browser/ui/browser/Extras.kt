package com.awv.browser.ui.browser

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.awv.browser.browser.BrowserController
import com.awv.browser.browser.UrlUtils
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

/** Start-page extras: voice search and code scanning. */
class ExtraActions(val voice: () -> Unit, val scan: () -> Unit)

/**
 * Voice: the system speech recognizer (it asks for the microphone itself, so Awv needs no permission);
 * the spoken text goes through the normal address-or-search logic.
 * Scan: Google's code scanner (QR + barcodes, no camera permission needed). A scanned web address is handed to
 * [onScannedLink] so the user can confirm before it opens; any other text is searched.
 */
@Composable
fun rememberExtraActions(c: BrowserController, onScannedLink: (String) -> Unit): ExtraActions {
    val context = LocalContext.current
    val currentOnLink by rememberUpdatedState(onScannedLink)

    val voiceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                ?.trim()
            if (!spoken.isNullOrEmpty()) c.load(c.active, spoken)
        }
    }

    return remember(c, context, voiceLauncher) {
        ExtraActions(
            voice = {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH)
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to search")
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                }
                try {
                    voiceLauncher.launch(intent)
                } catch (e: ActivityNotFoundException) {
                    c.toast("Voice search isn't available on this device")
                }
            },
            scan = {
                val options = GmsBarcodeScannerOptions.Builder().build()
                GmsBarcodeScanning.getClient(context, options).startScan()
                    .addOnSuccessListener { barcode ->
                        val text = (barcode.rawValue ?: barcode.displayValue)?.trim().orEmpty()
                        val address = UrlUtils.asAddress(text)
                        when {
                            text.isEmpty() -> c.toast("Couldn't read this code")
                            address != null -> currentOnLink(address)
                            else -> c.load(c.active, text)
                        }
                    }
                    .addOnFailureListener { c.toast("Code scanner isn't available right now") }
            }
        )
    }
}
