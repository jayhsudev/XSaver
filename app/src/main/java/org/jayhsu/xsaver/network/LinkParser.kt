package org.jayhsu.xsaver.network

import android.content.Context
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jayhsu.xsaver.network.model.ParsedTweet
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jayhsu.xsaver.core.error.ParseError

/**
 * Parse X (Twitter) tweet HTML and extract avatar, account name, text, videos, images.
 * Uses external model ParsedTweet for easier reuse and testing.
 */
@Singleton
class LinkParser @Inject constructor(
	@param:ApplicationContext private val context: Context
) {
	val timeoutMs: Long = 10_000L
	val waitSelector: String = "article[data-testid='tweet']"
	val pollIntervalMs: Long = 250L

	data class ParseTweetResult(val tweet: ParsedTweet?, val error: ParseError?)

	// 获取贴文 HTML 并分类错误
	suspend fun parseTweetLink(link: String): ParseTweetResult = try {
		val parsed = withContext(Dispatchers.IO) {
			val html = WebviewFetcher.fetchHtml(context, url = link, timeoutMs, waitSelector, pollIntervalMs)
				?: return@withContext ParseTweetResult(null, ParseError.NetworkTimeout)
			parseTweetHtml(html)
		}
		parsed
	} catch (e: Exception) {
		ParseTweetResult(null, ParseError.Unknown(e.message))
	}

	private fun parseTweetHtml(html: String): ParseTweetResult {
		val doc: Document = Jsoup.parse(html)
		val article = doc.selectFirst("article[data-testid=tweet]")
			?: return ParseTweetResult(null, ParseError.StructureChanged)

		val avatarUrl = article.selectFirst("div[data-testid=Tweet-User-Avatar] img")?.attr("src")
		val accountName = article.selectFirst("div[data-testid=User-Name]")
			?.selectFirst("span span")
			?.text()
		val textContent = article.selectFirst("div[data-testid=tweetText]")
			?.select("span")
			?.joinToString("") { it.text() }
			?.ifBlank { null }

		val videoList = mutableListOf<ParsedTweet.Video>()
		article.select("div[data-testid=tweetPhoto]").forEach { photoBlock ->
			photoBlock.select("div[data-testid=videoComponent]").forEach { videoComp ->
				val videoTag = videoComp.selectFirst("video")
				val poster = videoTag?.attr("poster")
				val sources = videoComp.select("source[type=video/mp4]").mapNotNull { srcEl ->
					val src = srcEl.attr("src")
					if (src.isBlank()) null else ParsedTweet.Video.Source(src, srcEl.attr("type"))
				}
				if (poster != null || sources.isNotEmpty()) {
					videoList += ParsedTweet.Video(poster = poster, sources = sources)
				}
			}
		}

		val imageList = article.select("div[data-testid=tweetPhoto] img")
			.mapNotNull { img -> img.attr("src").takeIf { it.isNotBlank() } }
			.distinct()

		val tweet = ParsedTweet(
			avatarUrl = avatarUrl,
			accountName = accountName,
			text = textContent,
			videoList = videoList,
			imageList = imageList
		)
		if (videoList.isEmpty() && imageList.isEmpty()) {
			return ParseTweetResult(tweet, ParseError.Empty)
		}
		return ParseTweetResult(tweet, null)
	}
}