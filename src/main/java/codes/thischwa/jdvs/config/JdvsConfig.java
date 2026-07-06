package codes.thischwa.jdvs.config;

import java.util.List;
import java.util.Objects;
import lombok.Data;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "jdvs")
@Data
public class JdvsConfig {
  private String baseDir;
  private boolean cleanOnStart;
  private boolean runOnStart;
  private String mavenCentralUrl;
  private List<RepoConfig> repositories;

  @Data
  public static class RepoConfig {
    private String name;
    private String groupId;
    private String artifactId;
    private String mavenRepoUrl;
  }
}
