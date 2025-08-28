package com.example.demo

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface VariantRepository : JpaRepository<Variant, Long> {
    fun findByExternalId(externalId: Long): Variant?
    fun findAllByProduct_Id(productId: Long): List<Variant>
}
