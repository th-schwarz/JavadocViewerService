package codes.thischwa.jdvs.service;

import codes.thischwa.jdvs.jpa.GitRepositoryRepository;
import codes.thischwa.jdvs.model.GitRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UpdateScheduler {

  private final GitRepositoryRepository repository;
  private final MavenJavadocService mavenJavadocService;

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
    Optional<String> latestVersion = mavenJavadocService.fetchLatestVersion(repo.getName());
    if (latestVersion.isPresent()) {
      String version = latestVersion.get();
      if (!version.equals(repo.getLastTag())) {
        log.info("New version {} found for {}. Generating Javadoc...", version, repo.getName());
        if (mavenJavadocService.generateJavadoc(repo.getName(), version)) {
          repo.setLastTag(version);
          repo.setUpdated(LocalDateTime.now());
          repository.save(repo);
        }
      } else {
        log.info("Repository {} is already up to date ({}).", repo.getName(), version);
      }
    }
  }
}
