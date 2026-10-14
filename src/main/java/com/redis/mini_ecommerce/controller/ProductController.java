package com.redis.mini_ecommerce.controller;

import com.redis.mini_ecommerce.dto.ProductInfo;
import com.redis.mini_ecommerce.dto.ProductResponse;
import com.redis.mini_ecommerce.entity.Product;
import com.redis.mini_ecommerce.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/product")
public class ProductController {

    ProductService productService;

    @Autowired
    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/getproduct/{productId}")
    public ResponseEntity<?> getProduct(@PathVariable long productId){
        return new ResponseEntity<>(productService.getProduct(productId), HttpStatus.OK);
    }

    @PutMapping("/updateproduct/{productId}")
    public ResponseEntity<?> updateProduct(@PathVariable long productId,@RequestBody ProductInfo product){
        productService.updateProduct(productId,product);
        return new ResponseEntity<>("product updated successfully", HttpStatus.CREATED);
    }

    @DeleteMapping("/deleteproduct/{productId}")
    public ResponseEntity<?> deleteProduct(@PathVariable long productId){
        return new ResponseEntity<>(productService.deleteProduct(productId),HttpStatus.OK);
    }
}
