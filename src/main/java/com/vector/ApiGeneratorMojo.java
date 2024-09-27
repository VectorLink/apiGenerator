package com.vector;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ResolutionErrorHandler;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.plexus.util.Os;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import com.vector.enums.Language;
import com.vector.generator.BasePackage;
import com.vector.generator.ProtoGenerator;

@Mojo(
        name = "generateProto",
        defaultPhase = LifecyclePhase.PROCESS_SOURCES
)
public class ApiGeneratorMojo extends AbstractMojo {
    @Parameter(
            defaultValue = "apidoc/proto"
    )
    private String proto;
    @Parameter(
            defaultValue = "src/generated/grpc"
    )
    private String output;
    @Parameter
    private Set<String> files;
    @Parameter
    private Set<String> directories;
    @Parameter(
            defaultValue = "SPRING"
    )
    private Language lang;
    @Parameter
    private List<String> imports;
    @Parameter
    private String protocExecutable;
    @Parameter
    private String protocArtifact;
    @Parameter(
            defaultValue = "${project.build.directory}/protoc-plugins"
    )
    private File protocPluginDirectory;
    @Parameter(
            defaultValue = "com.xiaoniu.common.model.common"
    )
    private String corePackage;
    @Parameter
    private String enumPackage;
    @Parameter(
            defaultValue = "true"
    )
    private boolean includeProject;
    @Parameter(
            defaultValue = "false"
    )
    private boolean recursive;
    @Parameter(
            defaultValue = "${project}",
            readonly = true
    )
    private MavenProject project;
    @Parameter(
            defaultValue = "${session}",
            readonly = true
    )
    private MavenSession session;
    @Parameter(
            required = true,
            readonly = true,
            property = "localRepository"
    )
    private ArtifactRepository localRepository;
    @Parameter(
            required = true,
            readonly = true,
            defaultValue = "${project.remoteArtifactRepositories}"
    )
    private List<ArtifactRepository> remoteRepositories;
    @Component
    private ArtifactFactory artifactFactory;
    @Component
    private RepositorySystem repositorySystem;
    @Component
    private ResolutionErrorHandler resolutionErrorHandler;
    private static final String VERSION = "3.0.0-SNAPSHOT";
    @Parameter(
            defaultValue = "false"
    )
    private Boolean removed;
    @Parameter(
            required = false,
            property = "apigen.skip",
            defaultValue = "false"
    )
    private Boolean skip;
    @Parameter(
            defaultValue = "true"
    )
    private Boolean useCore;
    @Parameter(
            defaultValue = "false"
    )
    private Boolean onlyGenMessage;

    public ApiGeneratorMojo() {
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (this.skip) {
            this.getLog().info("Skipped...3.0.0-SNAPSHOT");
        } else {
            this.getLog().info("Starting...3.0.0-SNAPSHOT");
            if (!StringUtils.isEmpty(this.proto) && !StringUtils.isEmpty(this.output) && (!StringUtils.isEmpty(this.protocExecutable) || !StringUtils.isEmpty(this.protocArtifact))) {
                this.getLog().info("Start to download plugins...");
                if (StringUtils.isEmpty(this.protocExecutable)) {
                    Artifact artifact = this.createDependencyArtifact(this.protocArtifact);
                    this.protocExecutable = this.resolveBinaryArtifact(artifact).getAbsolutePath();
                }

                this.getLog().info("Start to generate proto...");
                ProtoGenerator generator = new ProtoGenerator(this.protocExecutable, BasePackage.builder().corePath(this.corePackage).build(), this.lang, this.imports, this.useCore, this.onlyGenMessage);

                try {
                    if (this.removed) {
                        this.getLog().info("Start to clean...");
                        generator.clean(this.output);
                    }

                    this.getLog().info("Start to generate...");
                    this.output = StringUtils.appendIfMissing(this.output, "/", new CharSequence[0]);
                    if (this.files == null) {
                        this.files = new HashSet();
                    }

                    if (this.directories == null) {
                        this.directories = new HashSet();
                    }

                    this.getLog().info("includeProject: " + this.includeProject);
                    if (this.includeProject) {
                        this.directories.add(this.project.getName());
                    }

                    this.getLog().info("proto: " + this.proto);
                    Iterator var2 = this.directories.iterator();


                    while (var2.hasNext()) {
                        String s = (String) var2.next();
                        File file = new File(StringUtils.appendIfMissing(this.proto, "/", new CharSequence[0]) + s);
                        this.getLog().info("directory: " + file.getAbsolutePath());
                        this.getLog().info("recursive: " + this.recursive);
                        if (file.isDirectory() && file.exists()) {
                            FileUtils.listFiles(file, (String[]) null, this.recursive).forEach((o) -> {
                                String sx = ((File) o).getPath();
                                if (sx.endsWith(".proto")) {
                                    this.files.add(s + StringUtils.removeStart(sx, file.getPath()));
                                }

                            });
                        }
                    }

                    this.getLog().info("files: " + this.files);
                    var2 = this.files.iterator();

                    while (var2.hasNext()) {
                        String s = (String) var2.next();
                        s = StringUtils.appendIfMissing(this.proto, "/", new CharSequence[0]) + StringUtils.appendIfMissing(s, ".proto", new CharSequence[0]);
                        generator.generateFile(s, this.output, this.proto);
                    }

                    this.removeGoogleFile();
                } catch (Exception var5) {
                    this.getLog().error(var5);
                    throw new MojoFailureException(var5.getMessage(), var5);
                }
            } else {
                throw new MojoFailureException("Missing parameter");
            }
        }
    }

    private void removeGoogleFile() throws IOException {
        File file = new File(this.output, "com/google");
        FileUtils.deleteDirectory(file);
    }

    private Artifact createDependencyArtifact(String artifactSpec) throws MojoExecutionException {
        String[] parts = artifactSpec.split(":");
        if (parts.length >= 3 && parts.length <= 5) {
            String type = parts.length >= 4 ? parts[3] : "exe";
            String classifier = parts.length == 5 ? parts[4] : null;
            return this.createDependencyArtifact(parts[0], parts[1], parts[2], type, classifier);
        } else {
            throw new MojoExecutionException("Invalid artifact specification format, expected: groupId:artifactId:version[:type[:classifier]], actual: " + artifactSpec);
        }
    }

    private Artifact createDependencyArtifact(String groupId, String artifactId, String version, String type, String classifier) throws MojoExecutionException {
        VersionRange versionSpec;
        try {
            versionSpec = VersionRange.createFromVersionSpec(version);
        } catch (InvalidVersionSpecificationException var8) {
            throw new MojoExecutionException("Invalid version specification", var8);
        }

        return this.artifactFactory.createDependencyArtifact(groupId, artifactId, versionSpec, type, classifier, "runtime");
    }

    private File resolveBinaryArtifact(Artifact artifact) throws MojoExecutionException {
        ArtifactResolutionResult result;
        try {
            ArtifactResolutionRequest request = (new ArtifactResolutionRequest()).setArtifact(this.project.getArtifact()).setResolveRoot(false).setResolveTransitively(false).setArtifactDependencies(Collections.singleton(artifact)).setManagedVersionMap(Collections.emptyMap()).setLocalRepository(this.localRepository).setRemoteRepositories(this.remoteRepositories).setOffline(this.session.isOffline()).setForceUpdate(this.session.getRequest().isUpdateSnapshots()).setServers(this.session.getRequest().getServers()).setMirrors(this.session.getRequest().getMirrors()).setProxies(this.session.getRequest().getProxies());
            result = this.repositorySystem.resolve(request);
            this.resolutionErrorHandler.throwErrors(request, result);
        } catch (ArtifactResolutionException var12) {
            throw new MojoExecutionException("Unable to resolve artifact: " + var12.getMessage(), var12);
        }

        Set<Artifact> artifacts = result.getArtifacts();
        if (artifacts != null && !artifacts.isEmpty()) {
            Artifact resolvedBinaryArtifact = (Artifact) artifacts.iterator().next();
            if (this.getLog().isDebugEnabled()) {
                this.getLog().debug("Resolved artifact: " + resolvedBinaryArtifact);
            }

            File sourceFile = resolvedBinaryArtifact.getFile();
            String sourceFileName = sourceFile.getName();
            String targetFileName;
            if (Os.isFamily("windows") && !sourceFileName.endsWith(".exe")) {
                targetFileName = sourceFileName + ".exe";
            } else {
                targetFileName = sourceFileName;
            }

            File targetFile = new File(this.protocPluginDirectory, targetFileName);
            if (targetFile.exists()) {
                this.getLog().debug("Executable file already exists: " + targetFile.getAbsolutePath());
                return targetFile;
            } else {
                try {
                    FileUtils.forceMkdir(this.protocPluginDirectory);
                } catch (IOException var11) {
                    throw new MojoExecutionException("Unable to create directory " + this.protocPluginDirectory, var11);
                }

                try {
                    FileUtils.copyFile(sourceFile, targetFile);
                } catch (IOException var10) {
                    throw new MojoExecutionException("Unable to copy the file to " + this.protocPluginDirectory, var10);
                }

                if (!Os.isFamily("windows")) {
                    targetFile.setExecutable(true);
                }

                if (this.getLog().isDebugEnabled()) {
                    this.getLog().debug("Executable file: " + targetFile.getAbsolutePath());
                }

                return targetFile;
            }
        } else {
            throw new MojoExecutionException("Unable to resolve artifact");
        }
    }

    public static void main(String[] args) throws MojoFailureException, MojoExecutionException {
        ApiGeneratorMojo mojo = new ApiGeneratorMojo();
        mojo.proto = "apidoc/proto";
        mojo.output = "src/generated/grpc";
        mojo.lang = Language.SPRING;
        mojo.directories = new HashSet(Arrays.asList("consumer"));
        mojo.recursive = true;
        mojo.corePackage = "com.xtm.core";
        mojo.protocExecutable = "protoc";
        mojo.skip=false;
        mojo.useCore=false;
        mojo.removed=true;
        mojo.execute();
    }
}
