package com.vector.generator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import com.google.protobuf.ByteString;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import com.google.protobuf.UnknownFieldSet;
import com.vector.enums.ClassType;
import com.vector.enums.Language;
import com.vector.enums.MethodType;
import com.vector.generator.support.JavaClass;
import com.vector.generator.support.JavaField;
import com.vector.generator.support.JavaFile;
import com.vector.generator.support.JavaImport;
import com.vector.generator.support.JavaMethod;
import com.vector.generator.support.JavaPackage;
import com.vector.generator.support.JavaService;
import org.apache.commons.lang3.StringUtils;

public class ProtoGenerator extends AbstractGenerator {

    private List<String> apiBaseImports = new ArrayList();
    private List<String> defBaseImports = new ArrayList();
    private List<String> enumBaseImports = new ArrayList();
    private String protocExecutable;
    private BasePackage basePackage;
    private Language language;
    private Boolean useCore;
    private Boolean genMessage;
    private Map<String, JavaClass> definitionMap = new HashMap();

    public ProtoGenerator(String protocExecutable, BasePackage basePackage, Language language, List<String> imports, Boolean useCore, Boolean genMessage) {
        this.protocExecutable = protocExecutable;
        this.basePackage = basePackage;
        this.language = language;
        this.useCore = useCore;
        this.genMessage = genMessage;
        this.definitionMap.clear();
        this.apiBaseImports.clear();
        this.defBaseImports.clear();
        this.enumBaseImports.clear();
        if (language.equals(Language.SPRING)) {
            if (useCore) {
                this.apiBaseImports.add(basePackage.getCorePath() + ".CommonResponse");
//                this.apiBaseImports.add(basePackage.getCorePath() + ".PageResult");
            }

            this.apiBaseImports.add("org.springframework.web.bind.annotation.RequestBody");
            this.apiBaseImports.add("org.springframework.web.bind.annotation.RequestMapping");
            this.apiBaseImports.add("org.springframework.web.bind.annotation.RequestMethod");
            this.apiBaseImports.add("org.springframework.web.bind.annotation.RequestParam");
            if (useCore) {
                this.defBaseImports.add("io.swagger.annotations.ApiModel");
                this.defBaseImports.add("io.swagger.annotations.ApiModelProperty");
            }

            this.defBaseImports.add("lombok.Getter");
            this.defBaseImports.add("lombok.Setter");
            this.defBaseImports.add("lombok.Builder");
            this.defBaseImports.add("lombok.experimental.Tolerate");
            this.enumBaseImports.add("lombok.Getter");
            this.enumBaseImports.add("lombok.Setter");
        } else {
            this.apiBaseImports.add(basePackage.getCorePath() + ".grpc.GrpcExecutor");
            this.apiBaseImports.add("io.grpc.stub.StreamObserver");
            this.apiBaseImports.add("org.springframework.stereotype.Service");
            this.apiBaseImports.add("org.springframework.beans.factory.annotation.Autowired");
        }

        if (imports != null) {
            this.apiBaseImports.addAll(imports);
        }

    }

    @Override
    public void generateFile(String input, String output, String baseDir) throws Exception {
        this.generateFile(input, output, baseDir, (fileDescriptor) -> {
            String packageName = this.getJavaPackageName(fileDescriptor);
            String outputDir = output + (StringUtils.isEmpty(packageName) ? "" : packageName.replaceAll("\\.", "/")) + "/";
            Iterator var5;
            if (this.genMessage && fileDescriptor.getServices().size() <= 0) {
                var5 = fileDescriptor.getMessageTypes().iterator();

                while(var5.hasNext()) {
                    Descriptors.Descriptor messageDescriptor = (Descriptors.Descriptor)var5.next();
                    this.generateDefinitionFile(output, messageDescriptor, ClassType.MODEL);
                }
            } else {
                var5 = fileDescriptor.getServices().iterator();

                while(var5.hasNext()) {
                    Descriptors.ServiceDescriptor serviceDescriptor = (Descriptors.ServiceDescriptor)var5.next();
                    JavaService javaService = new JavaService(this.language, this.getSpringPrefix(fileDescriptor), serviceDescriptor.getName(), fileDescriptor.getPackage(), "");
                    JavaFile javaFile = new JavaFile(this.language, outputDir + javaService.getFileName() + ".java");
                    javaFile.setJavaPackage(new JavaPackage(this.language, packageName));
                    javaFile.setBaseImports(this.apiBaseImports);
                    javaFile.setJavaService(javaService);

                    JavaMethod javaMethod;
                    for(Iterator var9 = serviceDescriptor.getMethods().iterator(); var9.hasNext(); javaService.addMethod(javaMethod)) {
                        Descriptors.MethodDescriptor methodDescriptor = (Descriptors.MethodDescriptor)var9.next();
                        javaMethod = new JavaMethod(this.language, methodDescriptor.getName(), "", javaService, this.useCore);
                        if (this.isUploadMethod(methodDescriptor)) {
                            javaMethod.addMethodType(MethodType.UPLOAD);
                        }

                        if (this.isDownloadMethod(methodDescriptor)) {
                            javaMethod.addMethodType(MethodType.DOWNLOAD);
                        }

                        if (this.useCore || !this.isUploadMethod(methodDescriptor)) {
                            javaMethod.setInputClass(this.generateParamFile(output, methodDescriptor.getInputType()));
                        }

                        if (this.useCore) {
                            javaMethod.setOutputClass(this.generateModelFile(output, methodDescriptor.getOutputType()));
                        } else {
                            javaMethod.setOutputClass(this.generateResponseFile(output, methodDescriptor.getOutputType()));
                        }
                    }

                    javaFile.write();
                }
            }

        });
    }

    private JavaClass generateParamFile(String output, Descriptors.Descriptor descriptor) {
        JavaClass javaClass = this.generateDefinitionFile(output, descriptor, ClassType.PARAM);
        javaClass.setMessageType(descriptor.getName());
        javaClass.setJavaName(descriptor.getFile());
        return javaClass;
    }

    private JavaClass generateModelFile(String output, Descriptors.Descriptor descriptor) {
        JavaClass javaClass = null;
        boolean paged = false;
        Iterator var5 = descriptor.getFields().iterator();

        while(var5.hasNext()) {
            Descriptors.FieldDescriptor fieldDescriptor = (Descriptors.FieldDescriptor)var5.next();
            if (javaClass == null) {
                String packageName = this.getJavaPackageName(fieldDescriptor);
                javaClass = new JavaClass(this.language, "", ClassType.MODEL, packageName, "");
            }

            if (fieldDescriptor.getName().equals("page")) {
                paged = true;
            } else if (fieldDescriptor.getName().equals("data")) {
                if (fieldDescriptor.getJavaType().name().equals("MESSAGE")) {
                    javaClass = this.generateDefinitionFile(output, fieldDescriptor.getMessageType(), ClassType.MODEL);
                } else {
                    javaClass.setPackageName(this.getJavaPackageName(fieldDescriptor));
                    javaClass.setClassName(fieldDescriptor.getJavaType().name());
                    javaClass.setPrimitive(true);
                    javaClass.setClassType(ClassType.MODEL);
                }

                javaClass.setRepeated(fieldDescriptor.isRepeated());
            }

            javaClass.setMessageType(descriptor.getName());
            javaClass.setJavaName(descriptor.getFile());
        }

        if (javaClass != null) {
            javaClass.setPaged(paged);
        }

        return javaClass;
    }

    private JavaClass generateResponseFile(String output, Descriptors.Descriptor descriptor) {
        boolean result = false;
        JavaClass javaClass = this.generateDefinitionFile(output, descriptor, ClassType.MODEL);
        javaClass.setMessageType(descriptor.getName());
        javaClass.setJavaName(descriptor.getFile());
        javaClass.setPaged(false);
        javaClass.setResulted(false);
        return javaClass;
    }

    private JavaClass generateDefinitionFile(String output, Descriptors.Descriptor descriptor, ClassType classType) {
        if (!this.isGenerated(descriptor)) {
            return new JavaClass(this.language, false);
        } else {
            JavaFile javaFile = this.getDefinitionFile(output, descriptor, classType);
            JavaClass javaClass = javaFile.getJavaClass();
            if (this.definitionMap.containsKey(javaClass.getFullName()) && !javaClass.getClassName().equalsIgnoreCase("DataEntry")) {
                return javaClass;
            } else {
                this.definitionMap.put(javaClass.getFullName(), javaClass);
                Iterator var6 = descriptor.getFields().iterator();

                while(true) {
                    while(var6.hasNext()) {
                        Descriptors.FieldDescriptor fieldDescriptor = (Descriptors.FieldDescriptor)var6.next();
                        if (fieldDescriptor.getName().equals("result") && !this.useCore) {
                            javaClass.setResulted(true);
                            javaFile.addImport("com.xtm.common.model.Result");
                        } else {
                            JavaField javaField = new JavaField(this.language, fieldDescriptor.getName(), "", fieldDescriptor.getJavaType().name(), (Integer)null);
                            javaField.setRepeated(fieldDescriptor.isRepeated());
                            if (!javaField.isPrimitive()) {
                                javaField.setMapField(fieldDescriptor.isMapField());
                                Descriptors.GenericDescriptor genericDescriptor = javaField.isEnum() ? fieldDescriptor.getEnumType() : fieldDescriptor.getMessageType();
                                JavaClass c = this.generateDefinitionFile(output, (Descriptors.GenericDescriptor)genericDescriptor, classType);
                                if (javaField.isMapField()) {
                                    javaField.setMessageType(StringUtils.join((Iterable)c.getFields().stream().map(JavaField::getJavaType).collect(Collectors.toList()), ", "));
                                } else {
                                    javaField.setMessageType(this.getJavaPackageName((Descriptors.GenericDescriptor)genericDescriptor) + "." + ((Descriptors.GenericDescriptor)genericDescriptor).getName());
                                }

                                if (!c.isGenerated()) {
                                    continue;
                                }
                            }

                            javaClass.addField(javaField);
                        }
                    }

                    if (descriptor.getOptions().hasMapEntry()) {
                        javaClass.setMapField(true);
                    } else {
                        javaFile.write();
                    }

                    return javaClass;
                }
            }
        }
    }

    private JavaClass generateDefinitionFile(String output, Descriptors.EnumDescriptor descriptor, ClassType classType) {
        if (!this.isGenerated(descriptor)) {
            return new JavaClass(this.language, false);
        } else {
            JavaFile javaFile = this.getDefinitionFile(output, descriptor, classType);
            javaFile.setJavaImport((JavaImport)null);
            javaFile.setBaseImports(this.enumBaseImports);
            JavaClass javaClass = javaFile.getJavaClass();
            javaClass.setEnum(true);
            if (this.definitionMap.containsKey(javaClass.getFullName())) {
                return javaClass;
            } else {
                this.definitionMap.put(javaClass.getFullName(), javaClass);
                Iterator var6 = descriptor.getValues().iterator();

                while(var6.hasNext()) {
                    Descriptors.EnumValueDescriptor valueDescriptor = (Descriptors.EnumValueDescriptor)var6.next();
                    JavaField javaField = new JavaField(this.language, valueDescriptor.getName(), "", "", valueDescriptor.getNumber());
                    javaClass.addField(javaField);
                }

                javaFile.write();
                return javaClass;
            }
        }
    }

    private JavaClass generateDefinitionFile(String output, Descriptors.GenericDescriptor descriptor, ClassType classType) {
        return descriptor instanceof Descriptors.Descriptor ? this.generateDefinitionFile(output, (Descriptors.Descriptor)descriptor, classType) : this.generateDefinitionFile(output, (Descriptors.EnumDescriptor)descriptor, classType);
    }

    private JavaFile getDefinitionFile(String output, Descriptors.GenericDescriptor descriptor, ClassType classType) {
        String packageName = this.getJavaPackageName(descriptor);
        String outputDir = output + (StringUtils.isEmpty(packageName) ? "" : packageName.replaceAll("\\.", "/")) + "/";
        JavaFile javaFile = new JavaFile(this.language, outputDir + descriptor.getName() + ".java");
        javaFile.setJavaPackage(new JavaPackage(this.language, packageName));
        javaFile.setBaseImports(this.defBaseImports);
        if (this.useCore) {
            javaFile.addImport(" java.io.Serializable;");
        }

        JavaClass javaClass = new JavaClass(this.language, descriptor.getName(), classType, packageName, "");
        javaClass.setUseCore(this.useCore);
        javaFile.setJavaClass(javaClass);
        return javaFile;
    }

    private String getJavaPackageName(Descriptors.GenericDescriptor descriptor) {
        String packageName = descriptor.getFile().getOptions().getJavaPackage();
        if (StringUtils.isBlank(packageName)) {
            if (this.useCore) {
                packageName = "com.ezbuy.api." + descriptor.getFile().getPackage();
            } else {
                packageName = "com.elitb.infra.api." + descriptor.getFile().getPackage();
            }
        }

        return packageName;
    }

    private void generateDefinition(Descriptors.Descriptor descriptor, boolean isInputType, String packageName) {
        System.out.println("@ApiModel(description = \"批量订单参数\")");
        System.out.println("    @ApiModelProperty(\"订单参数\")");
    }

    private void generateFile(String input, String output, String baseDir, Consumer<Descriptors.FileDescriptor> consumer) throws Exception {
        File tempFile = File.createTempFile("apigen_", ".desc");
        FileInputStream inputStream = null;

        try {
            String command = String.format("%s --descriptor_set_out=%s %s --include_imports --include_source_info --proto_path=%s", this.protocExecutable, tempFile.getAbsolutePath(), input, baseDir);
            this.getLog().info("Output description file[" + input + "]");
            Process process = Runtime.getRuntime().exec(command);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            while((line = reader.readLine()) != null) {
                this.getLog().info(line);
            }

            reader.close();
            reader = new BufferedReader(new InputStreamReader(process.getErrorStream()));

            while((line = reader.readLine()) != null) {
                this.getLog().error(line);
            }

            reader.close();
            process.waitFor();
            if (process.exitValue() != 0) {
                throw new IOException("Failed to generate description support[" + command + "]");
            } else {
                this.getLog().info("Start to parse description file");
                List<Descriptors.FileDescriptor> dependencies = new ArrayList();
                inputStream = new FileInputStream(tempFile);
                DescriptorProtos.FileDescriptorSet fileDescriptorSet = DescriptorProtos.FileDescriptorSet.parseFrom(inputStream);
                inputStream.close();
                Iterator var13 = fileDescriptorSet.getFileList().iterator();

                Descriptors.FileDescriptor fileDescriptor;
                do {
                    if (!var13.hasNext()) {
                        return;
                    }

                    DescriptorProtos.FileDescriptorProto fileDescriptorProto = (DescriptorProtos.FileDescriptorProto)var13.next();
                    fileDescriptor = Descriptors.FileDescriptor.buildFrom(fileDescriptorProto, (Descriptors.FileDescriptor[])dependencies.toArray(new Descriptors.FileDescriptor[0]));
                    dependencies.add(fileDescriptor);
                } while(fileDescriptor.getServices().size() <= 0 && !this.genMessage);

                consumer.accept(fileDescriptor);
            }
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }

            if (!tempFile.delete()) {
                tempFile.deleteOnExit();
            }

        }
    }

    private boolean isUploadMethod(Descriptors.MethodDescriptor methodDescriptor) {
        String options = methodDescriptor.getOptions().toString();
        return options.startsWith("72295728") && options.contains("1: 1") ? true : methodDescriptor.getName().toLowerCase().startsWith("upload");
    }

    private boolean isDownloadMethod(Descriptors.MethodDescriptor methodDescriptor) {
        String options = methodDescriptor.getOptions().toString();
        return options.startsWith("72295728") && options.contains("2: 1") ? true : methodDescriptor.getName().toLowerCase().startsWith("download");
    }

    private boolean isGenerated(Descriptors.Descriptor descriptor) {
        String options = descriptor.getOptions().toString();
        return !options.startsWith("80000001") || !options.contains("1: 0");
    }

    private boolean isGenerated(Descriptors.EnumDescriptor descriptor) {
        String options = descriptor.getOptions().toString();
        return !options.startsWith("80000001") || !options.contains("1: 0");
    }

    private String getSpringPrefix(Descriptors.FileDescriptor fileDescriptor) {
        UnknownFieldSet.Field field = fileDescriptor.getOptions().getUnknownFields().getField(800);

        try {
            return field != null && field.getLengthDelimitedList().size() != 0 ? ((ByteString)field.getLengthDelimitedList().get(0)).toString("UTF-8") + "." : "";
        } catch (UnsupportedEncodingException var4) {
            throw new RuntimeException(var4);
        }
    }
}
