package org.jayhsu.xsaver.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import androidx.core.net.toUri

class ShareReceiverActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val link = extractUrlFromIntent(intent)
        val forward = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            if (link != null) putExtra(EXTRA_SHARED_LINK, link)
        }
        startActivity(forward)
        finish()
    }

    private fun extractUrlFromIntent(intent: Intent?): String? {
        if (intent == null) return null
        val action = intent.action
        val type = intent.type
        if ((Intent.ACTION_SEND == action || Intent.ACTION_SEND_MULTIPLE == action) && type?.startsWith("text/") == true) {
            val text = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (!text.isNullOrBlank()) {
                val matcher = Patterns.WEB_URL.matcher(text)
                if (matcher.find()) {
                    val url = matcher.group()
                    return try {
                        url.toUri().toString() } catch (_: Exception) { url }
                }
            }
        }
        return null
    }

    companion object {
        const val EXTRA_SHARED_LINK = "shared_link"
    }
}
