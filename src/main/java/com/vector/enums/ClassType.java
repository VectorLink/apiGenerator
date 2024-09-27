package com.vector.enums;

import lombok.AllArgsConstructor;

/**
 * 类型
 */
@AllArgsConstructor
public enum ClassType {
    PARAM,
    MODEL;
    public String toLowerString() {
        return this.toString().toLowerCase();
    }

    public String toClassString() {
        return this.toString().substring(0, 1).toUpperCase() + this.toString().substring(1).toLowerCase();
    }
}
