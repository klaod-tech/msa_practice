package se.magnus.microservices.core.review.persistence;

import static java.lang.String.format;  // ★ 추가

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "reviews", indexes = { @Index(name = "reviews_unique_idx", unique = true, columnList = "productId,reviewId") })
@Getter
@Setter
@NoArgsConstructor
public class ReviewEntity {

  @Id @GeneratedValue
  private int id;

  @Version
  private int version;

  private int productId;
  private int reviewId;
  private String author;
  private String subject;
  private String content;

  public ReviewEntity(int productId, int reviewId, String author, String subject, String content) {
    this.productId = productId;
    this.reviewId = reviewId;
    this.author = author;
    this.subject = subject;
    this.content = content;
  }

  // ★ 추가 (로그에 엔티티를 짧게 찍기 위해)
  @Override
  public String toString() {
    return format("ReviewEntity: %s/%d", productId, reviewId);
  }
}
