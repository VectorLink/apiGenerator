package com.vector.generator.support;

import java.io.BufferedWriter;
import java.io.IOException;
import com.vector.enums.Language;
import org.apache.commons.lang3.StringUtils;
import lombok.Data;

@Data
public class JavaPackage extends AbstractJavaGenerator {
    private String packageName;

    public JavaPackage(Language language, String packageName) {
        super(language);
        this.packageName = packageName;
    }

    @Override
    public void write(BufferedWriter writer) throws IOException {
        if (StringUtils.isNotEmpty(this.packageName)) {
            writer.write("package " + this.packageName + ";\n\n");
        }

    }
}
