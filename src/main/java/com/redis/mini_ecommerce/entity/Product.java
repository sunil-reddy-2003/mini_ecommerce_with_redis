package com.redis.mini_ecommerce.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;


@Entity
@Table(name = "products")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Product implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "product_id",nullable = false,unique = true)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long productId;

    @Column(name = "product_name",unique = true,nullable = false)
    private String productName;

    @Column(name = "quantity",nullable = false,columnDefinition = "BIGINT CHECK (quantity>=0)")
    private long quantity;

    @Column(name = "price",nullable = false)
    private double price;

}
