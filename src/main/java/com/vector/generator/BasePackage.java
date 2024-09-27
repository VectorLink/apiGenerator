package com.vector.generator;

import org.apache.commons.lang3.StringUtils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Builder
@AllArgsConstructor
@Data
public class BasePackage {
    private String corePath;
    private String enumPath;

    public String getEnumPath() {
        return StringUtils.isEmpty(this.enumPath) ? this.corePath : this.enumPath;
    }

}
