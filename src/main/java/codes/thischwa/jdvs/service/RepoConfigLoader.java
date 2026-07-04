package codes.thischwa.jdvs.service;

import codes.thischwa.jdvs.config.JvsConfig;
import codes.thischwa.jdvs.model.GitRepository;
import codes.thischwa.jdvs.repository.GitRepositoryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RepoConfigLoader {
  private final JvsConfig jvsConfig;
  private final GitRepositoryRepository repository;

  @PostConstruct
  public void loadConfig() {
    File configFile = new File(jvsConfig.getConfigPath());
    if (!configFile.exists()) {
      log.info("Configuration file {} not found.", jvsConfig.getConfigPath());
      return;
    }

    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    try {
      JvsConfig yamlConfig = mapper.readValue(configFile, JvsConfig.class);
      if (yamlConfig.getRepositories() != null) {
        for (JvsConfig.RepoConfig repoCfg : yamlConfig.getRepositories()) {
          if (repository.findByName(repoCfg.getName()).isEmpty()) {
            GitRepository repo = new GitRepository();
            repo.setName(repoCfg.getName());
            repo.setUrl(repoCfg.getUrl());
            repository.save(repo);
            log.info("Repository {} added from configuration.", repoCfg.getName());
          }
        }
      }
    } catch (IOException e) {
      log.error("Error reading configuration file", e);
    }
  }
}
