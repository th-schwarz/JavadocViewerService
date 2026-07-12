package codes.thischwa.jdvs.service;

import codes.thischwa.jdvs.model.config.JdvsConfig;
import codes.thischwa.jdvs.model.config.JdvsConfig.RepoConfig;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

@Service
@RequiredArgsConstructor
@Slf4j
public class MavenJavadocService {

  private final JdvsConfig jdvsConfig;
  private final RestClient restClient;

  public Optional<String> fetchLatestVersion(String repoName) {
    RepoConfig cfg = findConfig(repoName);
    String metadataUrl = baseUrl(cfg) + "/maven-metadata.xml";
    log.info("Fetching Maven metadata from {}", metadataUrl);
    try {
      String xml = restClient.get().uri(metadataUrl).retrieve().body(String.class);
      Document doc = parseXml(xml);
      Optional<String> version = resolveVersionTag(doc);
      if (version.isPresent()) {
        log.info("Latest version of {} is {}", repoName, version.get());
        return version;
      }
      log.warn("Could not determine latest version for {}", repoName);
    } catch (ParserConfigurationException | SAXException | IOException e) {
      log.error("Failed to parse Maven metadata for {}", repoName, e);
    } catch (RestClientException e) {
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
        + cfg.getArtifactId() + "-" + resolvedVersion + "-javadoc.jar";
    log.info("Downloading Javadoc JAR from {}", jarUrl);

    Path outputDir = Path.of(jdvsConfig.getBaseDir(), "javadoc", repoName);
    try {
      Files.createDirectories(outputDir);
      extractJavadocJar(jarUrl, outputDir);
      log.info("Javadoc successfully extracted for {} to {}", repoName, outputDir);
      return true;
    } catch (IOException | RestClientException e) {
      log.error("Failed to generate Javadoc for {} version {}", repoName, version, e);
      return false;
    }
  }

  private void extractJavadocJar(String jarUrl, Path outputDir) throws IOException {
    byte[] jarBytes = restClient.get().uri(jarUrl).retrieve().body(byte[].class);
    if (jarBytes == null) {
      throw new IOException("Empty response for " + jarUrl);
    }
    Path safeOutputDir = outputDir.toAbsolutePath().normalize();
    try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(jarBytes))) {
      ZipEntry entry;
      while ((entry = zip.getNextEntry()) != null) {
        String name = entry.getName();
        if (!entry.isDirectory() && !name.startsWith("META-INF/")) {
          Path dest = safeOutputDir.resolve(name).normalize();
          if (!dest.startsWith(safeOutputDir)) {
            throw new IOException("Zip slip detected: " + name);
          }
          Files.createDirectories(dest.getParent());
          Files.copy(zip, dest, StandardCopyOption.REPLACE_EXISTING);
        }
        zip.closeEntry();
      }
    }
  }

  private String resolveSnapshotVersion(RepoConfig cfg, String version) {
    String metadataUrl = baseUrl(cfg) + "/" + version + "/maven-metadata.xml";
    log.info("Resolving SNAPSHOT version from {}", metadataUrl);
    try {
      String xml = restClient.get().uri(metadataUrl).retrieve().body(String.class);
      Document doc = parseXml(xml);
      NodeList nodes = doc.getElementsByTagName("snapshotVersion");
      for (int i = 0; i < nodes.getLength(); i++) {
        org.w3c.dom.Element el = (org.w3c.dom.Element) nodes.item(i);
        String classifier = firstText(el.getElementsByTagName("classifier"));
        String ext = firstText(el.getElementsByTagName("extension"));
        String value = firstText(el.getElementsByTagName("value"));
        if ("javadoc".equals(classifier) && "jar".equals(ext) && value != null) {
          log.info("Resolved SNAPSHOT version to {}", value);
          return value;
        }
      }
    } catch (ParserConfigurationException | SAXException | IOException e) {
      log.warn("Could not parse SNAPSHOT metadata for {}, falling back to {}",
          cfg.getArtifactId(), version, e);
    } catch (RestClientException e) {
      log.warn("Could not fetch SNAPSHOT metadata for {}, falling back to {}",
          cfg.getArtifactId(), version, e);
    }
    return version;
  }

  private String baseUrl(RepoConfig cfg) {
    String groupPath = cfg.getGroupId().replace('.', '/');
    return jdvsConfig.getEffectiveMavenRepoUrl(cfg) + "/" + groupPath + "/" + cfg.getArtifactId();
  }

  private RepoConfig findConfig(String repoName) {
    return jdvsConfig.getRepositories().stream()
        .filter(r -> repoName.equals(r.getName()))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("No config found for: " + repoName));
  }

  private static Optional<String> resolveVersionTag(Document doc) {
    return Optional.ofNullable(firstText(doc.getElementsByTagName("release")))
        .or(() -> Optional.ofNullable(firstText(doc.getElementsByTagName("latest"))));
  }

  private static String firstText(NodeList nodes) {
    return nodes.getLength() > 0 ? nodes.item(0).getTextContent() : null;
  }

  private static Document parseXml(String xml)
      throws ParserConfigurationException, SAXException, IOException {
    return DocumentBuilderFactory.newInstance()
        .newDocumentBuilder()
        .parse(new InputSource(new StringReader(xml)));
  }
}
