package codes.thischwa.jdvs.service;

import codes.thischwa.jdvs.config.JdvsConfig;
import codes.thischwa.jdvs.jpa.GitRepositoryRepository;
import codes.thischwa.jdvs.model.GitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RepoConfigLoader {
  private final JdvsConfig jdvsConfig;
  private final GitRepositoryRepository repository;

  public void loadConfig() {
    if (jdvsConfig.getRepositories() != null) {
      for (JdvsConfig.RepoConfig repoCfg : jdvsConfig.getRepositories()) {
        if (repository.findByName(repoCfg.getName()).isEmpty()) {
          GitRepository repo = new GitRepository();
          repo.setName(repoCfg.getName());
          repository.save(repo);
          log.info("Repository {} added from configuration.", repoCfg.getName());
        }
      }
    }
  }
}
