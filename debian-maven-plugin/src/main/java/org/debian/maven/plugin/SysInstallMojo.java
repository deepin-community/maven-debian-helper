/*
 * Copyright 2009 Torsten Werner, Ludovic Claude.
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

package org.debian.maven.plugin;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.io.RawInputStreamFacade;
import org.debian.maven.repo.Dependency;
import org.debian.maven.repo.ListOfPOMs;
import org.debian.maven.repo.POMCleaner;
import org.debian.maven.repo.POMOptions;

/**
 * Install pom and jar files into the /usr/share/hierarchy
 */
@Mojo(name = "sysinstall")
public class SysInstallMojo extends AbstractMojo {

    /** Regex for detecting that package is a libXXX-java package */
    private static final Pattern JAVA_LIB_REGEX = Pattern.compile("lib.*-java");

    /** Regex for detecting that package is a maven plugin package */
    private static final Pattern PLUGIN_REGEX = Pattern.compile("lib.*-maven-plugin-java|libmaven-.*-plugin-java");

    // ----------------------------------------------------------------------
    // Mojo parameters
    // ----------------------------------------------------------------------

    /**
     * groupId
     */
    @Parameter(defaultValue = "${project.groupId}", readonly = true, required = true)
    private String groupId;

    /**
     * artifactId
     */
    @Parameter(defaultValue = "${project.artifactId}", readonly = true, required = true)
    private String artifactId;

    /**
     * destGroupId
     */
    @Parameter(defaultValue = "${project.groupId}", required = true)
    private String destGroupId;

    /**
     * destArtifactId
     */
    @Parameter(defaultValue = "${project.artifactId}", required = true)
    private String destArtifactId;

    /**
     * version
     */
    @Parameter(defaultValue = "${project.version}", readonly = true, required = true)
    private String version;

    /**
     * debianVersion
     */
    @Parameter
    private String debianVersion;

    /**
     * directory where the current pom.xml can be found
     */
    @Parameter(defaultValue = "${basedir}", readonly = true, required = true)
    private File basedir;

    /**
     * directory of the jar file
     */
    @Parameter(defaultValue = "${project.build.directory}", readonly = true, required = true)
    private String jarDir;

    /**
     * finalname of the artifact
     */
    @Parameter(defaultValue = "${project.build.finalName}", readonly = true, required = true)
    private String finalName;

    /**
     * Debian directory
     */
    @Parameter(property = "debian.dir")
    private File debianDir;

    /**
     * Debian package (send from command line)
     */
    @Parameter(property = "debian.package")
    private String debianPackage;

    /**
     * Debian package destination (set by xxx.poms file).
     * By default, equals to <code>debianPackage</code> attribute.
     */
    @Parameter(property = "debian.package")
    private String destPackage;

    @Parameter(property = "maven.rules", defaultValue = "maven.rules", required = true)
    private String mavenRules;

    @Parameter(property = "maven.ignoreRules", defaultValue = "maven.ignoreRules", required = true)
    private String mavenIgnoreRules;

    @Parameter(property = "maven.publishedRules", defaultValue = "maven.publishedRules", required = true)
    private String mavenPublishedRules;

    /**
     * root directory of the Maven repository
     */
    @Parameter(defaultValue = "${basedir}", readonly = true)
    private File repoDir;

    /**
     * Install the jar to /usr/share/java if true. Default is true
     */
    @Parameter(property = "install.to.usj", defaultValue = "true")
    private boolean installToUsj = true;

    /**
     * Basename of the JAR inside /usr/share/java
     */
    private String usjName;

    /**
     * Version of the JAR install /usr/share/java
     */
    private String usjVersion;

    /**
     * If true, disable installation of version-less JAR into /usr/share/java
     */
    private boolean noUsjVersionless;

    private String classifier;

    /**
     * The Maven artifacts relocated to this artifact.
     */
    private List<Dependency> relocatedArtifacts;

    // ----------------------------------------------------------------------
    // Public methods
    // ----------------------------------------------------------------------

    public void execute() throws MojoExecutionException {
        try {
            runMojo();
        } catch (IOException e) {
            getLog().error("execution failed", e);
            throw new MojoExecutionException("Failed to execute " + getClass().getSimpleName(), e);
        }
    }

    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public String getDestArtifactId() {
        return destArtifactId;
    }

    public void setDestArtifactId(String destArtifactId) {
        this.destArtifactId = destArtifactId;
    }

    public String getVersion() {
        return version;
    }

    public String getDebianVersion() {
        return debianVersion;
    }

    public void setDebianVersion(String debianVersion) {
        this.debianVersion = debianVersion;
    }

    public File getDebianDir() {
        return debianDir;
    }

    public void setDebianDir(File debianDir) {
        this.debianDir = debianDir;
    }

    public String getDebianPackage() {
        return debianPackage;
    }

    public void setDebianPackage(String debianPackage) {
        this.debianPackage = debianPackage;
    }

    public String getDestPackage() {
        return destPackage;
    }

    public void setDestPackage(String destPackage) {
        this.destPackage = destPackage;
    }

    public String getClassifier() {
        return classifier;
    }

    public void setClassifier(String classifier) {
        this.classifier = classifier;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getDestGroupId() {
        return destGroupId;
    }

    public void setDestGroupId(String destGroupId) {
        this.destGroupId = destGroupId;
    }

    public File getBasedir() {
        return basedir;
    }

    public void setBasedir(File basedir) {
        this.basedir = basedir;
    }

    public String getJarDir() {
        return jarDir;
    }

    public void setJarDir(String jarDir) {
        this.jarDir = jarDir;
    }

    public String getFinalName() {
        return finalName;
    }

    public void setFinalName(String finalName) {
        this.finalName = finalName;
    }

    public String getMavenRules() {
        return mavenRules;
    }

    public void setMavenRules(String mavenRules) {
        this.mavenRules = mavenRules;
    }

    public String getMavenIgnoreRules() {
        return mavenIgnoreRules;
    }

    public void setMavenIgnoreRules(String mavenIgnoreRules) {
        this.mavenIgnoreRules = mavenIgnoreRules;
    }

    public String getMavenPublishedRules() {
        return mavenPublishedRules;
    }

    public void setMavenPublishedRules(String mavenPublishedRules) {
        this.mavenPublishedRules = mavenPublishedRules;
    }

    public File getRepoDir() {
        return repoDir;
    }

    public void setRepoDir(File repoDir) {
        this.repoDir = repoDir;
    }

    public boolean isInstallToUsj() {
        return installToUsj;
    }

    public void setInstallToUsj(boolean installToUsj) {
        this.installToUsj = installToUsj;
    }

    public String getUsjName() {
        return usjName;
    }

    public void setUsjName(String usjName) {
        this.usjName = usjName;
    }

    public String getUsjVersion() {
        return usjVersion;
    }

    public void setUsjVersion(String usjVersion) {
        this.usjVersion = usjVersion;
    }

    public boolean isNoUsjVersionless() {
        return noUsjVersionless;
    }

    public void setNoUsjVersionless(boolean noUsjVersionless) {
        this.noUsjVersionless = noUsjVersionless;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    // ----------------------------------------------------------------------
    // Private methods
    // ----------------------------------------------------------------------

    /**
     * optional destination prefix, empty by default
     */
    protected String packagePath() {
        return "";
    }

    /**
     * returns e.g. /org/debian/maven/debian-maven-plugin/0.1/
     */
    protected final String repoPath() {
        return artifactPath(groupId, artifactId, version);
    }

    /**
     * returns e.g. /org/debian/maven/debian-maven-plugin/0.1/
     */
    protected final String destRepoPath() {
        return artifactPath(destGroupId, destArtifactId, version);
    }

    /**
     * returns e.g. /org/debian/maven/debian-maven-plugin/debian/
     */
    protected final String debianRepoPath() {
        return artifactPath(destGroupId, destArtifactId, debianVersion);
    }

    /**
     * Path to the files of an artifact relatively to the root of the repository.
     */
    private String artifactPath(String groupId, String artifactId, String version) {
        return "/" + groupId.replace('.', '/') + "/" + artifactId + "/" + version + "/";
    }

    /**
     * absolute path to destination dir
     */
    protected String fullRepoPath() {
        return packagePath() + "/usr/share/maven-repo" + destRepoPath();
    }

    /**
     * absolute path to destination dir
     */
    protected String debianFullRepoPath() {
        return packagePath() + "/usr/share/maven-repo" + debianRepoPath();
    }

    /**
     * Name of the pom file in the repository.
     */
    protected String pomName(String artifactId, String version) {
        return artifactId + "-" + version + ".pom";
    }

    private String pomSrcPath() {
        return basedir.getAbsolutePath() + "/pom.xml";
    }

    private String cleanedPomSrcPath() {
        return basedir.getAbsolutePath() + "/target/pom.xml";
    }

    private String cleanedPomPropertiesSrcPath() {
        return basedir.getAbsolutePath() + "/target/pom.properties";
    }

    private String debianPomSrcPath() {
        return basedir.getAbsolutePath() + "/target/pom.debian.xml";
    }

    private String debianPomPropertiesSrcPath() {
        return basedir.getAbsolutePath() + "/target/pom.debian.properties";
    }

    private String pomDestPath() {
        return fullRepoPath() + pomName(destArtifactId, version);
    }

    private String debianPomDestPath() {
        return debianFullRepoPath() + pomName(destArtifactId, debianVersion);
    }

    protected String jarName() {
        String jarName;
        if (finalName != null && finalName.length() > 0) {
            jarName = finalName;
        } else {
            jarName = artifactId + "-" + version;
        }
        if (classifier != null) {
            jarName += "-" + classifier;
        }
        return jarName + ".jar";
    }

    protected String destJarName() {
        if (classifier != null) {
            return destArtifactId + "-" + version + "-" + classifier + ".jar";
        }
        return destArtifactId + "-" + version + ".jar";
    }

    protected String debianJarName() {
        if (classifier != null) {
            return destArtifactId + "-" + debianVersion + "-" + classifier + ".jar";
        }
        return destArtifactId + "-" + debianVersion + ".jar";
    }

    protected final String fullJarName() {
        return jarDir + "/" + jarName();
    }

    protected final String jarDestPath() {
        return fullRepoPath() + destJarName();
    }

    protected final String jarDestRelPath() {
        return "../" + version + "/" + destJarName();
    }

    protected final String debianJarDestPath() {
        return debianFullRepoPath() + debianJarName();
    }

    /**
     * jar file name without version number
     */
    protected final String compatName() {
        return destArtifactId + ".jar";
    }

    protected String compatSharePath() {
        return packagePath() + "/usr/share/java/";
    }

    /**
     * Example: /usr/share/java/xml-apis.jar
     */
    protected String fullCompatPath() {
        return compatSharePath() + destUsjJarName();
    }

    /**
     * Example: /usr/share/java/xml-apis-1.3.04.jar
     */
    protected String versionedFullCompatPath() {
        return compatSharePath() + destUsjVersionnedJarName();
    }

    /**
     * Compute version-less filename for installation into /usr/share/java
     */
    private String destUsjJarName() {
        String usjJarName = "";
        if (usjName != null && usjName.length() > 0) {
            usjJarName += usjName;
        } else {
            usjJarName += destArtifactId;
        }

        return usjJarName + ".jar";
    }

    /**
     * Compute versionned filename for installation into /usr/share/java
     */
    private String destUsjVersionnedJarName() {
        String usjJarName = "";
        if (usjName != null && usjName.length() > 0) {
            usjJarName += usjName;
        } else {
            usjJarName += destArtifactId;
        }

        if (usjVersion != null && usjVersion.length() > 0) {
            usjJarName += "-" + usjVersion;
        } else {
            usjJarName += "-" + version;
        }

        return usjJarName + ".jar";
    }

    /**
     * command for creating the relative symlink
     */
    private void link(String target, String linkName) throws IOException {
        Process process;
        if (System.getProperty("os.name").contains("Windows")) {
            File linkNameFile = new File(linkName).getAbsoluteFile();
            linkNameFile.getParentFile().mkdirs();
            process = new ProcessBuilder().command("cmd", "/C", "mklink", linkNameFile.getAbsolutePath(), target.replace('/', '\\')).start();
        } else {
            process = new ProcessBuilder().command("ln", "-s", target, linkName).start();
        }

        try {
            process.waitFor();
        } catch (InterruptedException e) {
            throw new IOException(e);
        }
    }

    private void mkdir(String path) throws IOException {
        File destinationDirectory = new File(path);
        if (destinationDirectory.isDirectory()) {
            return;
        }
        if (!destinationDirectory.mkdirs()) {
            throw new IOException("cannot create destination directory " + path);
        }
    }

    /**
     * if a jar exists: copy it to the Maven repository
     */
    protected void copyJar() throws IOException {
        File jarFile = new File(fullJarName());
        if (jarFile.exists()) {
            getLog().info("Install jar file into Maven repo: " + jarFile.getAbsolutePath());
            FileUtils.copyFile(jarFile, new File(jarDestPath()));
            if (debianVersion != null && !debianVersion.equals(version)) {
                mkdir(debianFullRepoPath());
                link(jarDestRelPath(), debianJarDestPath());
            }
        }
    }

    /**
     * if a jar exists: copy it to the compat share dir
     */
    private void copyJarToUsj() throws IOException {
        File jarFile = new File(fullJarName());
        if (jarFile.exists()) {
            getLog().info("Install jar for " + artifactId + " into /usr/share/java");
            mkdir(compatSharePath());
            FileUtils.copyFile(jarFile, new File(versionedFullCompatPath()));
            if (!noUsjVersionless) {
                link(destUsjVersionnedJarName(), fullCompatPath());
                link(destUsjVersionnedJarName(), versionedFullCompatPath());
            }
        }
    }

    /**
     * if a jar exists: symlink it from the compat share dir to its targets in the Maven repository
     */
    private void symlinkJar() throws IOException {
        File jarFile = new File(fullJarName());
        if (jarFile.exists()) {
            mkdir(fullRepoPath());
            String targetPath = "";

            targetPath = versionedFullCompatPath();

            link(DirectoryUtils.relativePath(fullRepoPath(), targetPath), jarDestPath());
            if (debianVersion != null && !debianVersion.equals(version)) {
                mkdir(debianFullRepoPath());
                link(DirectoryUtils.relativePath(debianFullRepoPath(), targetPath), debianJarDestPath());
            }
        }
    }

    /**
     * clean the pom.xml
     */
    private void cleanPom() {
        File pomOptionsFile = new File(debianDir, debianPackage + ".poms");
        ListOfPOMs listOfPOMs = new ListOfPOMs(pomOptionsFile);

        // Use the saved pom before cleaning as it was untouched by the transform operation
        String pomPath = pomSrcPath() + ".save";
        File pomFile = new File(pomPath);
        String originalPomPath = pomSrcPath();
        File originalPom = new File(originalPomPath);
        if (!pomFile.exists()) {
            pomFile = originalPom;
            pomPath = originalPomPath;
        }

        String relativePomPath = originalPom.getAbsolutePath();
        relativePomPath = relativePomPath.substring(debianDir.getParentFile().getAbsolutePath().length() + 1);

        POMOptions pomOption = listOfPOMs.getPOMOptions(relativePomPath);

        if (pomOption != null && pomOption.isIgnore()) {
            throw new RuntimeException("POM file " + pomFile + " should be ignored");
        }

        if (pomOption != null) {
            if (pomOption.getDestPackage() != null) {
                destPackage = pomOption.getDestPackage();
            }

            // handle usj-name
            if (pomOption.getUsjName() != null) {
                usjName = pomOption.getUsjName();
            }

            // handle usj-version
            if (pomOption.getUsjVersion() != null) {
                usjVersion = pomOption.getUsjVersion();
            }

            // handle no-usj-versionless
            noUsjVersionless = pomOption.isNoUsjVersionless();

            // handle classifier
            if (pomOption.getClassifier() != null) {
                classifier = pomOption.getClassifier();
            }

            // If package is a java libary (lib.*-java)and is not a maven plugin
            // (lib.*-plugin-java) potentially install to /usr/share/java
            boolean packageIsJavaLib =
                    JAVA_LIB_REGEX.matcher(destPackage).matches() &&
                   !PLUGIN_REGEX.matcher(destPackage).matches();

            // Its also possible to configure USJ install using
            // DEB_MAVEN_INSTALL_TO_USJ which we should honour
            // over auto-detection if its set to 'false'.
            // This is stored in 'installToUsj' on class instance
            // creation
            installToUsj = pomOption.isJavaLib() ||
                           (packageIsJavaLib && installToUsj);

            relocatedArtifacts = pomOption.getRelocatedArtifacts();
        }

        List<String> params = new ArrayList<String>();
        params.add("--keep-pom-version");
        boolean hasPackageVersion = pomOption != null && pomOption.getHasPackageVersion();
        if (hasPackageVersion) {
            params.add("--has-package-version");
        }

        params.add("--package=" + destPackage);
        String mavenRulesPath = new File(debianDir, mavenRules).getAbsolutePath();
        params.add("--rules=" + mavenRulesPath);
        String mavenIgnoreRulesPath = new File(debianDir, mavenIgnoreRules).getAbsolutePath();
        params.add("--ignore-rules=" + mavenIgnoreRulesPath);
        String mavenPublishedRulesPath = new File(debianDir, mavenPublishedRules).getAbsolutePath();
        params.add("--published-rules=" + mavenPublishedRulesPath);

        getLog().info("Cleaning pom file: " + pomFile + " with options:");
        getLog().info("\t--keep-pom-version --package=" + destPackage + (hasPackageVersion ? " --has-package-version" : ""));
        getLog().info("\t--rules=" + mavenRulesPath);
        getLog().info("\t--ignore-rules=" + mavenIgnoreRulesPath);
        getLog().info("\t--published-rules=" + mavenPublishedRulesPath);

        // add optional --no-parent option
        if (pomOption != null && pomOption.isNoParent()) {
            params.add("--no-parent");
            getLog().info("\t--no-parent");
        }

        // add options --keep-elements option
        if (pomOption != null && pomOption.getKeepElements() != null) {
            params.add("--keep-elements=" + pomOption.getKeepElements());
            getLog().info("\t--keep-elements=" + pomOption.getKeepElements());
        }

        params.add(pomFile.getAbsolutePath());
        params.add(cleanedPomSrcPath());
        params.add(cleanedPomPropertiesSrcPath());

        POMCleaner.main(params.toArray(new String[params.size()]));

        Properties pomProperties = new Properties();
        try {
            pomProperties.load(new FileReader(cleanedPomPropertiesSrcPath()));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        destGroupId = pomProperties.getProperty("groupId");
        destArtifactId = pomProperties.getProperty("artifactId");
        debianVersion = pomProperties.getProperty("debianVersion");

        if (debianVersion != null && !debianVersion.equals(version)) {
            params.remove(0);
            params.remove(params.size() - 1);
            params.remove(params.size() - 1);
            params.add(debianPomSrcPath());
            params.add(debianPomPropertiesSrcPath());

            POMCleaner.main(params.toArray(new String[params.size()]));
        }
    }

    /**
     * copy the pom.xml
     */
    protected void copyPom() throws IOException {
        FileUtils.copyFile(new File(cleanedPomSrcPath()), new File(pomDestPath()));
        if (debianVersion != null && !debianVersion.equals(version)) {
            FileUtils.copyFile(new File(debianPomSrcPath()), new File(debianPomDestPath()));
        }
    }

    /**
     * Install the relocated poms
     */
    protected void relocatePoms() throws IOException {
        if (relocatedArtifacts != null) {
            for (Dependency relocated : relocatedArtifacts) {
                relocated.setType(new File(fullJarName()).exists() ? "jar" : "pom");

                getLog().info("Relocating " + relocated.formatCompactNotation());

                File relocatedPath = new File(packagePath() + "/usr/share/maven-repo" + artifactPath(relocated.getGroupId(), relocated.getArtifactId(), relocated.getVersion()));
                File relocatedPom = new File(relocatedPath, pomName(relocated.getArtifactId(), relocated.getVersion()));
                String pom = createRelocationPom(relocated);

                FileUtils.copyStreamToFile(new RawInputStreamFacade(new ByteArrayInputStream(pom.getBytes("UTF-8"))), relocatedPom);
            }
        }
    }

    private String createRelocationPom(Dependency relocateArtifact) {
        return "<project>\n" +
                "  <modelVersion>4.0.0</modelVersion>\n" +
                "  <groupId>" + relocateArtifact.getGroupId() + "</groupId>\n" +
                "  <artifactId>" + relocateArtifact.getArtifactId() + "</artifactId>\n" +
                "  <version>" + relocateArtifact.getVersion() + "</version>\n" +
                "  <packaging>" + relocateArtifact.getType() + "</packaging>\n" +
                "  <properties>\n" +
                "    <debian.package>" + destPackage + "</debian.package>\n" +
                "  </properties>\n" +
                "  <distributionManagement>\n" +
                "    <relocation>\n" +
                "      <groupId>" + destGroupId + "</groupId>\n" +
                "      <artifactId>" + destArtifactId + "</artifactId>\n" +
                "      <version>" + debianVersion + "</version>\n" +
                "    </relocation>\n" +
                "  </distributionManagement>\n" +
                "</project>";
    }

    /**
     * Prepare the destination  directories: remove the directory symlinks that were created
     * by copy-repo.sh if they exist as they point to a directory owned by root and that cannot
     * be modified.
     */
    protected void prepareDestDirs() {
        // Simply try to delete the path. If it's a symlink, it will work, otherwise delete() returns false
        new File(fullRepoPath()).delete();
        new File(debianFullRepoPath()).delete();
    }

    /**
     * do the actual work
     */
    protected void runMojo() throws IOException {
        cleanPom();
        prepareDestDirs();
        copyPom();
        relocatePoms();
        if (installToUsj) {
            copyJarToUsj();
            symlinkJar();
        } else {
            copyJar();
        }
    }
}
