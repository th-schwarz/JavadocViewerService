package codes.thischwa.jdvs.web;

import codes.thischwa.jdvs.config.JdvsConfig;
import java.nio.file.Path;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
  private final JdvsConfig jdvsConfig;

  public WebConfig(JdvsConfig jdvsConfig) {
    this.jdvsConfig = jdvsConfig;
  }

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    String javadocPath = Path.of(jdvsConfig.getBaseDir(), "javadoc").toAbsolutePath().toUri().toString();
    if (!javadocPath.endsWith("/")) {
      javadocPath += "/";
    }
    registry.addResourceHandler("/**")
        .addResourceLocations(javadocPath);
  }
}
