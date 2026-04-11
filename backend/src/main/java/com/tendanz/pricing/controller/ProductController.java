package com.tendanz.pricing.controller;

import com.tendanz.pricing.entity.Product;
import com.tendanz.pricing.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;
import java.util.List;

/**
 * REST Controller for retrieving available insurance products.
 * This controller is provided — no modifications needed.
 */
@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "http://localhost:4200")
@RequiredArgsConstructor
public class ProductController {

    private final ProductRepository productRepository;

    /**
     * Get all available products.
     *
     * @return list of all products
     */
    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts() {
        return ResponseEntity.ok(productRepository.findAll());
    }
}
