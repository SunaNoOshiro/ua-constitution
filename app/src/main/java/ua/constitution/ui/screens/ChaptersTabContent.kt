package ua.constitution.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.constitution.R
import ua.constitution.data.model.Article
import ua.constitution.data.model.Chapter
import ua.constitution.domain.content.ChapterRangeKind
import ua.constitution.domain.content.chapterRangeKind
import ua.constitution.ui.article.formatArticleId
import ua.constitution.ui.theme.SovereignBlue

@Composable
fun ChaptersTabContent(
    chapters: List<Chapter>,
    articlesForChapter: (Int) -> List<Article>,
    onSelectChapter: (Int) -> Unit,
    onOpenSourceUrl: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Text(
            text = stringResource(R.string.select_chapter_header),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            color = SovereignBlue,
            modifier = Modifier.padding(vertical = 10.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 20.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(chapters) { chapter ->
                val chapterArticles = remember(chapter.id) {
                    articlesForChapter(chapter.id)
                }
                val preambleStr = stringResource(R.string.preamble)
                val rangeText = if (chapterArticles.isNotEmpty()) {
                    val firstId = formatArticleId(chapterArticles.first().id)
                    val lastId = formatArticleId(chapterArticles.last().id)
                    when (chapterRangeKind(chapter.id, firstId == lastId)) {
                        ChapterRangeKind.PREAMBLE -> preambleStr
                        ChapterRangeKind.POINT_SINGLE -> stringResource(R.string.point_range_single, firstId)
                        ChapterRangeKind.POINT_MULTI -> stringResource(R.string.point_range_multi, firstId, lastId)
                        ChapterRangeKind.ARTICLE_SINGLE -> stringResource(R.string.article_range_single, firstId)
                        ChapterRangeKind.ARTICLE_MULTI -> stringResource(R.string.article_range_multi, firstId, lastId)
                    }
                } else {
                    ""
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("chapter_card_${chapter.id}")
                        .clickable { onSelectChapter(chapter.id) }
                        .shadow(2.dp, RoundedCornerShape(14.dp)),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.5.dp, SovereignBlue)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(SovereignBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${chapter.id}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "${stringResource(R.string.chapter_singular)} ${chapter.id}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = SovereignBlue
                                )
                                if (rangeText.isNotEmpty()) {
                                    Text(
                                        text = "($rangeText)",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                                        fontWeight = FontWeight.Black,
                                        color = SovereignBlue.copy(alpha = 0.6f)
                                    )
                                }
                            }
                            Text(
                                text = chapter.titleUa,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = SovereignBlue
                            )
                        }

                        if (chapter.sourceUrl.isNotEmpty()) {
                            IconButton(
                                onClick = { onOpenSourceUrl(chapter.sourceUrl) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = stringResource(R.string.read_chapter_source),
                                    tint = SovereignBlue.copy(alpha = 0.7f),
                                    modifier = Modifier.size(18.dp)
                                 )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = stringResource(R.string.open_chapter_articles),
                            tint = SovereignBlue
                        )
                    }
                }
            }
        }
    }
}
