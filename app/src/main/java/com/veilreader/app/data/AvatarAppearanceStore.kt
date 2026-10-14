package com.veilreader.app.data

import android.content.Context
import com.veilreader.app.domain.RagsToRichesAvatar

/**
 * Experiment-only separate cosmetic save. The user's books and GameRepository counters are not
 * touched. Before shipping, this schema must join app backup/restore and migrations.
 */
class AvatarAppearanceStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("veil_avatar_pilot_v1", Context.MODE_PRIVATE)

    fun read(): RagsToRichesAvatar = RagsToRichesAvatar(
        schemaVersion = prefs.getInt("schemaVersion", RagsToRichesAvatar.CURRENT_SCHEMA),
        hairStyle = prefs.getInt("hairStyle", 0),
        hairColor = prefs.getInt("hairColor", 0),
        skinTone = prefs.getInt("skinTone", 1),
        outfitStyle = prefs.getInt("outfitStyle", 0),
        accentColor = prefs.getInt("accentColor", 0)
    ).safeForPreview()

    /** Call only for explicit Save action. false means the UI must NOT claim success. */
    fun save(avatar: RagsToRichesAvatar): Boolean {
        val safe = avatar.safeForPreview()
        return prefs.edit()
            .putInt("schemaVersion", safe.schemaVersion)
            .putInt("hairStyle", safe.hairStyle)
            .putInt("hairColor", safe.hairColor)
            .putInt("skinTone", safe.skinTone)
            .putInt("outfitStyle", safe.outfitStyle)
            .putInt("accentColor", safe.accentColor)
            .commit()
    }
}
