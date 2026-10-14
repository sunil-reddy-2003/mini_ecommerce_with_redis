package com.redis.mini_ecommerce;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class MiniEcommerceApplication {

	public static void main(String[] args) {
		SpringApplication.run(MiniEcommerceApplication.class, args);
	}

}
