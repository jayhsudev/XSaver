package org.jayhsu.xsaver.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.material3.Surface

/**
 * 显示 X 贴文 头像 + 账户名 + 正文
 * 结构:
 *  Row( avatar, Column( accountName, bodyText ) ) 但按要求: 账户名单独一行靠左, 正文在其下。
 */
@Composable
fun TweetHeader(
    avatarUrl: String?,
    accountName: String?,
    text: String?,
    modifier: Modifier = Modifier,
    avatarSize: Int = 40,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        // Avatar
        AsyncImage(
            model = avatarUrl,
            contentDescription = "avatar",
            modifier = Modifier
                .size(avatarSize.dp)
                .clip(CircleShape)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            if (!accountName.isNullOrBlank()) {
                Text(
                    text = accountName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (!text.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "TweetHeader Light")
@Composable
private fun PreviewTweetHeaderLight() {
    Surface { 
        TweetHeader(
            avatarUrl = "https://abs.twimg.com/sticky/default_profile_images/default_profile_normal.png",
            accountName = "Elon Musk",
            text = "Just a sample tweet text for preview rendering in Compose."
        )
    }
}

@Preview(showBackground = true, name = "TweetHeader Long Text")
@Composable
private fun PreviewTweetHeaderLongText() {
    Surface { 
        TweetHeader(
            avatarUrl = null,
            accountName = "VeryLongUserName_ABCDEFG_HIJKLMNOP_123456789",
            text = "这是一段较长的正文，用于测试自动换行与布局表现。Second line example."
        )
    }
}
