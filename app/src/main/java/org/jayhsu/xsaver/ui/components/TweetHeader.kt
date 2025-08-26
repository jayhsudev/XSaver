package org.jayhsu.xsaver.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
    maxCollapsedLines: Int = 3,
    enableCollapse: Boolean = true,
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

                var expanded by remember(text) { mutableStateOf(false) }
                var canExpand by remember(text) { mutableStateOf(false) }

                Column(modifier = Modifier) {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = if (expanded || !enableCollapse) Int.MAX_VALUE else maxCollapsedLines,
                        overflow = TextOverflow.Ellipsis,
                        onTextLayout = { layoutResult ->
                            if (enableCollapse && !expanded) {
                                // If original line count exceeds collapsed limit, allow expansion
                                val more = layoutResult.lineCount > maxCollapsedLines
                                if (more != canExpand) canExpand = more
                            }
                        }
                    )
                    if (enableCollapse && canExpand) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (expanded) "收起" else "更多",
                            style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .clip(MaterialTheme.shapes.extraSmall)
                                .padding(end = 8.dp)
                                .clickable { expanded = !expanded },
                            maxLines = 1
                        )
                    }
                }
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

@Preview(showBackground = true, name = "TweetHeader Collapsed Body")
@Composable
private fun PreviewTweetHeaderCollapsed() {
    Surface {
        TweetHeader(
            avatarUrl = "https://abs.twimg.com/sticky/default_profile_images/default_profile_normal.png",
            accountName = "SampleUser",
            text = (1..12).joinToString(" ") { "段落$it" } + " 结尾",
            maxCollapsedLines = 3
        )
    }
}

@Preview(showBackground = true, name = "TweetHeader Expanded Body")
@Composable
private fun PreviewTweetHeaderExpanded() {
    Surface {
        // Force expanded preview by disabling collapse for demonstration
        TweetHeader(
            avatarUrl = null,
            accountName = "ExpandUser",
            text = List(30) { "文本${it}" }.joinToString(" "),
            enableCollapse = false
        )
    }
}
