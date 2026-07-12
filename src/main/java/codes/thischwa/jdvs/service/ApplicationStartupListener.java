package codes.thischwa.jdvs.service;

import codes.thischwa.jdvs.config.RepoConfigLoader;
import codes.thischwa.jdvs.jpa.GitRepositoryRepository;
import codes.thischwa.jdvs.model.config.JdvsConfig;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ApplicationStartupListener {

  private final GitRepositoryRepository repository;
  private final JdvsConfig jdvsConfig;
  private final RepoConfigLoader repoConfigLoader;
  private final UpdateScheduler updateScheduler;

  @EventListener(ApplicationReadyEvent.class)
  public void onApplicationReady() {
    if (jdvsConfig.isCleanOnStart()) {
      log.info("'jdvs.clean-on-start' is enabled – cleaning base directory and database.");
      cleanBaseDir();
      repository.deleteAll();
    }
    repoConfigLoader.loadConfig();
    if (jdvsConfig.isRunOnStart()) {
      log.info("'jdvs.run-on-start' is enabled – running initial update.");
      updateScheduler.updateAll();
    }
  }

  private void cleanBaseDir() {
    Path baseDir = Path.of(jdvsConfig.getBaseDir());
    if (!Files.exists(baseDir)) {
      return;
    }
    try (var stream = Files.walk(baseDir)) {
      stream.sorted(Comparator.reverseOrder()).forEach(path -> {
        try {
          Files.delete(path);
        } catch (IOException e) {
          log.warn("Failed to delete path: {}", path, e);
        }
      });
      log.info("Deleted base directory: {}", baseDir);
    } catch (IOException e) {
      log.error("Failed to walk base directory: {}", baseDir, e);
    }
  }
}
