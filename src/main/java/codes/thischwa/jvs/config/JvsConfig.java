package codes.thischwa.jvs.config;

import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "jvs")
@Data
public class JvsConfig {
  private String configPath;
  private String baseDir;
  private List<RepoConfig> repositories;

  @Data
  public static class RepoConfig {
    private String name;
    private String url;
  }
}
