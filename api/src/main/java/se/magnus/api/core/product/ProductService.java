package se.magnus.api.core.product;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import reactor.core.publisher.Mono;

public interface ProductService {

  Mono<Product> createProduct(Product body);  // ★ 변경 (전: @PostMapping + Product 반환 → 이벤트로 호출, HTTP 매핑 삭제)

  /**
   * Sample usage: "curl $HOST:$PORT/product/1".
   *
   * @param productId Id of the product
   * @return the product, if found, else null
   */
  @GetMapping(
    value = "/product/{productId}",
    produces = "application/json")
  Mono<Product> getProduct(@PathVariable int productId);  // ★ 변경 (전: Product)

  Mono<Void> deleteProduct(int productId);  // ★ 변경 (전: @DeleteMapping + void → 이벤트로 호출)
}
