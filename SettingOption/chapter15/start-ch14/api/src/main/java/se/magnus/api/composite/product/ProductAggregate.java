package se.magnus.api.composite.product;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter                               // 모든 필드의 게터
@AllArgsConstructor                   // 필드 순서대로 받는 생성자
@NoArgsConstructor(force = true)      // 기본 생성자 (final 필드를 0/null로 채움)
public class ProductAggregate {
    private final int productId;
    private final String name;
    private final int weight;
    private final List<RecommendationSummary> recommendations;
    private final List<ReviewSummary> reviews;
    private final ServiceAddresses serviceAddresses;
}
