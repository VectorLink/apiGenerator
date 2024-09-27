package com.vector.generator.support;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import com.vector.enums.Language;
import org.apache.commons.lang3.StringUtils;
import lombok.Data;

@Data
public class JavaService extends AbstractJavaGenerator{
    private String prefix;
    private String appName;
    private String serviceName;
    private List<JavaMethod> methods = new ArrayList();
    private String comment;

    public JavaService(Language language, String prefix, String serviceName, String appName, String comment) {
        super(language);
        this.prefix = prefix;
        this.serviceName = serviceName;
        this.appName = appName;
        this.comment = comment;
    }

    public void addMethod(JavaMethod method) {
        this.methods.add(method);
    }

    public String getFileName() {
        return this.language.equals(Language.SPRING) ? this.serviceName + "ControllerApi" : this.serviceName + "Service";
    }

    public String getSuffixAppName() {
        String[] names = this.appName.split("\\.");
        return names.length > 1 ? StringUtils.join(names, "/", 1, names.length) + "/" : "";
    }

    @Override
    public void write(BufferedWriter writer) throws IOException {
        super.write(writer);
        Iterator var2 = this.methods.iterator();

        while(var2.hasNext()) {
            JavaMethod method = (JavaMethod)var2.next();
            method.write(writer);
        }

        writer.write("}\n");
    }

    @Override
    public void writeSpring(BufferedWriter writer) throws IOException {
        if (StringUtils.isNotEmpty(this.comment)) {
            writer.write("@Api(description = \"" + this.comment + "\")\n");
        }

        writer.write("public interface " + this.getFileName() + " {\n");
    }

    @Override
    public void writeGateway(BufferedWriter writer) throws IOException {
        writer.write("@Service(\"" + this.appName + "." + this.serviceName + "\")\n");
        writer.write("public class " + this.getFileName() + " extends " + this.serviceName + "Grpc." + this.serviceName + "ImplBase {\n");
        writer.write("  @Autowired\n");
        writer.write("  private GrpcExecutor executor;\n");
    }

    public String getPrefix() {
        return this.prefix;
    }

    public String getAppName() {
        return this.appName;
    }
}
