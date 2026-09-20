package dev.qtremors.osyster.feature.home

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Security
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.qtremors.osyster.core.device.domain.DevicePage
import dev.qtremors.osyster.core.ui.catalog.label
import dev.qtremors.osyster.core.ui.catalog.title
import dev.qtremors.osyster.core.ui.components.ConnectedGroup
import dev.qtremors.osyster.core.ui.components.HeroCard
import dev.qtremors.osyster.core.ui.components.HeroMetric
import dev.qtremors.osyster.core.ui.components.NavigationRow
import dev.qtremors.osyster.core.ui.components.OsysterScaffold
import dev.qtremors.osyster.core.ui.components.SectionHeader

@Composable
internal fun HomeScreen(
    state: HomeState,
    onOpenPage: (DevicePage) -> Unit
) {
    OsysterScaffold(title = stringResource(R.string.home_title)) { padding ->
        LazyColumn(contentPadding = padding) {
            item(key = "hero") {
                HeroCard(
                    title = state.hero.name,
                    subtitle = stringResource(R.string.home_hero_subtitle, state.hero.androidRelease, state.hero.apiLevel),
                    metrics = heroMetrics(state.hero),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            items(state.groups, key = { (group, _) -> group.name }) { (group, pages) ->
                SectionHeader(stringResource(group.title))
                ConnectedGroup {
                    pages.forEach { page ->
                        val label = page.label
                        NavigationRow(
                            title = stringResource(label.title),
                            supporting = stringResource(label.summary),
                            icon = label.icon,
                            badgeKey = page.id,
                            onClick = { onOpenPage(page) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun heroMetrics(hero: HeroIdentity): List<HeroMetric> = buildList {
    hero.securityPatch?.let {
        add(HeroMetric(label = stringResource(R.string.home_metric_patch), value = it, icon = Icons.Rounded.Security))
    }
    hero.chip?.let {
        add(HeroMetric(label = stringResource(R.string.home_metric_chip), value = it, icon = Icons.Rounded.Memory))
    }
}
