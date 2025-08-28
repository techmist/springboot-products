package com.example.demo

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface ProductRepository : JpaRepository<Product, Long> {
    fun findByExternalId(externalId: Long): Product?
    
    @Query("""
        SELECT p FROM Product p 
        WHERE LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%'))
           OR LOWER(p.vendor) LIKE LOWER(CONCAT('%', :query, '%'))
    """)
    fun search(@Param("query") query: String): List<Product>
}
