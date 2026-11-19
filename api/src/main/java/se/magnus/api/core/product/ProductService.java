package se.magnus.api.core.product;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;  // ★ 추가
import reactor.core.publisher.Mono;

public interface ProductService {

  Mono<Product> createProduct(Product body);

  /**
   * Sample usage: "curl $HOST:$PORT/product/1".
   *
   * @param productId Id of the product
   * @return the product, if found, else null
   */
  @GetMapping(
    value = "/product/{productId}",
    produces = "application/json")
  Mono<Product> getProduct(  // ★ 변경 (전: getProduct(@PathVariable int productId))
    @PathVariable int productId,
    @RequestParam(value = "delay", required = false, defaultValue = "0") int delay,  // ★ 추가
    @RequestParam(value = "faultPercent", required = false, defaultValue = "0") int faultPercent  // ★ 추가
  );

  Mono<Void> deleteProduct(int productId);
}