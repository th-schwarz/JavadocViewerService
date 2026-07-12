package codes.thischwa.jdvs.web;

import codes.thischwa.jdvs.model.config.JdvsConfig;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {
  private final JdvsConfig jdvsConfig;

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
