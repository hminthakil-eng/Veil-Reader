package com.veilreader.app.data.db

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class BookWithCollections(
    @Embedded val book: BookEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = BookCollectionCrossRef::class,
            parentColumn = "bookId",
            entityColumn = "collectionId"
        )
    )
    val collections: List<CollectionEntity>
)
