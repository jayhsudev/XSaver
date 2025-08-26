package org.jayhsu.xsaver.domain.usecase

import org.jayhsu.xsaver.data.repository.MediaRepository
import org.jayhsu.xsaver.network.model.ParsedTweet
import javax.inject.Inject

/**
 * High-level use case: parse a tweet URL into ParsedTweet + media items.
 * Returns pair (ParsedTweet?, mediaItems).
 */
class ParseTweetUseCase @Inject constructor(
    private val repository: MediaRepository
) {
    suspend operator fun invoke(url: String): Result<Pair<ParsedTweet?, List<org.jayhsu.xsaver.data.model.MediaItem>>> = runCatching {
        val mediaItems = repository.parseLink(url)
        val parsed = mediaItems.firstOrNull()?.let { _ -> null } // placeholder if later we separate
        parsed to mediaItems
    }
}
