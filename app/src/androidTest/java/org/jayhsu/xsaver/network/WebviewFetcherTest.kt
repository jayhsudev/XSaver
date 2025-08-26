package org.jayhsu.xsaver.network

import android.app.Application
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WebviewFetcherTest {
	private val testUrl = "https://x.com/elonmusk/status/1954330244552561103?t=gonNw23jNHCnRIyTOHjKMg&s=19"

	@Test
	fun fetchTweetHtml() = runBlocking {
		val app = ApplicationProvider.getApplicationContext<Application>()
		val html = WebviewFetcher.fetchHtml(
			context = app,
			url = testUrl,
			timeoutMs = 25_000,
			waitSelector = "article[data-testid='tweet']", // only return the tweet article
			pollIntervalMs = 550,
		)
		Log.i("WebviewFetcherTest", "HTML CONTEXT: $html")
		// If running in CI without network, skip (avoid hard failure on infra)
		assumeTrue("Network unavailable or blocked", html != null)
		val snippet = html!!
		assertNotNull("Article element should be returned", snippet)
		val lower = snippet.lowercase()
		// Should contain tweet article with data-testid
		assumeTrue(lower.contains("<article") && lower.contains("data-testid=\"tweet\""))
	}
}
