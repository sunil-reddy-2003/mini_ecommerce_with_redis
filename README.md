# Redis Caching with Spring Boot

A learning project showing how to use Redis as a cache in a Spring Boot app backed by PostgreSQL.

## What It Demonstrates

- Connecting Spring Boot to Redis
- Caching responses with `@Cacheable`
- Refreshing cached data with `@CachePut`
- Removing stale data with `@CacheEvict`
- Configuring cache keys, JSON serialization, and TTL
- Verifying cached entries with `redis-cli`

## How It Works

PostgreSQL is the source of truth. Redis only stores temporary copies of frequently read data.

For `GET /product/getproduct/21`:

1. Spring checks Redis for the key.
2. On a **miss**, the service fetches product 21 from PostgreSQL, returns a `ProductResponse`, and Spring stores it in Redis.
3. On a **hit**, Spring returns the cached value without running the method.

With cache name `products` and ID `21`, the default key is:

```
products::21
```

The cache name is just a logical name chosen by the app. It does not need to match a table or entity.

## Cache Annotations

### `@Cacheable`: read from cache, cache on a miss

```java
@Cacheable(value = "products", key = "#productId")
@Transactional(readOnly = true)
public ProductResponse getProduct(long productId) {
    Product product = productRepo.findById(productId)
            .orElseThrow(() -> new RuntimeException("Product not found"));

    return new ProductResponse(
            product.getProductId(),
            product.getProductName(),
            product.getQuantity(),
            product.getPrice()
    );
}
```

It caches the method's **return value** (one product), not the whole table.

### `@CachePut`: always run the method, then update the cache

```java
@CachePut(value = "products", key = "#productId")
public ProductResponse updateProduct(long productId, ProductRequest request) {
    Product product = productRepo.findById(productId)
            .orElseThrow(() -> new RuntimeException("Product not found"));

    product.setProductName(request.getProductName());
    product.setQuantity(request.getQuantity());
    product.setPrice(request.getPrice());

    Product updated = productRepo.save(product);

    return new ProductResponse(
            updated.getProductId(),
            updated.getProductName(),
            updated.getQuantity(),
            updated.getPrice()
    );
}
```

> **Important:** `@CachePut` caches whatever the method returns. If it returns `"Product updated successfully"`, that string is what gets cached.

To send a success message to the client while caching the real response, let the service return `ProductResponse` and let the controller send the message:

```java
@PutMapping("/{productId}")
public ResponseEntity<String> updateProduct(
        @PathVariable long productId,
        @RequestBody ProductRequest request) {

    productService.updateProduct(productId, request);
    return ResponseEntity.ok("Product updated successfully");
}
```

### `@CacheEvict`: remove stale data

Use it when the method returns only a message, or when deleting.

```java
@CacheEvict(value = "products", key = "#productId")
public String updateProduct(long productId, ProductRequest request) {
    // update product in PostgreSQL
    return "Product updated successfully";
}

@CacheEvict(value = "products", key = "#productId")
public void deleteProduct(long productId) {
    // delete product from PostgreSQL
}
```

The next `GET` misses the cache, reads fresh data from PostgreSQL, and `@Cacheable` repopulates it.

To clear the entire cache (use sparingly):

```java
@CacheEvict(value = "products", allEntries = true)
```

By default, eviction happens only after the method completes successfully.

## Choosing the Right Annotation

| Requirement | Annotation | Result |
|---|---|---|
| Fetch a product and reuse results | `@Cacheable` | Hit returns cached value; miss runs the method and caches it |
| Update a product and refresh cache | `@CachePut` | Stores the returned value immediately |
| Update a product, return only a message | `@CacheEvict` | Removes the old entry; next GET repopulates |
| Delete a product | `@CacheEvict` | Removes that product's entry |

## Redis Configuration

```java
@Configuration
public class RedisConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {

        RedisCacheConfiguration configuration =
                RedisCacheConfiguration.defaultCacheConfig()
                        .entryTtl(Duration.ofMinutes(10))
                        .serializeKeysWith(
                                RedisSerializationContext.SerializationPair
                                        .fromSerializer(new StringRedisSerializer()))
                        .serializeValuesWith(
                                RedisSerializationContext.SerializationPair
                                        .fromSerializer(
                                                new JacksonJsonRedisSerializer<>(
                                                        ProductResponse.class)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(configuration)
                .build();
    }
}
```

- **TTL:** entries expire after 10 minutes.
- **Key serializer:** keys are stored as plain strings.
- **Value serializer:** `ProductResponse` is stored as JSON. Since it is typed to `ProductResponse`, use this cache only for that type, or configure serializers per cache.

## `application.yml`

Adjust the database credentials for your local setup.

```yaml
spring:
  application:
    name: mini-ecommerce

  datasource:
    url: jdbc:postgresql://localhost:5432/ecommerce-redis
    username: postgres
    password: ${DB_PASSWORD}

  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true

  data:
    redis:
      host: localhost
      port: 6379

  cache:
    type: redis
    cache-names: products
    redis:
      time-to-live: 10m
      cache-null-values: false
```

If the `RedisCacheManager` bean sets the TTL and serializers, treat the Java config as authoritative and keep the YAML aligned with it.

## Running Redis with Docker

```bash
# Start a new container
docker run -d --name redis-learning -p 6379:6379 redis

# Or start an existing one
docker start redis-learning

# Check it responds (expected output: PONG)
docker exec -it redis-learning redis-cli ping
```

## Inspecting the Cache

```bash
docker exec -it redis-learning redis-cli
```

```
KEYS *
GET "products::21"
TTL "products::21"
```

TTL results:

- Positive number: seconds left before expiry
- `-1`: key exists with no expiry
- `-2`: key does not exist

> `KEYS *` is fine for local learning, but avoid it on large production databases because it can block Redis. Use `SCAN` instead.

## Testing the Cache Behavior

1. Call the GET endpoint for a product.
2. Check Redis and confirm a key like `products::21` exists.
3. Call GET again and confirm the DB-fetch log does **not** appear (cache hit).
4. Update the product using `@CachePut` or `@CacheEvict`.
5. Inspect Redis:
   - `@CachePut`: the key holds the updated response.
   - `@CacheEvict`: the key is gone until the next GET.
6. Delete the product and confirm its key is removed.

## Practical Notes

- PostgreSQL is the source of truth; Redis is only a cache.
- Caching works through Spring's proxy. Calling an annotated method from another method in the **same class** can bypass the cache.
- PostgreSQL and Redis updates are not one distributed transaction, so concurrent requests or cache failures can briefly leave stale data. Production systems may need extra consistency safeguards.
- Put `@CachePut` and `@CacheEvict` on **service** methods, not chosen based on whether the controller uses GET, PUT, or DELETE.
