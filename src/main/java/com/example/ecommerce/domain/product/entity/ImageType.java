package com.example.ecommerce.domain.product.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ImageType {

    MAIN("메인 이미지"),
    DETAIL("상세 이미지");

    private final String displayName;

    /**
     * 메인 이미지 여부
     */
    public boolean isMain() {
        return this == MAIN;
    }

    /**
     * 상세 이미지 여부
     */
    public boolean isDetail() {
        return this == DETAIL;
    }
}
