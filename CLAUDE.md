# JavadocViewerService

## Tech Stack

- **Java 21**
- **Spring Boot 4.x** (spring-boot-starter-parent)
  - Spring MVC (spring-boot-starter-web)
  - Spring Data JPA (spring-boot-starter-data-jpa)
  - Thymeleaf (spring-boot-starter-thymeleaf)
  - Bean Validation (spring-boot-starter-validation)
  - Liquibase (spring-boot-starter-liquibase)
- **H2** — file-based embedded database (`./database/jdvsdb`)
- **Lombok** — boilerplate reduction (`@Data`, `@RequiredArgsConstructor`, `@Slf4j`)
- **JGit 7.x** — Git operations (clone, fetch, tag resolution)
- **Jackson YAML** — parsing of `jdvs.yml` config file
- **Maven** — build tool

## Configuration

- `application.yml` — base configuration (datasource, JPA, default jdvs properties)
- `jdvs.yml` — external runtime config (repositories, property overrides); imported via `spring.config.import`

## Package Structure

- `codes.thischwa.jdvs.config` — `@ConfigurationProperties` beans
- `codes.thischwa.jdvs.service` — business logic (Git, Javadoc generation, scheduling)
- `codes.thischwa.jdvs.jpa` — Spring Data repositories
- `codes.thischwa.jdvs.model` — JPA entities
- `codes.thischwa.jdvs.web` — MVC controllers and `WebMvcConfigurer`

## Coding

The generated code follows the principles of clean architecture and follows the SOLID principles. It is designed to be modular, testable, and maintainable. The code is written in a way that promotes readability and reduces complexity. It uses modern Java features and best practices to ensure efficient and reliable operation. To formate the code use che checkstyle definition in [google_custom_checks.xml](src/checkstyle/google_custom_checks.xml). Each changing of code must be verified with compiling.