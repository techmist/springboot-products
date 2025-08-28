package com.example.demo

import jakarta.persistence.*

@Entity
@Table(name = "variants")
data class Variant(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "external_id", unique = true)
    var externalId: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    var product: Product,

    @Column(nullable = false)
    var title: String,

    var sku: String? = null,

    var price: String? = null
)
