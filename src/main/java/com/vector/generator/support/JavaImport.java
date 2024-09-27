package com.vector.generator.support;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import com.vector.enums.Language;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JavaImport extends AbstractJavaGenerator {
    private List<String> imports = new ArrayList();
    private List<String> baseImports = new ArrayList();

    public JavaImport(Language language) {
        super(language);
    }

    public JavaImport(Language language, List<String> baseImports) {
        super(language);
        this.baseImports = baseImports;
    }

    public void addImport(String s) {
        this.imports.add(s);
    }

    public void addBaseImport(String s) {
        this.baseImports.add(s);
    }

    @Override
    public void write(BufferedWriter writer) throws IOException {
        Iterator var2 = this.baseImports.iterator();

        String s;
        while(var2.hasNext()) {
            s = (String)var2.next();
            writer.write("import " + s + ";\n");
        }

        writer.write("\n");
        var2 = this.imports.iterator();

        while(var2.hasNext()) {
            s = (String)var2.next();
            writer.write("import " + s + ";\n");
        }

        writer.write("\n");
    }
}
