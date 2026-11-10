package se.magnus.microservices.core.product.persistence;

import static java.lang.String.format;  // ★ 추가

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "products")
@Getter
@Setter
@NoArgsConstructor
public class ProductEntity {

  @Id private String id;

  @Version private Integer version;

  @Indexed(unique = true)
  private int productId;

  private String name;
  private int weight;

  public ProductEntity(int productId, String name, int weight) {
    this.productId = productId;
    this.name = name;
    this.weight = weight;
  }

  // ★ 추가 (로그에 엔티티를 짧게 찍기 위해)
  @Override
  public String toString() {
    return format("ProductEntity: %s", productId);
  }
}
