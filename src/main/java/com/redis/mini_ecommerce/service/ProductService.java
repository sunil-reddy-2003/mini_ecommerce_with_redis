package com.redis.mini_ecommerce.service;

import com.redis.mini_ecommerce.dto.ProductInfo;
import com.redis.mini_ecommerce.dto.ProductResponse;
import com.redis.mini_ecommerce.entity.Product;
import com.redis.mini_ecommerce.repo.ProductRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class ProductService {

    ProductRepo productRepo;

    @Autowired
    public ProductService(ProductRepo productRepo) {
        this.productRepo = productRepo;
    }

    @Cacheable(value = "products",key="#productId")
    @Transactional(readOnly = true)
    public ProductResponse getProduct(long productId){
        System.out.println("Fetching product from PostgreSQL...");
        Product product=productRepo.getReferenceById(productId);
        return new ProductResponse(
                product.getProductId(),
                product.getProductName(),
                product.getQuantity(),
                product.getPrice());
    }

    @CachePut(value = "products",key = "#productId")
    @Transactional
    public Product updateProduct(long productId, ProductInfo updateProduct) {
        log.info("update product: "+updateProduct);
        Product existingProduct=productRepo.getById(productId);
        existingProduct.setPrice(updateProduct.getPrice());
        existingProduct.setProductName(updateProduct.getProductName());
        existingProduct.setQuantity(updateProduct.getQuantity());
        Product product=productRepo.save(existingProduct);
        return product;
    }

    @Transactional
    @CacheEvict(value = "products",key = "#productId")
    public String deleteProduct(long productId) {
        productRepo.deleteById(productId);
         return "product deleted successfully with Id: "+productId;
    }
}
