package com.vector.generator.support;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import com.google.protobuf.Descriptors;
import com.vector.enums.ClassType;
import com.vector.enums.Language;
import org.apache.commons.lang3.StringUtils;
import lombok.Data;

@Data
public class JavaClass extends AbstractJavaGenerator{
    private String className;
    private ClassType classType;
    private String packageName;
    private boolean paged;
    private boolean repeated;
    private boolean mapField;
    private boolean isPrimitive;
    private boolean isEnum;
    private boolean isGenerated = true;
    private List<JavaField> fields = new ArrayList();
    private String comment;
    private String javaName;
    private String messageType;
    private boolean resulted;
    private boolean useCore = true;

    public JavaClass(Language language) {
        super(language);
    }

    public JavaClass(Language language, boolean isGenerated) {
        super(language);
        this.isGenerated = isGenerated;
    }

    public JavaClass(Language language, String className, ClassType classType, String packageName, String comment) {
        super(language);
        this.className = className;
        this.classType = classType;
        this.packageName = packageName;
        this.comment = comment;
    }

    public void addField(JavaField field) {
        this.fields.add(field);
    }

    public String getRepeatedName() {
        String repeatedName = this.getJavaType();
        if (!this.isPrimitive && StringUtils.isNotEmpty(this.packageName)) {
            repeatedName = this.packageName + "." + repeatedName;
        }

        if (this.repeated && !this.paged) {
            repeatedName = "java.util.List<" + repeatedName + ">";
        }

        if (this.mapField && !this.paged) {
            repeatedName = "java.util.Map<" + StringUtils.join((Iterable)this.fields.stream().map(JavaField::getJavaType).collect(Collectors.toList()), ", ") + ">";
        }

        return repeatedName;
    }

    public String getFullName() {
        return this.packageName + "." + this.className;
    }

    public String getJavaType() {
        if (!this.isPrimitive) {
            return this.className;
        } else if (this.className.equals("INT")) {
            return "Integer";
        } else {
            return this.className.equals("BYTE_STRING") ? "byte[]" : this.className.substring(0, 1) + this.className.substring(1).toLowerCase();
        }
    }

    public void setJavaName(Descriptors.FileDescriptor descriptor) {
        String packageName = descriptor.getOptions().getJavaPackage();
        String[] splits = StringUtils.substringBefore(StringUtils.substringAfterLast("/" + descriptor.getName(), "/") + ".", ".").split("_");
        StringBuilder sb = new StringBuilder();
        String[] var5 = splits;
        int var6 = splits.length;

        for(int var7 = 0; var7 < var6; ++var7) {
            String s = var5[var7];
            if (StringUtils.isNotEmpty(s)) {
                sb.append(s.substring(0, 1).toUpperCase()).append(s.substring(1));
            }
        }

        if (descriptor.findMessageTypeByName(sb.toString()) != null || descriptor.findServiceByName(sb.toString()) != null || descriptor.findEnumTypeByName(sb.toString()) != null || descriptor.findExtensionByName(sb.toString()) != null) {
            sb.append("OuterClass");
        }

        this.javaName = packageName + "." + sb.toString();
    }

    public boolean isEmpty() {
        return this.className == null;
    }

    @Override
    public void write(BufferedWriter writer) throws IOException {
        if (StringUtils.isNotEmpty(this.comment)) {
            writer.write("@ApiModel(description = \"" + this.comment + "\")\n");
        }

        if (this.isEnum) {
            writer.write("public enum " + this.className + " {\n  ");

            for(int i = 0; i < this.fields.size(); ++i) {
                ((JavaField)this.fields.get(i)).write(writer);
                if (i < this.fields.size() - 1) {
                    writer.write(",");
                }
            }

            writer.write(";\n\n");
            writer.write("  private " + this.className + " (int code) { this.code = code;}\n\n  @Getter @Setter \n  private int code;\n  public static " + this.className + " getEnumByCode(Integer code) { \n       if(code==null) {\n    \t\treturn null;\n      } \n      for (" + this.className + " value : " + this.className + ".values()) { \n        if (value.code == code) { \n            return value;\n        } \n      } \n    return null; \n  } \n");
            writer.write("}\n");
        } else {
            writer.write("@Getter @Setter");
            if (this.fields.size() > 0) {
                writer.write(" @Builder");
            }

            writer.write("\n");
            if (this.useCore) {
                writer.write("public class " + this.className + " implements Serializable" + " {\n");
            } else {
                writer.write("public class " + this.className + " {\n");
            }

            if (this.resulted) {
                writer.write("  private Result result;\n");
            }

            Iterator var4 = this.fields.iterator();

            while(var4.hasNext()) {
                JavaField field = (JavaField)var4.next();
                field.write(writer);
            }

            writer.write("  @Tolerate\n");
            writer.write("  public " + this.className + "() { }\n");
            writer.write("}\n");
        }

    }
}
