package se.magnus.api.core.recommendation;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;  // ★ 추가

@Getter
@Setter  // ★ 추가 (교재: setter 메서드를 직접 작성)
@AllArgsConstructor 
@NoArgsConstructor(force = true)

public class Recommendation {
  private int productId;  // ★ 변경: final 삭제
  private int recommendationId;  // ★ 변경: final 삭제
  private String author;  // ★ 변경: final 삭제
  private int rate;  // ★ 변경: final 삭제
  private String content;  // ★ 변경: final 삭제
  private String serviceAddress;  // ★ 변경: final 삭제
}
