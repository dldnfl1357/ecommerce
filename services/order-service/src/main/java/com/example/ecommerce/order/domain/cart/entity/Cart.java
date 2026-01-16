package com.example.ecommerce.order.domain.cart.entity;

import com.example.ecommerce.common.core.entity.BaseEntity;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("carts")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Cart extends BaseEntity {

    @Id
    private Long id;

    @Column("member_id")
    private Long memberId;

    public static Cart create(Long memberId) {
        return Cart.builder()
                .memberId(memberId)
                .build();
    }
}
