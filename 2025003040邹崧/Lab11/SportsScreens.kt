package com.example.myapplicationlab10.ui

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplicationlab10.data.LocalSportsDataProvider
import com.example.myapplicationlab10.model.Sport
import com.example.myapplicationlab10.utils.SportsContentType
import com.example.myapplicationlab10.viewmodel.SportsViewModel
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.activity.compose.BackHandler

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun SportsApp(
    windowWidthSizeClass: WindowWidthSizeClass = WindowWidthSizeClass.Compact,
    viewModel: SportsViewModel = viewModel()
) {
    val contentType = when (windowWidthSizeClass) {
        WindowWidthSizeClass.Expanded -> SportsContentType.ListAndDetail
        else -> SportsContentType.ListOnly
    }
    val uiState = viewModel.uiState

    Scaffold(
        topBar = {
            SportsAppBar(
                isShowingListPage = uiState.isShowingListPage,
                isListAndDetail = contentType == SportsContentType.ListAndDetail,
                onBackButtonClick = { viewModel.navigateToListPage() }
            )
        }
    ) { innerPadding ->
        if (contentType == SportsContentType.ListAndDetail) {
            SportsListAndDetails(
                sports = LocalSportsDataProvider.allSportsData,
                currentSport = uiState.currentSport,
                onSportClick = { viewModel.updateCurrentSport(it) },
                contentPadding = innerPadding
            )
        } else {
            if (uiState.isShowingListPage) {
                SportsList(
                    sports = LocalSportsDataProvider.allSportsData,
                    onClick = {
                        viewModel.updateCurrentSport(it)
                        viewModel.navigateToDetailPage()
                    },
                    contentPadding = innerPadding
                )
            } else {
                SportsDetail(
                    selectedSport = uiState.currentSport,
                    onBackPressed = { viewModel.navigateToListPage() },
                    contentPadding = innerPadding
                )
            }
        }
    }
}

@Composable
private fun SportsListAndDetails(
    sports: List<Sport>,
    currentSport: Sport,
    onSportClick: (Sport) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    BackHandler { (context as Activity).finish() }

    Row(modifier = modifier.fillMaxSize()) {
        SportsList(
            sports = sports,
            onClick = onSportClick,
            modifier = Modifier.weight(1f),
            contentPadding = contentPadding
        )
        SportsDetail(
            selectedSport = currentSport,
            onBackPressed = {},
            contentPadding = contentPadding,
            modifier = Modifier.weight(2f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SportsAppBar(
    isShowingListPage: Boolean,
    isListAndDetail: Boolean,
    onBackButtonClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = {
            Text(
                text = if (isListAndDetail || isShowingListPage) "Sports" else "Sport Info"
            )
        },
        navigationIcon = {
            if (!isShowingListPage && !isListAndDetail) {
                IconButton(onClick = onBackButtonClick) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        modifier = modifier
    )
}

@Composable
private fun SportsList(
    sports: List<Sport>,
    onClick: (Sport) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    LazyColumn(contentPadding = contentPadding, modifier = modifier) {
        items(sports, key = { it.id }) { sport ->
            SportsListItem(sport = sport, onItemClick = onClick)
        }
    }
}

@Composable
private fun SportsListItem(
    sport: Sport,
    onItemClick: (Sport) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        elevation = CardDefaults.cardElevation(2.dp),
        shape = MaterialTheme.shapes.medium,
        modifier = modifier
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .fillMaxWidth(),
        onClick = { onItemClick(sport) }
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(MaterialTheme.colorScheme.primary)
            )
            Column(modifier = Modifier.padding(start = 16.dp)) {
                Text(
                    sport.title,
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    sport.subtitle,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
private fun SportsDetail(
    selectedSport: Sport,
    onBackPressed: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    LazyColumn(contentPadding = contentPadding, modifier = modifier.fillMaxSize()) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Text(
                    text = selectedSport.title,
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            Text(
                text = "运动员人数: ${selectedSport.athleteCount}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )
            Text(
                text = selectedSport.detail,
                textAlign = TextAlign.Justify,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
    BackHandler(onBack = onBackPressed)
}

@Preview(widthDp = 900, showBackground = true)
@Composable
fun TabletPreview() {
    MaterialTheme {
        SportsListAndDetails(
            sports = LocalSportsDataProvider.allSportsData,
            currentSport = LocalSportsDataProvider.defaultSport,
            onSportClick = {},
            contentPadding = PaddingValues(0.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PhonePreview() {
    MaterialTheme {
        SportsApp()
    }
}