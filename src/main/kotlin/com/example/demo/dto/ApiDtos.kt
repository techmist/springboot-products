package com.example.demo.dto

data class ApiResponse(
    val products: List<ApiProduct> = emptyList()
)

data class ApiProduct(
    val id: Long,
    val title: String,
    val handle: String? = null,
    val variants: List<ApiVariant> = emptyList()
)

data class ApiVariant(
    val id: Long,
    val title: String,
    val sku: String? = null,
    val price: String? = null
)
