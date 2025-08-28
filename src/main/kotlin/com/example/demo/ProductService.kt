package com.example.demo

import com.example.demo.dto.ApiResponse
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.beans.factory.annotation.Value
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.URI

@Service
class ProductService(
    private val productRepository: ProductRepository,
    private val variantRepository: VariantRepository,
    @Value("\${app.productsApiUrl:https://famme.no/products.json}")
    private val productsApiUrl: String
) {
    private val log = LoggerFactory.getLogger(ProductService::class.java)
    private val mapper = jacksonObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    @Transactional
    fun fetchAndStore(apiUrl: String? = null): Int {
        // Java 11 HttpClient with redirects and timeouts
        val client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(10))
            .build()

        fun fetchJson(url: String): String {
            val req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .header("Accept", MediaType.APPLICATION_JSON_VALUE)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) SpringBoot/HTMX Demo")
                .header("Accept-Language", "en-US,en;q=0.9")
                .GET()
                .build()
            val resp = client.send(req, HttpResponse.BodyHandlers.ofString())
            val snippet = resp.body()?.take(300)
            log.info("GET {} -> {} {}, bodySnippet='{}'", url, resp.statusCode(), if (resp.statusCode() in 200..299) "OK" else "ERR", snippet)
            if (resp.statusCode() !in 200..299) {
                throw IllegalStateException("Fetch failed: HTTP ${'$'}{resp.statusCode()} for ${'$'}url")
            }
            return resp.body() ?: "{\"products\":[]}"
        }
        val primaryUrl = (apiUrl?.takeIf { it.isNotBlank() })
            ?: (productsApiUrl.takeIf { it.isNotBlank() })
            ?: "https://famme.no/products.json"
        val fallbackUrl = "https://r.jina.ai/http://famme.no/products.json"
        try {
            var body = fetchJson(primaryUrl)
            var apiResponse: ApiResponse = mapper.readValue(body)
            if (apiResponse.products.isEmpty()) {
                log.warn("Primary API returned 0 products. Retrying via mirror: {}", fallbackUrl)
                body = fetchJson(fallbackUrl)
                apiResponse = mapper.readValue(body)
            }

        var upserted = 0
        apiResponse.products.forEach { apiProduct ->
            val product = productRepository.findByExternalId(apiProduct.id)
                ?: productRepository.save(
                    Product(
                        externalId = apiProduct.id,
                        title = apiProduct.title,
                        handle = apiProduct.handle
                    )
                )
            
            var changed = false
            if (product.title != apiProduct.title) { product.title = apiProduct.title; changed = true }
            if (product.handle != apiProduct.handle) { product.handle = apiProduct.handle; changed = true }
            if (changed) productRepository.save(product)

            
            (apiProduct.variants ?: emptyList()).forEach { av ->
                val existing = variantRepository.findByExternalId(av.id)
                if (existing == null) {
                    variantRepository.save(
                        Variant(
                            externalId = av.id,
                            product = product,
                            title = av.title,
                            sku = av.sku,
                            price = av.price
                        )
                    )
                } else {
                    var vChanged = false
                    if (existing.title != av.title) { existing.title = av.title; vChanged = true }
                    if (existing.sku != av.sku) { existing.sku = av.sku; vChanged = true }
                    if (existing.price != av.price) { existing.price = av.price; vChanged = true }
                    if (existing.product.id != product.id) { existing.product = product; vChanged = true }
                    if (vChanged) variantRepository.save(existing)
                }
            }
            upserted++
        }
        log.info("Fetched and upserted {} products from {} (primary: {}, fallback: {})", upserted, if (apiUrl==primaryUrl) "primary" else "override", primaryUrl, fallbackUrl)
        return upserted
        } catch (ex: Exception) {
            log.error("Error fetching products from {}: {}", apiUrl, ex.message, ex)
            throw ex
        }
    }

    fun listProducts(): List<Product> = productRepository.findAll().sortedBy { it.id }
    
    fun searchProducts(query: String): List<Product> = 
        if (query.isBlank()) listProducts() 
        else productRepository.search(query).sortedBy { it.id }

    fun listVariants(productId: Long): List<Variant> = variantRepository.findAllByProduct_Id(productId)

    fun getProduct(productId: Long): Product? = productRepository.findById(productId).orElse(null)

    @Transactional
    fun addProduct(title: String, handle: String? = null, vendor: String? = null): Product {
        return productRepository.save(
            Product(
                title = title,
                handle = handle,
                vendor = vendor
            )
        )
    }
}
