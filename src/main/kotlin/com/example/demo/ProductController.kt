
package com.example.demo

import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*
import org.springframework.http.ResponseEntity
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory

@Controller
class ProductController(
    private val productService: ProductService
) {
    private val log = LoggerFactory.getLogger(ProductController::class.java)
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
    
    // In-memory storage for deleted products (for demo purposes only)
    private val deletedProducts = mutableMapOf<Long, Product>()
    
    @DeleteMapping("/products/{id}")
    @ResponseBody
    fun deleteProduct(
        @PathVariable id: Long,
        @RequestHeader(value = "HX-Request", required = false) isHtmx: Boolean?,
        response: HttpServletResponse
    ): ResponseEntity<String> {
        return try {
            val product = productService.getProduct(id)
                ?: throw NoSuchElementException("Product not found with id: $id")
                
            // Store the product before deletion (for undo)
            deletedProducts[id] = product
            
            productService.deleteProduct(id)
            
            if (isHtmx == true) {
                // Return empty response with 200 OK for HTMX
                response.addHeader("HX-Trigger", "productDeleted")
                response.addHeader("HX-Reswap", "delete")
                response.addHeader("HX-Trigger-After-Swap", "recountProducts")
                ResponseEntity.ok().body("")
            } else {
                ResponseEntity.ok("Product deleted successfully")
            }
        } catch (ex: Exception) {
            log.error("Error deleting product $id", ex)
            if (isHtmx == true) {
                response.addHeader("HX-Retarget", "#error-message")
                response.addHeader("HX-Reswap", "innerHTML")
                ResponseEntity.badRequest().body("Error deleting product: ${ex.message}")
            } else {
                ResponseEntity.badRequest().body("Error deleting product: ${ex.message}")
            }
        }
    }
    
    @PostMapping("/api/products/{id}/restore")
    @ResponseBody
    fun restoreProduct(
        @PathVariable id: Long,
        @RequestHeader(value = "HX-Request", required = false) isHtmx: Boolean?,
        response: HttpServletResponse
    ): ResponseEntity<String> {
        return try {
            val product = deletedProducts[id] ?: 
                throw NoSuchElementException("No deleted product found with id: $id")
                
            // Restore the product
            productService.addProduct(
                title = product.title,
                handle = product.handle,
                vendor = product.vendor
            )
            
            // Remove from deleted products cache
            deletedProducts.remove(id)
            
            if (isHtmx == true) {
                // Return success response for HTMX
                response.addHeader("HX-Refresh", "true")
                ResponseEntity.ok().body("")
            } else {
                ResponseEntity.ok("Product restored successfully")
            }
        } catch (ex: Exception) {
            log.error("Error restoring product $id", ex)
            if (isHtmx == true) {
                response.addHeader("HX-Retarget", "#error-message")
                response.addHeader("HX-Reswap", "innerHTML")
            }
            ResponseEntity.badRequest().body("Error restoring product: ${ex.message}")
        }
    }
}

