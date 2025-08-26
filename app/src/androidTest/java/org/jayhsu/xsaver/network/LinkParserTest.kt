package org.jayhsu.xsaver.network

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.jayhsu.xsaver.network.model.ParsedTweet.Video
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LinkParserTest {

	@Test
	fun parseRealTweetLink_printResult() = runBlocking {
		val context = ApplicationProvider.getApplicationContext<Context>()
		val parser = LinkParser(context)
		val link = "https://x.com/elonmusk/status/1954330244552561103?t=gonNw23jNHCnRIyTOHjKMg&s=19"
		val result = parser.parseTweetLink(link)
		println("ParseTweetResult => tweet=${result.tweet} error=${result.error}")
		assertNotNull(result)
	}
}
