package com.example.demo

import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*
import org.springframework.http.ResponseEntity

@Controller
class ProductController(
    private val productService: ProductService
) {
    @GetMapping("/")
    fun index(
        @RequestParam(name = "q", required = false, defaultValue = "") query: String,
        @RequestHeader(value = "HX-Request", required = false) isHtmx: Boolean?,
        model: Model
    ): String {
        val products = if (query.isNotBlank()) {
            productService.searchProducts(query)
        } else {
            productService.listProducts()
        }
        model.addAttribute("products", products)
        model.addAttribute("searchQuery", query)
        
        // Return only the table fragment for HTMX requests
        return if (isHtmx == true) {
            "fragments/product-table :: table"
        } else {
            "index"
        }
    }

    
    @PostMapping("/fetch")
    fun fetch(model: Model): String {
        productService.fetchAndStore()
        model.addAttribute("products", productService.listProducts())
        return "fragments/product-table"
    }

    
    @PostMapping("/products")
    fun addProduct(
        @RequestParam title: String,
        @RequestParam(required = false) vendor: String?,
        @RequestParam(required = false) handle: String?,
        model: Model
    ): String {
        val product = productService.addProduct(title, handle, vendor)
        model.addAttribute("id", product.id)
        model.addAttribute("title", product.title)
        model.addAttribute("vendor", product.vendor)
        model.addAttribute("handle", product.handle)
        return "fragments/product-row :: row"
    }

    @GetMapping("/products/{id}")
    fun productVariants(@PathVariable id: Long, model: Model): String {
        val product = productService.getProduct(id) ?: return "redirect:/"
        model.addAttribute("product", product)
        model.addAttribute("variants", productService.listVariants(id))
        return "variants"
    }

    @GetMapping("/api/products/search")
    @ResponseBody
    fun searchProducts(@RequestParam("q") query: String): List<Map<String, Any?>> {
        return productService.searchProducts(query).map { product ->
            mapOf(
                "id" to product.id,
                "title" to product.title,
                "vendor" to product.vendor,
                "handle" to product.handle
            )
        }
    }

    @GetMapping("/debug/fetch")
    fun debugFetch(): ResponseEntity<String> = try {
        val count = productService.fetchAndStore()
        ResponseEntity.ok("Fetched and upserted $count products")
    } catch (ex: Exception) {
        ResponseEntity.internalServerError().body("Fetch error: ${ex.message}")
    }
}

