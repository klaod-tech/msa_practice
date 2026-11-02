package se.magnus.api.composite.product;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor(force = true)

public class ServiceAddresses {
  private final String cmp;
  private final String pro;
  private final String rev;
  private final String rec;
}
