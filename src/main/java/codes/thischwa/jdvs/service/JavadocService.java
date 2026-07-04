package codes.thischwa.jdvs.service;

import codes.thischwa.jdvs.config.JvsConfig;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class JavadocService {
  private final JvsConfig jvsConfig;

  public boolean generateJavadoc(String projectName, File repoDir) {
    File outputDir = Path.of(jvsConfig.getBaseDir(), "javadoc", projectName).toFile();
    if (!outputDir.exists()) {
      outputDir.mkdirs();
    }

    // Try Maven first if a pom.xml is present
    if (new File(repoDir, "pom.xml").exists()) {
      boolean success = runMavenJavadoc(repoDir, outputDir);
      if (success) {
        copyGeneratedJavadoc(repoDir, outputDir);
      }
      return success;
    } else {
      log.warn("No pom.xml found in {}. Manual javadoc generation is not implemented.", projectName);
      return false;
    }
  }

  private void copyGeneratedJavadoc(File repoDir, File targetDir) {
    // Possible standard paths for Javadoc
    String[] possiblePaths = {
        "target/reports/apidocs",
        "target/site/apidocs",
        "target/apidocs"
    };

    for (String relPath : possiblePaths) {
      File sourceDir = new File(repoDir, relPath);
      if (sourceDir.exists() && sourceDir.isDirectory()) {
        log.info("Found Javadoc in {}, copying to {}", sourceDir.getAbsolutePath(), targetDir.getAbsolutePath());
        try {
          copyDirectory(sourceDir.toPath(), targetDir.toPath());
          return;
        } catch (IOException e) {
          log.error("Error copying Javadoc from {} to {}", sourceDir.getAbsolutePath(), targetDir.getAbsolutePath(), e);
        }
      }
    }
    log.warn("No generated Javadoc files found in the standard directories of {}.", repoDir.getName());
  }

  private void copyDirectory(Path source, Path target) throws IOException {
    try (Stream<Path> stream = Files.walk(source)) {
      stream.forEach(path -> {
        try {
          Path dest = target.resolve(source.relativize(path));
          if (Files.isDirectory(path)) {
            if (!Files.exists(dest)) {
              Files.createDirectories(dest);
            }
          } else {
            Files.copy(path, dest, StandardCopyOption.REPLACE_EXISTING);
          }
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      });
    }
  }

  private boolean runMavenJavadoc(File repoDir, File outputDir) {
    ProcessBuilder pb = new ProcessBuilder(
        "mvn", "javadoc:javadoc",
        "-Dmaven.javadoc.failOnError=false",
        "--no-transfer-progress",
        "-Dlombok.delombok.skip=true",
        "-Dcheckstyle.skip=true",
        "-Djacoco.skip=true",
        "-DskipTests"
    );
    pb.directory(repoDir);
    pb.redirectErrorStream(true);

    try {
      Process process = pb.start();
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
        String line;
        while ((line = reader.readLine()) != null) {
          log.info("[MAVEN] {}", line);
        }
      }
      int exitCode = process.waitFor();
      if (exitCode == 0) {
        log.info("Javadoc successfully generated in {}", outputDir.getAbsolutePath());
        return true;
      } else {
        log.error("Maven javadoc failed with exit code {}", exitCode);
      }
    } catch (IOException | InterruptedException e) {
      log.error("Error executing Maven javadoc", e);
    }
    return false;
  }
}
