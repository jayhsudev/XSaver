package org.jayhsu.xsaver.network

import android.content.Context
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jayhsu.xsaver.network.model.ParsedTweet
import javax.inject.Named
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Parse X (Twitter) tweet HTML and extract avatar, account name, text, videos, images.
 * Uses external model ParsedTweet for easier reuse and testing.
 */
@Singleton
class LinkParser @Inject constructor(
	@param:ApplicationContext private val context: Context
) {

	// 获取贴文 HTML（IO 线程），避免 runBlocking 阻塞主线程
	suspend fun parseTweetLink(link: String): String? = runCatching {
		withContext(Dispatchers.IO) { WebviewFetcher.fetchHtml(context, link) }
	}.getOrNull()

	fun parseTweetHtml(html: String?): ParsedTweet? {
		val nonNullHtml = html ?: return null
		val doc: Document = Jsoup.parse(nonNullHtml)
		val article = doc.selectFirst("article[data-testid=tweet]") ?: return null

		val avatarUrl = article.selectFirst("div[data-testid=Tweet-User-Avatar] img")?.attr("src")

		val accountName = article.selectFirst("div[data-testid=User-Name]")
			?.selectFirst("span span")
			?.text()

		val textContent = article.selectFirst("div[data-testid=tweetText]")
			?.select("span")
			?.joinToString("") { it.text() }
			?.ifBlank { null }

		// Videos: locate containers inside tweetPhoto blocks
		val videoList = mutableListOf<ParsedTweet.Video>()
		article.select("div[data-testid=tweetPhoto]").forEach { photoBlock ->
			photoBlock.select("div[data-testid=videoComponent]").forEach { videoComp ->
				val videoTag = videoComp.selectFirst("video")
				val poster = videoTag?.attr("poster")
				val sources = videoComp.select("source[type=video/mp4]").mapNotNull { srcEl ->
					val src = srcEl.attr("src")
					if (src.isNullOrBlank()) null else ParsedTweet.Video.Source(src, srcEl.attr("type"))
				}
				if (poster != null || sources.isNotEmpty()) {
					videoList += ParsedTweet.Video(poster = poster, sources = sources)
				}
			}
		}

		// Images (exclude video posters)
		val imageList = article.select("div[data-testid=tweetPhoto] img")
			.mapNotNull { img -> img.attr("src").takeIf { it.isNotBlank() } }
			.distinct()

		return ParsedTweet(
			avatarUrl = avatarUrl,
			accountName = accountName,
			text = textContent,
			videoList = videoList,
			imageList = imageList
		)
	}
}