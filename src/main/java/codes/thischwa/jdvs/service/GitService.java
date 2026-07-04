package codes.thischwa.jdvs.service;

import codes.thischwa.jdvs.config.JdvsConfig;
import codes.thischwa.jdvs.model.GitRepository;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class GitService {
  private final JdvsConfig jdvsConfig;

  public Optional<String> updateAndCheckoutLatestTag(GitRepository repoEntity) {
    Path repoPath = Path.of(jdvsConfig.getBaseDir(), "repos", repoEntity.getName());
    try {
      Git git;
      if (Files.exists(repoPath)) {
        git = Git.open(repoPath.toFile());
        git.fetch().setCheckFetchedObjects(true).call();
      } else {
        git = Git.cloneRepository()
            .setURI(repoEntity.getUrl())
            .setDirectory(repoPath.toFile())
            .setCloneAllBranches(true)
            .setNoCheckout(false)
            .call();
      }

      List<Ref> tags = git.tagList().call();
      Optional<Ref> latestVTag = tags.stream()
          .filter(ref -> ref.getName().startsWith("refs/tags/v"))
          .max(Comparator.comparing(Ref::getName));

      if (latestVTag.isPresent()) {
        String tagName = latestVTag.get().getName().substring("refs/tags/".length());
        git.checkout().setName(tagName).call();
        log.info("Repository {} checked out at tag {}.", repoEntity.getName(), tagName);
        return Optional.of(tagName);
      } else {
        log.warn("No v* tag found for repository {}.", repoEntity.getName());
      }
    } catch (IOException | GitAPIException e) {
      log.error("Error processing repository " + repoEntity.getName(), e);
    }
    return Optional.empty();
  }

  public File getRepoDirectory(String name) {
    return Path.of(jdvsConfig.getBaseDir(), "repos", name).toFile();
  }
}
