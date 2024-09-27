package com.vector.generator.support;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import com.vector.enums.Language;
import com.vector.enums.MethodType;
import org.apache.commons.lang3.StringUtils;
import lombok.Data;

@Data
public class JavaMethod extends AbstractJavaGenerator {
    private String methodName;
    private JavaClass inputClass;
    private JavaClass outputClass;
    private Set<MethodType> methodType;
    private JavaService service;
    private String comment;
    private Boolean useCore;

    public JavaMethod(Language language, String methodName, String comment, JavaService service, Boolean useCore) {
        super(language);
        this.methodName = methodName;
        this.comment = comment;
        this.service = service;
        this.methodType = new HashSet();
        this.useCore = useCore;
    }

    public void addMethodType(MethodType methodType) {
        this.methodType.add(methodType);
    }

    @Override
    public void writeSpring(BufferedWriter writer) throws IOException {
        String firstLetterLowercaseMethod = this.methodName.substring(0, 1).toLowerCase() + this.methodName.substring(1);
        String mapping = null;
        if (this.useCore) {
            mapping = "@RequestMapping(value = \"" + this.service.getPrefix() + this.service.getSuffixAppName() + this.service.getServiceName() + "/" + this.methodName + "\", method = RequestMethod.POST";
        } else {
            mapping = "@RequestMapping(value = \"" + this.service.getPrefix() + this.service.getSuffixAppName() + this.service.getAppName() + "." + this.service.getServiceName() + "/" + this.methodName + "\", method = RequestMethod.POST";
        }

        if (this.methodType.contains(MethodType.DOWNLOAD)) {
            mapping = mapping + ", produces = org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE";
        }

        mapping = mapping + ")";
        String inType = "";
        if (this.inputClass != null && !this.inputClass.isEmpty()) {
            if (!this.methodType.contains(MethodType.UPLOAD)) {
                inType = "@RequestBody ";
            }

            inType = inType + this.inputClass.getRepeatedName() + " param";
        }

        if (this.methodType.contains(MethodType.UPLOAD)) {
            if (!this.useCore) {
                inType = "org.springframework.web.multipart.MultipartFile file";
            } else {
                if (StringUtils.isNotEmpty(inType)) {
                    inType = inType + ", ";
                }

                inType = inType + "org.springframework.web.multipart.MultipartFile file";
            }
        }

        if (this.methodType.contains(MethodType.DOWNLOAD)) {
            if (StringUtils.isNotEmpty(inType)) {
                inType = inType + ", ";
            }

            inType = inType + "javax.servlet.http.HttpServletResponse response";
        }

        String outType;
        if (this.outputClass != null && !this.outputClass.isEmpty()) {
            if (this.useCore) {
                if (this.outputClass.isPaged()) {
                    outType = "PageResult";
                } else {
                    outType = "CommonResponse";
                }

                if (StringUtils.isNotEmpty(this.outputClass.getClassName()) && (!this.outputClass.isMapField() || !this.outputClass.isPaged())) {
                    outType = outType + "<" + this.outputClass.getRepeatedName() + ">";
                }
            } else {
                outType = this.outputClass.getRepeatedName();
            }
        } else {
            outType = "void";
        }

        if (StringUtils.isNotEmpty(this.comment)) {
            writer.write("  @ApiOperation(\"" + this.comment + "\")\n");
            writer.write("  @ApiResponses(@ApiResponse(code = 200, message = \"" + this.comment + "\"))\n");
        }

        if (this.useCore) {
            writer.write("  " + mapping + "\n  " + outType + " " + this.methodName + "(" + inType + ");\n\n");
        } else {
            writer.write("  " + mapping + "\n  " + outType + " " + firstLetterLowercaseMethod + "(" + inType + ");\n\n");
        }

    }

    @Override
    public void writeGateway(BufferedWriter writer) throws IOException {
        writer.write("  @Override\n");
        writer.write("  public void " + this.methodName + "(" + this.inputClass.getJavaName() + "." + this.inputClass.getMessageType() + " request, StreamObserver<" + this.outputClass.getJavaName() + "." + this.outputClass.getMessageType() + "> responseObserver) {\n");
        writer.write("      executor.httpSend(request, responseObserver, " + this.outputClass.getJavaName() + "." + this.outputClass.getMessageType() + ".Builder.class);\n");
        writer.write("  }\n");
    }
}
