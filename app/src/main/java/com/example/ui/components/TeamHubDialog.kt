package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.provider.ApiFootballClient
import com.example.data.provider.DataResult
import com.example.data.provider.EspnTeamApi
import com.example.data.provider.HubMatch
import com.example.data.provider.HubNews
import com.example.data.provider.HubPlayer
import com.example.data.provider.TeamHubData
import com.example.model.PlayerLineup
import com.example.model.TeamTransfer
import com.example.ui.theme.AppColors

private val SOFT = Color.White.copy(alpha = 0.65f)

/**
 * Real team page: fixtures (past + upcoming), squad, transfer news and team news.
 * All data comes from [EspnTeamApi] (same team id for logo, players and photos). Nothing is invented.
 */
@Composable
fun TeamHubDialog(
    teamName: String,
    leagueName: String = "",
    onDismiss: () -> Unit,
    onPlayerClick: (PlayerLineup) -> Unit
) {
    var tab by remember { mutableIntStateOf(0) }
    val state = produceState<TeamHubData?>(initialValue = null, teamName, leagueName) {
        value = try {
            EspnTeamApi.load(teamName, leagueName)
        } catch (_: Exception) {
            null
        }
    }
    val data = state.value

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = AppColors.Bg) {
            Column(modifier = Modifier.fillMaxSize()) {
                HubHeader(teamName = teamName, data = data, onDismiss = onDismiss)
                HubTabs(selected = tab, onSelect = { tab = it })
                if (data == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AppColors.NeonBlue)
                    }
                } else {
                    when (tab) {
                        0 -> HubMatchesTab(data)
                        1 -> HubPlayersTab(data, onPlayerClick)
                        2 -> HubTransfersTab(teamName = teamName, data = data)
                        else -> HubNewsList(items = data.news, note = "", empty = "لا توجد أخبار حالياً.")
                    }
                }
            }
        }
    }
}

@Composable
private fun HubHeader(teamName: String, data: TeamHubData?, onDismiss: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.BgElevated)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            val logo = data?.logo ?: ""
            if (logo.isNotBlank()) {
                AsyncImage(
                    model = logo,
                    contentDescription = teamName,
                    modifier = Modifier.size(44.dp),
                    contentScale = ContentScale.Fit
                )
            } else {
                Text(text = teamName.take(1), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = data?.name ?: teamName, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val line = listOfNotNull(
                data?.standing?.takeIf { it.isNotBlank() },
                data?.stadium?.takeIf { it.isNotBlank() }?.let { "الملعب: $it" },
                data?.coach?.takeIf { it.isNotBlank() }?.let { "المدرب: $it" }
            ).joinToString(" • ")
            if (line.isNotBlank()) {
                Text(text = line, color = SOFT, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = Icons.Rounded.Close, contentDescription = "إغلاق", tint = Color.White)
        }
    }
}

@Composable
private fun HubTabs(selected: Int, onSelect: (Int) -> Unit) {
    val titles = listOf("المباريات", "اللاعبون", "الصفقات", "الأخبار")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.BgElevated)
    ) {
        titles.forEachIndexed { index, title ->
            val on = index == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelect(index) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = title,
                        color = if (on) AppColors.NeonBlue else SOFT,
                        fontSize = 14.sp,
                        fontWeight = if (on) FontWeight.Bold else FontWeight.Normal
                    )
                    Spacer(Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .height(2.dp)
                            .width(28.dp)
                            .background(if (on) AppColors.NeonBlue else Color.Transparent)
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        color = Color.White,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 14.dp, bottom = 6.dp)
    )
}

@Composable
private fun EmptyNote(text: String) {
    Text(text = text, color = SOFT, fontSize = 13.sp, modifier = Modifier.padding(vertical = 8.dp))
}

@Composable
private fun HubMatchesTab(data: TeamHubData) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 6.dp)
    ) {
        if (!data.resolved) {
            item { EmptyNote("تعذّر تحديد هذا الفريق في مصدر البيانات، لذلك لا تتوفر مبارياته ولاعبوه. الأخبار تعمل في تبويبها.") }
        }
        item { SectionTitle("المباريات القادمة") }
        if (data.upcoming.isEmpty()) item { EmptyNote("لا توجد مباريات قادمة في المصدر حالياً.") }
        items(data.upcoming) { m -> HubMatchRow(m) }
        item { SectionTitle("آخر النتائج") }
        if (data.recent.isEmpty()) item { EmptyNote("لا توجد نتائج سابقة في المصدر حالياً.") }
        items(data.recent) { m -> HubMatchRow(m) }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun HubMatchRow(m: HubMatch) {
    val badge = when (m.result) {
        "W" -> AppColors.NeonGreen
        "L" -> AppColors.NeonRed
        "D" -> AppColors.Gold
        else -> Color.Transparent
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(AppColors.Surface)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(34.dp), contentAlignment = Alignment.Center) {
            if (m.opponentLogo.isNotBlank()) {
                AsyncImage(model = m.opponentLogo, contentDescription = m.opponent, modifier = Modifier.size(30.dp), contentScale = ContentScale.Fit)
            } else {
                Text(text = m.opponent.take(1), color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = (if (m.isHome) "ضد " else "عند ") + m.opponent, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(text = m.dateLabel, color = SOFT, fontSize = 11.sp)
        }
        if (m.scoreText.isNotBlank()) {
            Text(text = m.scoreText, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(badge),
                contentAlignment = Alignment.Center
            ) {
                Text(text = m.result, color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun HubPlayersTab(data: TeamHubData, onPlayerClick: (PlayerLineup) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 6.dp)
    ) {
        if (data.players.isEmpty()) {
            item { EmptyNote("قائمة اللاعبين غير متاحة من المصدر لهذا الفريق حالياً.") }
        } else {
            item { EmptyNote("${data.players.size} لاعباً • اضغط على اللاعب لعرض ملفه") }
        }
        items(data.players) { p -> HubPlayerRow(p, onPlayerClick) }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun HubPlayerRow(p: HubPlayer, onPlayerClick: (PlayerLineup) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(AppColors.Surface)
            .clickable {
                onPlayerClick(
                    PlayerLineup(
                        number = p.number,
                        name = p.name,
                        position = p.position,
                        rating = 0f,
                        photoUrl = p.photoUrl,
                        athleteId = p.id
                    )
                )
            }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PlayerAvatarView(photoUrl = p.photoUrl, playerName = p.name, playerNumber = p.number, size = 44.dp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = p.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val line = listOf(
                p.position.takeIf { it.isNotBlank() },
                p.age.takeIf { it > 0 }?.let { "$it سنة" },
                p.nationality.takeIf { it.isNotBlank() }
            ).filterNotNull().joinToString(" • ")
            if (line.isNotBlank()) Text(text = line, color = SOFT, fontSize = 11.sp)
        }
        if (p.number > 0) {
            Text(text = "#${p.number}", color = AppColors.NeonBlue, fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
        }
    }
}

@Composable
private fun HubNewsRow(n: HubNews) {
    val uri = LocalUriHandler.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(AppColors.Surface)
            .clickable {
                try {
                    uri.openUri(n.url)
                } catch (_: Exception) {}
            }
            .padding(12.dp)
    ) {
        Text(text = n.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 3, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(4.dp))
        Text(text = listOf(n.source, n.timeAgo).filter { it.isNotBlank() }.joinToString(" • "), color = SOFT, fontSize = 11.sp)
    }
}

@Composable
private fun HubNewsList(items: List<HubNews>, note: String, empty: String) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (note.isNotBlank()) item { EmptyNote(note) }
        if (items.isEmpty()) item { EmptyNote(empty) }
        items(items) { n -> HubNewsRow(n) }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun HubTransfersTab(teamName: String, data: TeamHubData) {
    val transfersState = produceState<DataResult<List<TeamTransfer>>?>(initialValue = null, teamName) {
        value = try {
            ApiFootballClient.fetchConfirmedTransfers(teamName)
        } catch (e: Exception) {
            DataResult.Error(
                errorType = com.example.data.provider.ErrorType.UNKNOWN,
                message = e.message ?: "خطأ في الشبكة",
                source = "API-Football",
                throwable = e
            )
        }
    }
    val result = transfersState.value

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 6.dp)
    ) {
        item { SectionTitle("الانتقالات الرسمية المؤكدة (API-Football)") }

        if (result == null) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(color = AppColors.NeonBlue, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("جارٍ التحقق من صفقات الانتقالات المؤكدة عبر API-Football...", color = SOFT, fontSize = 12.sp)
                }
            }
        } else {
            when (result) {
                is DataResult.Success -> {
                    if (result.data.isEmpty()) {
                        item { EmptyNote("لا توجد صفقات انتقال مؤكدة مسجلة في المصدر حالياً.") }
                    } else {
                        item {
                            Text(
                                text = "تم العثور على ${result.data.size} صفقة مؤكدة من المصدر:",
                                color = AppColors.NeonGreen,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }
                        items(result.data) { transfer ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(AppColors.Surface)
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                PlayerAvatarView(photoUrl = null, playerName = transfer.playerName, size = 36.dp)
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = transfer.playerName,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${if (transfer.isIncoming) "قادم من: " else "منتقل إلى: "}${transfer.otherClub}",
                                        color = SOFT,
                                        fontSize = 11.sp
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (transfer.isIncoming) AppColors.NeonBlue.copy(alpha = 0.2f) else AppColors.Coral.copy(alpha = 0.2f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = transfer.type,
                                            color = if (transfer.isIncoming) AppColors.NeonBlue else AppColors.Coral,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    if (transfer.season.isNotBlank() && transfer.season != "غير متاح") {
                                        Spacer(Modifier.height(2.dp))
                                        Text(text = transfer.season, color = SOFT, fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }
                }
                is DataResult.Empty -> {
                    item {
                        EmptyNote("الانتقالات الرسمية: غير متاح (${result.message})")
                    }
                }
                is DataResult.Error -> {
                    item {
                        EmptyNote("الانتقالات الرسمية: غير متاح (${result.message})")
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(14.dp))
            SectionTitle("أخبار وتقارير الانتقالات (الصحافة الرياضية)")
        }

        if (data.transferNews.isEmpty()) {
            item { EmptyNote("لا توجد تقارير انتقالات صحفية حالياً.") }
        } else {
            items(data.transferNews) { n ->
                HubNewsRow(n)
                Spacer(Modifier.height(6.dp))
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}
