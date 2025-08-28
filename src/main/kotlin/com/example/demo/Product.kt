package com.example.demo

import jakarta.persistence.*

@Entity
@Table(name = "products")
data class Product(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "external_id", unique = true)
    var externalId: Long? = null,

    @Column(nullable = false)
    var title: String,

    var handle: String? = null,
    
    var vendor: String? = null
)
