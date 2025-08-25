package org.jayhsu.xsaver.network.model

/**
 * Tweet parsed result model extracted from LinkParser for reuse / serialization.
 */
 data class ParsedTweet(
	val avatarUrl: String?,
	val accountName: String?,
	val text: String?,
	val videoList: List<Video>,
	val imageList: List<String>
 ) {
	data class Video(
		val poster: String?,
		val sources: List<Source>
	) {
		data class Source(val url: String, val type: String?)
	}
 }
