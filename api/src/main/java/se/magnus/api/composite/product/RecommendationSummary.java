package se.magnus.api.composite.product;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor(force = true)

public class RecommendationSummary {

  private final int recommendationId;
  private final String author;
  private final int rate;
}
