package codes.thischwa.jdvs.config;

import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "jdvs")
@Data
public class JdvsConfig {
  private String baseDir;
  private boolean cleanOnStart;
  private boolean runOnStart;
  private List<RepoConfig> repositories;

  @Data
  public static class RepoConfig {
    private String name;
    private String url;
  }
}
