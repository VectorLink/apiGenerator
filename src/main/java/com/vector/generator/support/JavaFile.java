package com.vector.generator.support;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import com.vector.enums.Language;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JavaFile extends AbstractJavaGenerator {
    private String filePath;
    private JavaPackage javaPackage;
    private JavaImport javaImport;
    private JavaService javaService;
    private JavaClass javaClass;

    public JavaFile(Language language, String filePath) {
        super(language);
        this.filePath = filePath;
    }

    public void setBaseImports(List<String> baseImports) {
        if (this.javaImport == null) {
            this.javaImport = new JavaImport(this.language);
        }

        this.javaImport.setBaseImports(baseImports);
    }

    public void addImport(String s) {
        if (this.javaImport == null) {
            this.javaImport = new JavaImport(this.language);
        }

        this.javaImport.addImport(s);
    }

    public void write() {
        try {
            this.doWrite();
        } catch (IOException var2) {
            throw new RuntimeException(var2);
        }
    }

    @Override
    public void write(BufferedWriter writer) throws IOException {
        if (this.javaPackage != null) {
            this.javaPackage.write(writer);
        }

        if (this.javaImport != null) {
            this.javaImport.write(writer);
        }

        if (this.javaService != null) {
            this.javaService.write(writer);
        }

        if (this.javaClass != null) {
            this.javaClass.write(writer);
        }

    }

    private void doWrite() throws IOException {
        if (!this.language.equals(Language.GATEWAY) || this.javaService != null) {
            File file = new File(this.filePath);
            if (!file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
                throw new IOException("Failed to create folders");
            } else if (!file.exists()) {
                if (file.createNewFile()) {
                    FileWriter fileWriter = new FileWriter(file);
                    BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);

                    try {
                        this.write(bufferedWriter);
                    } finally {
                        bufferedWriter.close();
                        fileWriter.close();
                    }

                } else {
                    throw new IOException("Failed to create support");
                }
            }
        }
    }
}
