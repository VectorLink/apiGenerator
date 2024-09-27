package com.vector.generator.support;

import java.io.BufferedWriter;
import java.io.IOException;
import com.vector.enums.Language;
import org.apache.commons.lang3.StringUtils;
import lombok.Data;

@Data
public class JavaField extends AbstractJavaGenerator {
    private String fieldName;
    private String fieldType;
    private String messageType;
    private boolean repeated;
    private boolean mapField;
    private String comment;
    private Integer enumNumber;

    public JavaField(Language language, String fieldName, String comment, String fieldType, Integer enumNumber) {
        super(language);
        this.fieldName = fieldName;
        this.comment = comment;
        this.fieldType = fieldType;
        this.enumNumber = enumNumber;
    }

    public boolean isPrimitive() {
        return !this.fieldType.equals("MESSAGE") && !this.fieldType.equals("ENUM");
    }

    public boolean isEnum() {
        return this.fieldType.equals("ENUM");
    }

    public String getJavaType() {
        if (!this.isPrimitive()) {
            return this.messageType;
        } else if (this.fieldType.equals("INT")) {
            return "Integer";
        } else {
            return this.fieldType.equals("BYTE_STRING") ? "byte[]" : this.fieldType.substring(0, 1) + this.fieldType.substring(1).toLowerCase();
        }
    }

    @Override
    public void write(BufferedWriter writer) throws IOException {
        if ("".equals(this.fieldType)) {
            writer.write(this.getFieldName() + "(" + this.getEnumNumber() + ")");
        } else {
            String javaType = this.getJavaType();
            if (this.mapField) {
                javaType = "java.util.Map<" + javaType + ">";
            } else if (this.repeated) {
                javaType = "java.util.List<" + javaType + ">";
            }

            if (StringUtils.isNotEmpty(this.comment)) {
                writer.write("  @ApiModelProperty(\"" + this.comment + "\")\n");
            }

            writer.write("  private " + javaType + " " + this.getFieldName() + ";\n");
        }

    }
}
