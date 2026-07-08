package codes.thischwa.jdvs.web;

import codes.thischwa.jdvs.jpa.GitRepositoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequiredArgsConstructor
public class BadgeController {

  private static final String BADGE_URL_PREFIX =
      "https://img.shields.io/badge/javadoc%40JdVS-";

  private static final String BADGE_URL_SUFFIX = "-green";

  private final GitRepositoryRepository repository;

  @GetMapping("/badge/{repositoryName}")
  public String badge(@PathVariable String repositoryName) {
    String tag =
        repository
            .findByName(repositoryName)
            .map(r -> r.getLastTag() != null ? r.getLastTag() : "unknown")
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Repository not found: " + repositoryName));
    return "redirect:" + BADGE_URL_PREFIX + tag + BADGE_URL_SUFFIX;
  }
}
