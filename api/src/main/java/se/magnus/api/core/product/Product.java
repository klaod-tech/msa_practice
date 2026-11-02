package se.magnus.api.core.product;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor(force = true)

public class Product {
    private final int productId;
    private final String name;
    private final int weight;
    private final String serviceAddress;
}
