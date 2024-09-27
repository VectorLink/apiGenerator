package com.vector.enums;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum Language {
    SPRING,
    GATEWAY;

    public static final String DEFAULT_VALUE = "SPRING";
}
