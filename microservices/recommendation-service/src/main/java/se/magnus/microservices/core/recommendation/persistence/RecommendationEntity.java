package se.magnus.microservices.core.recommendation.persistence;

import static java.lang.String.format;  // ★ 추가

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "recommendations")
@CompoundIndex(name = "prod-rec-id", unique = true, def = "{'productId': 1, 'recommendationId' : 1}")
@Getter
@Setter
@NoArgsConstructor
public class RecommendationEntity {

  @Id
  private String id;

  @Version
  private Integer version;

  private int productId;
  private int recommendationId;
  private String author;
  private int rating;
  private String content;

  public RecommendationEntity(int productId, int recommendationId, String author, int rating, String content) {
    this.productId = productId;
    this.recommendationId = recommendationId;
    this.author = author;
    this.rating = rating;
    this.content = content;
  }

  // ★ 추가 (로그에 엔티티를 짧게 찍기 위해)
  @Override
  public String toString() {
    return format("RecommendationEntity: %s/%d", productId, recommendationId);
  }
}
