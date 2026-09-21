package com.veilreader.app.manga.reader.image

data class MangaImageDeliveryPlan(
    val page: MangaResolvedPage,
    val dimensions: MangaImageDimensions?,
    val decision: MangaImageStrategyDecision
)

class MangaImageDeliveryPlanner(
    private val dimensionProbe: MangaImageDimensionProbe,
    private val strategy: MangaExtremeImageStrategy = MangaExtremeImageStrategy()
) {
    suspend fun plan(page: MangaResolvedPage): MangaImageDeliveryPlan {
        val dimensions = dimensionProbe.probe(page)
        return MangaImageDeliveryPlan(
            page = page,
            dimensions = dimensions,
            decision = strategy.decide(page, dimensions)
        )
    }
}
