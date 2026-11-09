package dev.qtremors.osyster.core.device.data

import dev.qtremors.osyster.core.device.domain.HeroTile
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.qtremors.osyster.core.device.domain.DevicePage
import dev.qtremors.osyster.core.device.domain.HomeLayout
import dev.qtremors.osyster.core.device.domain.HomeLayoutRepository
import dev.qtremors.osyster.core.device.domain.PinKey
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private val Context.homeLayout: DataStore<Preferences> by preferencesDataStore("home_layout")

/**
 * Persists Home's arrangement as stable ids, so renamed pages keep their place and pages added in
 * later versions simply appear at the end of their group.
 */
@Singleton
internal class HomeLayoutStore @Inject constructor(
    @param:ApplicationContext private val context: Context
) : HomeLayoutRepository {
    private object Keys {
        val order = stringPreferencesKey("page_order")
        val hidden = stringPreferencesKey("hidden_pages")
        val pinned = stringPreferencesKey("pinned")
        val tiles = stringPreferencesKey("tile_order")
        val hiddenTiles = stringPreferencesKey("hidden_tiles")
    }

    override val layout: Flow<HomeLayout> = context.homeLayout.data.map(::decode).distinctUntilChanged()

    override suspend fun update(transform: (HomeLayout) -> HomeLayout) {
        context.homeLayout.edit { prefs ->
            val next = transform(decode(prefs))
            prefs[Keys.order] = next.pageOrder.joinToString(SEPARATOR) { it.id }
            prefs[Keys.hidden] = next.hiddenPages.joinToString(SEPARATOR) { it.id }
            prefs[Keys.pinned] = next.pinned.joinToString(SEPARATOR) { it.raw }
            prefs[Keys.tiles] = next.tileOrder.joinToString(SEPARATOR) { it.id }
            prefs[Keys.hiddenTiles] = next.hiddenTiles.joinToString(SEPARATOR) { it.id }
        }
    }

    private fun decode(prefs: Preferences): HomeLayout = HomeLayout(
        pageOrder = DevicePage.restoreOrder(split(prefs[Keys.order])),
        hiddenPages = split(prefs[Keys.hidden]).mapNotNull(DevicePage::fromId).toSet(),
        pinned = split(prefs[Keys.pinned]).mapNotNull(PinKey::parse).distinct(),
        tileOrder = HeroTile.restoreOrder(split(prefs[Keys.tiles])),
        hiddenTiles = split(prefs[Keys.hiddenTiles]).mapNotNull(HeroTile::fromId).toSet()
    )

    private fun split(raw: String?): List<String> = raw?.split(SEPARATOR)?.filter(String::isNotBlank).orEmpty()

    private companion object {
        const val SEPARATOR = ","
    }
}
