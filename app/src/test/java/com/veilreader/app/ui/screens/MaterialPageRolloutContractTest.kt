package com.veilreader.app.ui.screens

import com.veilreader.app.BuildConfig
import com.veilreader.app.ui.reader.material.MaterialPageEngineRollout
import com.veilreader.app.ui.reader.material.MaterialPagePreset
import com.veilreader.app.ui.reader.material.MaterialPageProfiles
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MaterialPageRolloutContractTest {

    @Test
    fun `debug build enables material review while release default stays off`() {
        MaterialPageEngineRollout.setDebugOverride(null)
        try {
            assertFalse(MaterialPageEngineRollout.DEFAULT_ENABLED)
            assertTrue(BuildConfig.DEBUG)
            assertTrue(MaterialPageEngineRollout.isEnabled())
            assertTrue(shouldCapturePaperTurnSnapshot(reducedMotion = true))

            MaterialPageEngineRollout.setDebugOverride(false)
            assertFalse(MaterialPageEngineRollout.isEnabled())
            assertFalse(shouldCapturePaperTurnSnapshot(reducedMotion = true))
        } finally {
            MaterialPageEngineRollout.setDebugOverride(null)
        }
    }

    @Test
    fun `review preset selection is source structured and reversible`() {
        MaterialPageEngineRollout.setDebugOverride(null)
        try {
            MaterialPageEngineRollout.setPreviewPreset(MaterialPagePreset.PAPYRUS)
            assertEquals(
                MaterialPageProfiles.Papyrus,
                MaterialPageEngineRollout.selectedProfile()
            )

            MaterialPageEngineRollout.setPreviewPreset(MaterialPagePreset.MATTE_BOOK)
            assertEquals(
                MaterialPageProfiles.MatteBook,
                MaterialPageEngineRollout.selectedProfile()
            )
        } finally {
            MaterialPageEngineRollout.setPreviewPreset(MaterialPagePreset.MATTE_BOOK)
            MaterialPageEngineRollout.setDebugOverride(null)
        }
    }
}
