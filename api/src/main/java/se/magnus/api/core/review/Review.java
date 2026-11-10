package se.magnus.api.core.review;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;  // ★ 추가

@Getter
@Setter  // ★ 추가 (교재: setter 메서드를 직접 작성)
@AllArgsConstructor 
@NoArgsConstructor(force = true)

public class Review {
  private int productId;  // ★ 변경: final 삭제
  private int reviewId;  // ★ 변경: final 삭제
  private String author;  // ★ 변경: final 삭제
  private String subject;  // ★ 변경: final 삭제
  private String content;  // ★ 변경: final 삭제
  private String serviceAddress;  // ★ 변경: final 삭제
}
