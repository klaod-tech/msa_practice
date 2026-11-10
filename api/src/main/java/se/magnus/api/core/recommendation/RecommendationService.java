package se.magnus.api.core.recommendation;

import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface RecommendationService {

  Mono<Recommendation> createRecommendation(Recommendation body);  // ★ 변경 (전: @PostMapping + Recommendation 반환)

  /**
   * Sample usage: "curl $HOST:$PORT/recommendation?productId=1".
   *
   * @param productId Id of the product
   * @return the recommendations of the product
   */
  @GetMapping(
    value = "/recommendation",
    produces = "application/json")
  Flux<Recommendation> getRecommendations(  // ★ 변경 (전: List<Recommendation>)
    @RequestParam(value = "productId", required = true) int productId);

  Mono<Void> deleteRecommendations(int productId);  // ★ 변경 (전: @DeleteMapping + void)
}
