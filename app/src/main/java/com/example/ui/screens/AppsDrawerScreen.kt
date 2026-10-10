package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppItem
import com.example.ui.components.AppCard
import com.example.ui.theme.AuraPrimary
import com.example.ui.theme.AuraSecondary

@Composable
fun AppsDrawerScreen(
    installedApps: List<AppItem>,
    importedApks: List<AppItem>,
    searchQuery: String,
    onLaunchWithLogo: (AppItem) -> Unit,
    onBrandApk: (AppItem) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf("all") }

    val allCombinedApps = remember(installedApps, importedApks) {
        importedApks + installedApps
    }

    val filteredApps = remember(allCombinedApps, searchQuery, selectedFilter) {
        allCombinedApps.filter { app ->
            val matchesFilter = when (selectedFilter) {
                "imported" -> app.isImportedApk
                "installed" -> !app.isImportedApk
                else -> true
            }
            val matchesSearch = searchQuery.isBlank() ||
                    app.name.contains(searchQuery, ignoreCase = true) ||
                    app.packageName.contains(searchQuery, ignoreCase = true)

            matchesFilter && matchesSearch
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Filter Chips Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedFilter == "all",
                onClick = { selectedFilter = "all" },
                label = { Text("All (${allCombinedApps.size})", fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = AuraPrimary.copy(alpha = 0.2f),
                    selectedLabelColor = AuraPrimary,
                    containerColor = Color(0xFF131B2E),
                    labelColor = Color(0xFF94A3B8)
                ),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = if (selectedFilter == "all") AuraPrimary else Color(0xFF1E293B),
                    enabled = true,
                    selected = selectedFilter == "all"
                ),
                modifier = Modifier.testTag("filter_chip_all")
            )

            FilterChip(
                selected = selectedFilter == "imported",
                onClick = { selectedFilter = "imported" },
                label = { Text("Imported APKs (${importedApks.size})", fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = AuraSecondary.copy(alpha = 0.2f),
                    selectedLabelColor = AuraSecondary,
                    containerColor = Color(0xFF131B2E),
                    labelColor = Color(0xFF94A3B8)
                ),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = if (selectedFilter == "imported") AuraSecondary else Color(0xFF1E293B),
                    enabled = true,
                    selected = selectedFilter == "imported"
                ),
                modifier = Modifier.testTag("filter_chip_imported")
            )

            FilterChip(
                selected = selectedFilter == "installed",
                onClick = { selectedFilter = "installed" },
                label = { Text("Installed (${installedApps.size})", fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = AuraPrimary.copy(alpha = 0.2f),
                    selectedLabelColor = AuraPrimary,
                    containerColor = Color(0xFF131B2E),
                    labelColor = Color(0xFF94A3B8)
                ),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = if (selectedFilter == "installed") AuraPrimary else Color(0xFF1E293B),
                    enabled = true,
                    selected = selectedFilter == "installed"
                ),
                modifier = Modifier.testTag("filter_chip_installed")
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (filteredApps.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.SearchOff,
                        contentDescription = null,
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "No applications found",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Try adjusting your search query or importing an APK from storage.",
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(filteredApps, key = { it.packageName + (it.sourceApkPath ?: "") }) { app ->
                    AppCard(
                        app = app,
                        onLaunchWithLogo = { onLaunchWithLogo(app) },
                        onBrandApk = { onBrandApk(app) }
                    )
                }
            }
        }
    }
}
