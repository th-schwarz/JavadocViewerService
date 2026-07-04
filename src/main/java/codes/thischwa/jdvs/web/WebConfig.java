package codes.thischwa.jdvs.web;

import codes.thischwa.jdvs.config.JvsConfig;
import java.nio.file.Path;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
  private final JvsConfig jvsConfig;

  public WebConfig(JvsConfig jvsConfig) {
    this.jvsConfig = jvsConfig;
  }

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    String javadocPath = Path.of(jvsConfig.getBaseDir(), "javadoc").toAbsolutePath().toUri().toString();
    if (!javadocPath.endsWith("/")) {
      javadocPath += "/";
    }
    registry.addResourceHandler("/**")
        .addResourceLocations(javadocPath);
  }
}
