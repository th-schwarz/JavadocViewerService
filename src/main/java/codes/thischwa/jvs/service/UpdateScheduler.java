package codes.thischwa.jvs.service;

import codes.thischwa.jvs.model.GitRepository;
import codes.thischwa.jvs.repository.GitRepositoryRepository;
import java.io.File;
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
  private final GitService gitService;
  private final JavadocService javadocService;


  @Scheduled(cron = "${jvs.cron}")
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
