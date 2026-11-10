package se.magnus.microservices.core.product.persistence;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

// ★ 변경 (전: PagingAndSortingRepository + CrudRepository)
public interface ProductRepository extends ReactiveCrudRepository<ProductEntity, String> {
  Mono<ProductEntity> findByProductId(int productId);  // ★ 변경 (전: Optional<ProductEntity>)
}
