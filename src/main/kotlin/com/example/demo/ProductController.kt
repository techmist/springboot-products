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
    fun index(model: Model): String {
        model.addAttribute("products", productService.listProducts())
        return "index"
    }

    
    @PostMapping("/fetch")
    fun fetch(model: Model): String {
        productService.fetchAndStore()
        model.addAttribute("products", productService.listProducts())
       return "fragments/product-table :: table"
    }

    
    @PostMapping("/products")
    fun addProduct(
        @RequestParam title: String,
        @RequestParam(required = false) handle: String?,
        model: Model
    ): String {
        val product = productService.addProduct(title, handle)
        model.addAttribute("p", product)
        return "fragments/product-row :: row"
    }

    @GetMapping("/products/{id}")
    fun productVariants(@PathVariable id: Long, model: Model): String {
        val product = productService.getProduct(id) ?: return "redirect:/"
        model.addAttribute("product", product)
        model.addAttribute("variants", productService.listVariants(id))
        return "variants"
    }

    @GetMapping("/debug/fetch")
    fun debugFetch(): ResponseEntity<String> = try {
        val count = productService.fetchAndStore()
        ResponseEntity.ok("Fetched and upserted $count products")
    } catch (ex: Exception) {
        ResponseEntity.internalServerError().body("Fetch error: ${ex.message}")
    }
}

