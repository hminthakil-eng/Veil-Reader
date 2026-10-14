package com.veilreader.app.data

import android.content.Context
import com.veilreader.app.domain.RagsToRichesAvatar

/**
 * Versioned built-in cosmetic identity, stored alongside the existing game counters.
 * LibraryExport already backs up and transactionally restores veil_game_v1.
 * Legacy pilot preferences migrate only if the new key has not been saved/restored.
 */
class AvatarAppearanceStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("veil_game_v1", Context.MODE_PRIVATE)
    private val legacy = appContext.getSharedPreferences("veil_avatar_pilot_v1", Context.MODE_PRIVATE)

    init {
        if (!prefs.contains(KEY_SCHEMA) && legacy.contains("schemaVersion")) {
            val old = RagsToRichesAvatar(
                schemaVersion = legacy.getInt("schemaVersion", 1),
                hairStyle = legacy.getInt("hairStyle", 0),
                hairColor = legacy.getInt("hairColor", 0),
                skinTone = legacy.getInt("skinTone", 1),
                outfitStyle = legacy.getInt("outfitStyle", 0),
                accentColor = legacy.getInt("accentColor", 0)
            ).safeForPreview()
            // Leave the legacy file intact. Failed commits can be retried on the next launch.
            save(old)
        }
    }

    fun read(): RagsToRichesAvatar = RagsToRichesAvatar(
        schemaVersion = prefs.getInt(KEY_SCHEMA, RagsToRichesAvatar.CURRENT_SCHEMA),
        hairStyle = prefs.getInt("avatarV1.hairStyle", 0),
        hairColor = prefs.getInt("avatarV1.hairColor", 0),
        skinTone = prefs.getInt("avatarV1.skinTone", 1),
        outfitStyle = prefs.getInt("avatarV1.outfitStyle", 0),
        accentColor = prefs.getInt("avatarV1.accentColor", 0)
    ).safeForPreview()

    /** Call only after explicit user Save. Never claim success on a failed disk write. */
    fun save(avatar: RagsToRichesAvatar): Boolean {
        val safe = avatar.safeForPreview()
        return prefs.commitDurably {
            putInt(KEY_SCHEMA, safe.schemaVersion)
            putInt("avatarV1.hairStyle", safe.hairStyle)
            putInt("avatarV1.hairColor", safe.hairColor)
            putInt("avatarV1.skinTone", safe.skinTone)
            putInt("avatarV1.outfitStyle", safe.outfitStyle)
            putInt("avatarV1.accentColor", safe.accentColor)
        }
    }

    private companion object { const val KEY_SCHEMA = "avatarV1.schemaVersion" }
}
