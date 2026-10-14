package com.veilreader.app.game.island

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class IslandShopSnapshotCodecTest {
    @Test fun allFieldsAndActionIdsSurviveRoundTrip() {
        val first = IslandShopState()
        val loaned = reduceShop(first, ShopCommand.Lend("loan-1", "initial:fog_tales", "1-a")).state
        val ordered = reduceShop(loaned, ShopCommand.Order("order-2", "garden")).state
        val restored = IslandShopSnapshotCodec.decode(IslandShopSnapshotCodec.encode(ordered))
        assertEquals(ordered, restored)
        assertEquals(ShopOutcome.DUPLICATE,
            reduceShop(restored, ShopCommand.Order("order-2", "garden")).outcome)
    }

    @Test fun invalidSchemaFailsClosed() {
        val json = IslandShopSnapshotCodec.encode(IslandShopState())
        val future = json.replace("\"schemaVersion\":1", "\"schemaVersion\":999")
        try {
            IslandShopSnapshotCodec.decode(future)
            org.junit.Assert.fail("future schema silently accepted")
        } catch (_: IllegalStateException) { }
        assertFalse(future == json)
    }
}
