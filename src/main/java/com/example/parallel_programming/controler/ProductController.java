package com.example.parallel_programming.controler;


import com.example.parallel_programming.entity.Product;
import com.example.parallel_programming.services.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.Future;

@RestController
@RequestMapping("api/product")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping("/add_product")
    public ResponseEntity<Product> addProduct(@RequestBody Product product) {
        Product savedProduct = productService.addProduct(product);
        return ResponseEntity.ok(savedProduct);
    }

    @GetMapping("/get_products")
    public ResponseEntity<List<Product>> getAllProducts() {
        List<Product> products = productService.getAllProducts();
        return ResponseEntity.ok(products);
    }

    @GetMapping("/get_by_id/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable int id) {
        return productService.getProductById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/delete_by_id/{id}")
    public ResponseEntity<Void> deleteProductById(@PathVariable int id){
        productService.deleteProductById(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/update_Product")
    public ResponseEntity<Product> updateProduct (@RequestBody Product product){
        Product updatedProduct = productService.updateProduct(product);
        return ResponseEntity.ok(updatedProduct);
    }

    @PatchMapping("/decrease_quantity/{id}")
    public ResponseEntity<Product> decreaseQuantity (@PathVariable int id, @RequestParam Integer quantity){
        Product newProduct = productService.decreaseProductQuantity(id, quantity);
        return ResponseEntity.ok(newProduct);
    }

    @PatchMapping("/decrease_quantity_async/{id}")
    public ResponseEntity<String> decreaseAsync(
            @PathVariable Long id,
            @RequestParam int amount) {

        productService.decreaseStockAsync(id, amount);

        return ResponseEntity.ok("Request submitted");
    }
}
