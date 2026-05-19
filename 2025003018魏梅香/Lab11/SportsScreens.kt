package com.example.sports.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.sports.data.LocalSportsDataProvider
import com.example.sports.model.Sport

// 新增：定义内容类型（单窗格/双窗格）
enum class SportsContentType {
    ListOnly, // 仅列表（小屏）
    ListAndDetail // 列表+详情（大屏）
}

// 主入口：接收 WindowSizeClass 并判断布局类型
@Composable
fun SportsApp(
    windowSizeClass: WindowSizeClass,
    modifier: Modifier = Modifier
) {
    // 根据屏幕宽度判断布局类型
    val contentType = when (windowSizeClass.widthSizeClass) {
        WindowWidthSizeClass.Expanded -> SportsContentType.ListAndDetail // 大屏：双窗格
        else -> SportsContentType.ListOnly // 小屏：单窗格
    }

    // 状态管理：选中的运动、是否显示列表（小屏用）
    val (selectedSport, setSelectedSport) = remember {
        mutableStateOf(LocalSportsDataProvider.allSports.first())
    }
    val (isShowingListPage, setIsShowingListPage) = remember { mutableStateOf(true) }

    MaterialTheme {
        Scaffold(
            topBar = {
                SportsAppBar(
                    contentType = contentType,
                    isShowingListPage = isShowingListPage,
                    onBackButtonClick = { setIsShowingListPage(true) }
                )
            }
        ) { contentPadding ->
            when (contentType) {
                // 大屏：双窗格（列表+详情并排）
                SportsContentType.ListAndDetail -> {
                    SportsListAndDetails(
                        sports = LocalSportsDataProvider.allSports,
                        selectedSport = selectedSport,
                        onSportSelected = { setSelectedSport(it) },
                        contentPadding = contentPadding
                    )
                }
                // 小屏：单窗格（列表/详情切换）
                SportsContentType.ListOnly -> {
                    if (isShowingListPage) {
                        SportsList(
                            sports = LocalSportsDataProvider.allSports,
                            onClick = { sport ->
                                setSelectedSport(sport)
                                setIsShowingListPage(false)
                            },
                            contentPadding = contentPadding
                        )
                    } else {
                        selectedSport?.let { sport ->
                            SportsDetail(
                                selectedSport = sport,
                                onBackPressed = { setIsShowingListPage(true) },
                                contentPadding = contentPadding
                            )
                        }
                    }
                }
            }
        }
    }
}

// 新增：大屏双窗格布局（列表+详情并排）
@Composable
fun SportsListAndDetails(
    sports: List<Sport>,
    selectedSport: Sport,
    onSportSelected: (Sport) -> Unit,
    contentPadding: androidx.compose.foundation.layout.PaddingValues,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxSize()) {
        // 左侧列表（占1/3宽度）
        SportsList(
            sports = sports,
            onClick = onSportSelected,
            contentPadding = contentPadding,
            modifier = Modifier.weight(1f)
        )
        // 右侧详情（占2/3宽度）
        SportsDetail(
            selectedSport = selectedSport,
            onBackPressed = {}, // 大屏不需要返回
            contentPadding = contentPadding,
            modifier = Modifier.weight(2f)
        )
    }
}

// 适配大屏的顶部导航栏
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SportsAppBar(
    contentType: SportsContentType,
    isShowingListPage: Boolean,
    onBackButtonClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = {
            Text(
                text = when {
                    contentType == SportsContentType.ListAndDetail -> "Sports" // 大屏固定标题
                    isShowingListPage -> "Sports List" // 小屏列表页
                    else -> "Sport Detail" // 小屏详情页
                },
                style = MaterialTheme.typography.headlineSmall
            )
        },
        navigationIcon = {
            // 只有小屏详情页显示返回按钮
            if (contentType == SportsContentType.ListOnly && !isShowingListPage) {
                IconButton(onClick = onBackButtonClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack, // 修复过时API
                        contentDescription = "Back"
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        modifier = modifier
    )
}

// 原有列表逻辑（无需修改）
@Composable
fun SportsList(
    sports: List<Sport>,
    onClick: (Sport) -> Unit,
    contentPadding: androidx.compose.foundation.layout.PaddingValues,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp)
    ) {
        items(sports) { sport ->
            SportsListItem(
                sport = sport,
                onClick = { onClick(sport) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// 原有列表项逻辑（无需修改）
@Composable
fun SportsListItem(
    sport: Sport,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = sport.imageResourceId),
                contentDescription = sport.titleResourceId,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = sport.titleResourceId,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = sport.subtitleResourceId,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// 原有详情页逻辑（无需修改）
@Composable
fun SportsDetail(
    selectedSport: Sport,
    onBackPressed: () -> Unit,
    contentPadding: androidx.compose.foundation.layout.PaddingValues,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = selectedSport.sportsImageBanner),
            contentDescription = selectedSport.titleResourceId,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(16.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = selectedSport.titleResourceId,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Players: ${selectedSport.playerCount}", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Olympic: ${if (selectedSport.olympic) "Yes" else "No"}", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = selectedSport.sportDetails,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

// 预览（可选）
@Preview(showBackground = true)
@Composable
fun SportsListItemPreview() {
    MaterialTheme {
        SportsListItem(sport = LocalSportsDataProvider.allSports.first(), onClick = {})
    }
}

@Preview(showBackground = true, widthDp = 1200) // 大屏预览
@Composable
fun SportsListAndDetailsPreview() {
    MaterialTheme {
        SportsListAndDetails(
            sports = LocalSportsDataProvider.allSports,
            selectedSport = LocalSportsDataProvider.allSports.first(),
            onSportSelected = {},
            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
        )
    }
}