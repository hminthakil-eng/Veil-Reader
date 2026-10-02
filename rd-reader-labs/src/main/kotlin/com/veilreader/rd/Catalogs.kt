package com.veilreader.rd

data class CatalogBook(
    val id: String,
    val title: String,
    val author: String? = null,
    val acquisitionHref: String? = null,
    val coverHref: String? = null
)

data class CatalogPage(
    val books: List<CatalogBook>,
    val nextHref: String? = null
)

interface CatalogProviderContract {
    val id: String
    val requiresNetwork: Boolean
    fun accepts(uri: String): Boolean
}
