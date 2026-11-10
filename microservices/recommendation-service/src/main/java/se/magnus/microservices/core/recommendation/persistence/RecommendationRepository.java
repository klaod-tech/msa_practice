package se.magnus.microservices.core.recommendation.persistence;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

// ★ 변경 (전: CrudRepository)
public interface RecommendationRepository extends ReactiveCrudRepository<RecommendationEntity, String> {
  Flux<RecommendationEntity> findByProductId(int productId);  // ★ 변경 (전: List<RecommendationEntity>)
}
