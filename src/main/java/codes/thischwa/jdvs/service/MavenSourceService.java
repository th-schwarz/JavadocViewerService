package codes.thischwa.jdvs.service;

import codes.thischwa.jdvs.config.GitConfig;
import codes.thischwa.jdvs.config.JdvsConfig;
import codes.thischwa.jdvs.config.JdvsConfig.RepoConfig;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.xml.parsers.DocumentBuilderFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

@Service
@RequiredArgsConstructor
@Slf4j
public class MavenSourceService {

  private final JdvsConfig jdvsConfig;
  private final GitConfig gitConfig;
  private final RestClient restClient = RestClient.create();

  public Optional<String> fetchLatestVersion(String repoName) {
    RepoConfig cfg = findConfig(repoName);
    String metadataUrl = baseUrl(cfg) + "/maven-metadata.xml";
    log.info("Fetching Maven metadata from {}", metadataUrl);
    try {
      String xml = restClient.get().uri(metadataUrl).retrieve().body(String.class);
      Document doc = DocumentBuilderFactory.newInstance()
          .newDocumentBuilder()
          .parse(new InputSource(new StringReader(xml)));
      String version = firstText(doc, "release");
      if (version == null) {
        version = firstText(doc, "latest");
      }
      if (version != null) {
        log.info("Latest version of {} is {}", repoName, version);
        return Optional.of(version);
      }
      log.warn("Could not determine latest version for {}", repoName);
    } catch (Exception e) {
      log.error("Failed to fetch Maven metadata for {}", repoName, e);
    }
    return Optional.empty();
  }

  public boolean generateJavadoc(String repoName, String version) {
    RepoConfig cfg = findConfig(repoName);
    String resolvedVersion = version.endsWith("-SNAPSHOT")
        ? resolveSnapshotVersion(cfg, version)
        : version;
    String jarUrl = baseUrl(cfg) + "/" + version + "/"
        + cfg.getArtifactId() + "-" + resolvedVersion + "-sources.jar";
    log.info("Downloading sources JAR from {}", jarUrl);

    Path workDir = null;
    try {
      workDir = Files.createTempDirectory("jdvs-maven-" + repoName + "-");
      log.info("Created temporary working directory {}", workDir);
      Path srcMainJava = workDir.resolve("src/main/java");
      Files.createDirectories(srcMainJava);
      downloadAndExtract(jarUrl, workDir, srcMainJava);
      copyPomFromMetaInf(workDir, cfg);

      File outputDir = Path.of(jdvsConfig.getBaseDir(), "javadoc", repoName).toFile();
      if (!outputDir.exists()) {
        outputDir.mkdirs();
      }
      return runMavenJavadoc(workDir, cfg, repoName, outputDir.toPath());
    } catch (Exception e) {
      log.error("Failed to generate Javadoc for {} version {}", repoName, version, e);
      return false;
    } finally {
      if (workDir != null) {
        deleteDirectory(workDir);
      }
    }
  }

  private void downloadAndExtract(String jarUrl, Path metaDir, Path srcDir) throws IOException {
    byte[] jarBytes = restClient.get().uri(jarUrl).retrieve().body(byte[].class);
    if (jarBytes == null) {
      throw new IOException("Empty response for " + jarUrl);
    }
    try (ZipInputStream zip = new ZipInputStream(new java.io.ByteArrayInputStream(jarBytes))) {
      ZipEntry entry;
      while ((entry = zip.getNextEntry()) != null) {
        if (!entry.isDirectory()) {
          String name = entry.getName();
          // META-INF goes to metaDir, Java sources go to srcDir
          Path base = name.startsWith("META-INF/") ? metaDir : srcDir;
          Path dest = base.resolve(name).normalize();
          if (!dest.startsWith(metaDir) && !dest.startsWith(srcDir)) {
            throw new IOException("Zip slip detected: " + name);
          }
          Files.createDirectories(dest.getParent());
          Files.copy(zip, dest, StandardCopyOption.REPLACE_EXISTING);
        }
        zip.closeEntry();
      }
    }
  }

  private void copyPomFromMetaInf(Path workDir, RepoConfig cfg) throws IOException {
    Path pomSource = workDir.resolve("META-INF/maven")
        .resolve(cfg.getGroupId())
        .resolve(cfg.getArtifactId())
        .resolve("pom.xml");
    if (!Files.exists(pomSource)) {
      throw new IOException("pom.xml not found in META-INF at " + pomSource);
    }
    Files.copy(pomSource, workDir.resolve("pom.xml"), StandardCopyOption.REPLACE_EXISTING);
    log.info("Copied POM from META-INF to working directory");
  }

  private boolean runMavenJavadoc(Path workDir, RepoConfig cfg, String repoName, Path outputDir)
      throws IOException, InterruptedException {
    Path mavenLocalRepo = Path.of(jdvsConfig.getBaseDir(), "maven-repo").toAbsolutePath();
    Files.createDirectories(mavenLocalRepo);

    // create empty sonar-project.properties so properties-maven-plugin doesn't fail
    Files.createFile(workDir.resolve("sonar-project.properties"));

    List<String> cmd = new ArrayList<>(List.of(
        "mvn", "-U", "--no-transfer-progress",
        "clean", "javadoc:javadoc",
        "-Djacoco.skip=true",
        "-DskipTests",
        "-Dsonar.skip=true",
        "-Dmaven.javadoc.skip=false",
        "-Dcheckstyle.skip=true",
        "-Dlombok.delombok.skip=true",
        "-Dmaven.repo.local=" + mavenLocalRepo
    ));
    if (cfg.getMavenRepoUrl() != null) {
      cmd.add("-s");
      cmd.add(writeSettingsXml(workDir, cfg.getMavenRepoUrl()).toAbsolutePath().toString());
    }

    ProcessBuilder pb = new ProcessBuilder(cmd);
    pb.directory(workDir.toFile());
    pb.redirectErrorStream(true);
    Process process = pb.start();
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
      String line;
      while ((line = reader.readLine()) != null) {
        log.debug("[MVN] {}", line);
      }
    }
    int exitCode = process.waitFor();
    if (exitCode != 0) {
      log.error("mvn javadoc:javadoc failed with exit code {} for {}", exitCode, repoName);
      return false;
    }

    Path apidocsDir = gitConfig.getJavadocPaths().stream()
        .map(workDir::resolve)
        .filter(Files::exists)
        .findFirst()
        .orElse(null);
    if (apidocsDir == null) {
      Path targetDir = workDir.resolve("target");
      if (Files.exists(targetDir)) {
        try (var s = Files.walk(targetDir, 4)) {
          s.filter(p -> p.toString().endsWith(".html"))
              .findFirst()
              .ifPresentOrElse(
                  p -> log.error("Javadoc not at expected path. Found HTML at: {}", p),
                  () -> log.error("No HTML files found under {}", targetDir)
              );
        }
      } else {
        log.error("target/ directory does not exist in {}", workDir);
      }
      return false;
    }
    log.debug("Found apidocs directory at: {}", apidocsDir);
    copyDirectory(apidocsDir, outputDir);
    log.info("Javadoc successfully generated for {} in {}", repoName, outputDir);
    return true;
  }

  private Path writeSettingsXml(Path workDir, String repoUrl) throws IOException {
    Path settingsFile = workDir.resolve("settings.xml");
    String xml = """
        <?xml version="1.0" encoding="UTF-8"?>
        <settings>
          <profiles>
            <profile>
              <id>jdvs-repos</id>
              <repositories>
                <repository>
                  <id>custom</id>
                  <url>""" + repoUrl + """
                  </url>
                  <snapshots><enabled>true</enabled></snapshots>
                </repository>
              </repositories>
            </profile>
          </profiles>
          <activeProfiles>
            <activeProfile>jdvs-repos</activeProfile>
          </activeProfiles>
        </settings>
        """;
    Files.writeString(settingsFile, xml);
    return settingsFile;
  }

  private void copyDirectory(Path source, Path target) throws IOException {
    try (var stream = Files.walk(source)) {
      stream.forEach(path -> {
        try {
          Path dest = target.resolve(source.relativize(path));
          if (Files.isDirectory(path)) {
            Files.createDirectories(dest);
          } else {
            Files.createDirectories(dest.getParent());
            Files.copy(path, dest, StandardCopyOption.REPLACE_EXISTING);
          }
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      });
    }
  }

  private String resolveSnapshotVersion(RepoConfig cfg, String version) {
    String metadataUrl = baseUrl(cfg) + "/" + version + "/maven-metadata.xml";
    log.info("Resolving SNAPSHOT version from {}", metadataUrl);
    try {
      String xml = restClient.get().uri(metadataUrl).retrieve().body(String.class);
      Document doc = DocumentBuilderFactory.newInstance()
          .newDocumentBuilder()
          .parse(new InputSource(new StringReader(xml)));
      NodeList nodes = doc.getElementsByTagName("snapshotVersion");
      for (int i = 0; i < nodes.getLength(); i++) {
        org.w3c.dom.Element el = (org.w3c.dom.Element) nodes.item(i);
        String classifier = firstText(el, "classifier");
        String ext = firstText(el, "extension");
        String value = firstText(el, "value");
        if ("sources".equals(classifier) && "jar".equals(ext) && value != null) {
          log.info("Resolved SNAPSHOT version to {}", value);
          return value;
        }
      }
    } catch (Exception e) {
      log.warn("Could not resolve SNAPSHOT version for {}, falling back to {}",
          cfg.getArtifactId(), version, e);
    }
    return version;
  }

  private String baseUrl(RepoConfig cfg) {
    String repoUrl = cfg.getMavenRepoUrl() != null
        ? cfg.getMavenRepoUrl()
        : jdvsConfig.getMavenCentralUrl();
    String groupPath = cfg.getGroupId().replace('.', '/');
    return repoUrl + "/" + groupPath + "/" + cfg.getArtifactId();
  }

  private RepoConfig findConfig(String repoName) {
    return jdvsConfig.getRepositories().stream()
        .filter(r -> repoName.equals(r.getName()))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("No config found for: " + repoName));
  }

  private String firstText(Document doc, String tagName) {
    NodeList nodes = doc.getElementsByTagName(tagName);
    return nodes.getLength() > 0 ? nodes.item(0).getTextContent() : null;
  }

  private String firstText(org.w3c.dom.Element parent, String tagName) {
    NodeList nodes = parent.getElementsByTagName(tagName);
    return nodes.getLength() > 0 ? nodes.item(0).getTextContent() : null;
  }

  private void deleteDirectory(Path dir) {
    try (var stream = Files.walk(dir)) {
      stream.sorted(java.util.Comparator.reverseOrder())
          .map(Path::toFile)
          .forEach(File::delete);
    } catch (IOException e) {
      log.warn("Failed to clean temp directory {}", dir, e);
    }
  }
}
