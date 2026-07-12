package codes.thischwa.jdvs.model.config;

import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "git")
@Data
public class GitConfig {
  private List<String> javadocPaths;
}
