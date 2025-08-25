package org.jayhsu.xsaver.network

import android.annotation.SuppressLint
import android.content.Context
import android.net.http.SslError
import android.os.Build
import android.util.Log
import android.webkit.*
import android.view.View
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.coroutineContext

/**
 * Lightweight HTML fetcher using an off-screen WebView.
 * Returns outerHTML or null on failure/timeout.
 */
object WebviewFetcher {

    private const val TAG = "WebviewFetcher"
    private const val JS_INTERFACE_NAME = "XSaverFetcher"

    /**
     * Fetches HTML from a URL using a headless WebView.
     *
     * @param context Context.
     * @param url The URL to fetch.
     * @param timeoutMs Total timeout for the operation.
     * @param waitSelector Optional CSS selector to wait for. If present, the fetch will wait until an element matching this selector appears.
     * @param pollIntervalMs How often to check for the `waitSelector`.
     * @return The HTML content (outer HTML of the element if `waitSelector` is used, otherwise the full document HTML), or `null` on failure or timeout.
     */
    @SuppressLint("SetJavaScriptEnabled", "AddJavascriptInterface", "JavascriptInterface")
    suspend fun fetchHtml(
        context: Context,
        url: String,
        timeoutMs: Long = 25_000L,
        waitSelector: String? = null,
        pollIntervalMs: Long = 500L,
    ): String? = withContext(Dispatchers.Main) {
        val t0 = System.currentTimeMillis()
        Log.d(TAG, "Fetching url=$url, timeout=${timeoutMs}ms, selector=$waitSelector")

        val result = CompletableDeferred<String?>()
        val isDone = AtomicBoolean(false)
        val wv = createAndConfigureWebView(context)

        val job = SupervisorJob()
        val scope = CoroutineScope(Dispatchers.Main + job)

        // Cleanup logic
        fun cleanup() {
            if (isDone.compareAndSet(false, true)) {
                job.cancel() // Cancel all coroutines started in the scope
                wv.removeJavascriptInterface(JS_INTERFACE_NAME)
                wv.stopLoading()
                wv.destroy()
                val elapsed = System.currentTimeMillis() - t0
                Log.d(TAG, "WebView destroyed. Total time: ${elapsed}ms")
            }
        }

        // Setup WebViewClient
        wv.webViewClient = createWebViewClient(isDone, result, scope, waitSelector, pollIntervalMs)

        // Setup JavascriptInterface to receive results from JS
        wv.addJavascriptInterface(createJavascriptInterface(isDone, result), JS_INTERFACE_NAME)

        try {
            withTimeout(timeoutMs) {
                wv.loadUrl(url)
                val html = result.await()
                val elapsed = System.currentTimeMillis() - t0
                if (html != null) {
                    Log.d(TAG, "Success. url=$url, length=${html.length}, time=${elapsed}ms")
                } else {
                    Log.w(TAG, "Finished with null result. url=$url, time=${elapsed}ms")
                }
                html
            }
        } catch (e: TimeoutCancellationException) {
            val elapsed = System.currentTimeMillis() - t0
            Log.e(TAG, "Timeout fetching url=$url, time=${elapsed}ms", e)
            null
        } catch (e: Exception) {
            val elapsed = System.currentTimeMillis() - t0
            Log.e(TAG, "Exception fetching url=$url, time=${elapsed}ms", e)
            null
        } finally {
            cleanup()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun createAndConfigureWebView(context: Context): WebView {
        return WebView(context.applicationContext).apply {
            // Optimization for headless fetching
            setWillNotDraw(true)
            setLayerType(View.LAYER_TYPE_SOFTWARE, null)

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                loadsImagesAutomatically = false
                blockNetworkImage = true
                val desktopUA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
                userAgentString = "$desktopUA XSaverFetcher/1.0"
            }
        }
    }

    private fun createWebViewClient(
        isDone: AtomicBoolean,
        result: CompletableDeferred<String?>,
        scope: CoroutineScope,
        waitSelector: String?,
        pollIntervalMs: Long
    ): WebViewClient {
        return object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                if (isDone.get()) return
                Log.d(TAG, "onPageFinished: $url")
                scope.launch {
                    if (waitSelector != null) {
                        pollForSelector(view, waitSelector, pollIntervalMs)
                    } else {
                        // No selector, just grab the document HTML
                        evaluateJs(view, getDocumentOuterHtmlJs())
                    }
                }
            }

            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                if (request.isForMainFrame && isDone.compareAndSet(false, true)) {
                    Log.e(TAG, "onReceivedError: code=${error.errorCode}, desc=${error.description}")
                    result.complete(null)
                }
            }

            override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
                if (isDone.compareAndSet(false, true)) {
                    Log.e(TAG, "onReceivedSslError: primary=${error.primaryError}, url=${error.url}")
                    handler.cancel()
                    result.complete(null)
                }
            }

            @RequiresApi(Build.VERSION_CODES.O)
            override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
                if (isDone.compareAndSet(false, true)) {
                    Log.e(TAG, "onRenderProcessGone: didCrash=${detail.didCrash()}")
                    result.complete(null)
                }
                return true
            }
        }
    }

    private fun createJavascriptInterface(isDone: AtomicBoolean, result: CompletableDeferred<String?>): Any {
        return object {
            @JavascriptInterface
            fun onResult(html: String?) {
                if (isDone.compareAndSet(false, true)) {
                    Log.d(TAG, "JS interface received result, length=${html?.length ?: -1}")
                    result.complete(html)
                }
            }
        }
    }

    private suspend fun pollForSelector(view: WebView, selector: String, pollIntervalMs: Long) {
        Log.d(TAG, "Polling for selector: $selector")
        while (coroutineContext.isActive) {
            val checkJs = """
                (function() {
                    try { return !!document.querySelector(${selector.quoteForJs()}); }
                    catch(e) { return false; }
                })()
            """.trimIndent()

            val found = CompletableDeferred<Boolean>()
            view.evaluateJavascript(checkJs) { value ->
                found.complete(value == "true")
            }

            if (found.await()) {
                Log.d(TAG, "Selector found: $selector")
                evaluateJs(view, getElementOuterHtmlJs(selector))
                return
            }
            delay(pollIntervalMs)
        }
    }

    private fun evaluateJs(view: WebView, script: String) {
        val fullScript = """
            (function() {
                try {
                    const result = $script;
                    window.$JS_INTERFACE_NAME.onResult(result);
                } catch (e) {
                    window.$JS_INTERFACE_NAME.onResult(null);
                }
            })();
        """.trimIndent()
        view.evaluateJavascript(fullScript, null)
    }

    private fun getDocumentOuterHtmlJs(): String =
        "(document.documentElement && document.documentElement.outerHTML) || (document.body && document.body.outerHTML) || null"

    private fun getElementOuterHtmlJs(selector: String): String =
        "(document.querySelector(${selector.quoteForJs()}) || {}).outerHTML || null"

    private fun String.quoteForJs(): String = "\"${this.replace("\"", "\\\"")}\""
}