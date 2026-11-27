package se.magnus.api.composite.product;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter                               // 모든 필드의 게터
@AllArgsConstructor                   // 필드 순서대로 받는 생성자
@NoArgsConstructor(force = true)      // 기본 생성자 (final 필드를 0/null로 채움)
public class ServiceAddresses {
    private final String cmp;
    private final String pro;
    private final String rev;
    private final String rec;
}
