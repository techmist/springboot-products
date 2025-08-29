package com.example.demo

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface VariantRepository : JpaRepository<Variant, Long> {
    fun findByExternalId(externalId: Long): Variant?
    fun findAllByProduct_Id(productId: Long): List<Variant>
    
    @Modifying
    @Query("DELETE FROM Variant v WHERE v.product.id = :productId")
    fun deleteAllByProductId(@Param("productId") productId: Long)
}
