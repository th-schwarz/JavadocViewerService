package codes.thischwa.jdvs;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@Slf4j
public class JdvsApplication {
  public static void main(String[] args) {

    try {
      SpringApplication.run(JdvsApplication.class, args);
    } catch (Exception e) {
      log.error("Unexpected exception, Spring Boot stops! Message: {}", e.getMessage());
      System.exit(10);
    }
  }
}
