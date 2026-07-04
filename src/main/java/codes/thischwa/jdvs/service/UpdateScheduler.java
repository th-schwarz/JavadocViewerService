package codes.thischwa.jdvs.service;

import codes.thischwa.jdvs.config.JdvsConfig;
import codes.thischwa.jdvs.jpa.GitRepositoryRepository;
import codes.thischwa.jdvs.model.GitRepository;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UpdateScheduler {
  private final GitRepositoryRepository repository;
  private final GitService gitService;
  private final JavadocService javadocService;
  private final JdvsConfig jdvsConfig;
  private final RepoConfigLoader repoConfigLoader;

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
      updateAll();
    }
  }

  private void cleanBaseDir() {
    Path baseDir = Path.of(jdvsConfig.getBaseDir());
    if (!Files.exists(baseDir)) {
      return;
    }
    try (var stream = Files.walk(baseDir)) {
      stream.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
      log.info("Deleted base directory: {}", baseDir);
    } catch (IOException e) {
      log.error("Failed to delete base directory: {}", baseDir, e);
    }
  }

  @Scheduled(cron = "${jdvs.cron}")
  public void updateAll() {
    log.info("Starting scheduled update of repositories...");
    List<GitRepository> repos = repository.findAll();
    for (GitRepository repo : repos) {
      updateRepo(repo);
    }
  }

  private void updateRepo(GitRepository repo) {
    log.info("Checking repository: {}", repo.getName());
    Optional<String> latestTag = gitService.updateAndCheckoutLatestTag(repo);
    if (latestTag.isPresent()) {
      String tag = latestTag.get();
      if (!tag.equals(repo.getLastTag())) {
        log.info("New tag {} found for {}. Generating Javadoc...", tag, repo.getName());
        File repoDir = gitService.getRepoDirectory(repo.getName());
        if (javadocService.generateJavadoc(repo.getName(), repoDir)) {
          repo.setLastTag(tag);
          repo.setUpdated(LocalDateTime.now());
          repository.save(repo);
        }
      } else {
        log.info("Repository {} is already up to date ({}).", repo.getName(), tag);
      }
    }
  }
}
