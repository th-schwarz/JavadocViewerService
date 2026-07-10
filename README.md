# JavadocViewerService

## Preface

This project Spring Boot web service that automatically fetches, manages, and serves Javadoc documentation for Maven artifacts.

## Features

- Fetches Javadoc JARs from Maven Central or custom Maven repositories
- Tracks multiple Maven artifacts with version detection
- Serves generated Javadoc via a simple Bootstrap web UI
- Scheduled updates via configurable cron expression
- Persists metadata in an embedded H2 database (Liquibase-managed)

## Requirements

JRE-21 or docker

## Start
### ... with Java

```bash
java -jar jdvc-<version>.jar --spring.config.import=file:./jdvs.yml
```

### .. with Docker

```yaml
services:
  jvs-dockerized:
    image: git.mein-gateway.de/thischwa/javadocviewerservice:develop
    volumes:
      - /opt/javadocviewerservice-dockerized/jdvs.yml:/app/jdvs.yml:ro
      - /opt/javadocviewerservice-dockerized/database:/app/database
      - /opt/javadocviewerservice-dockerized/javadoc-storage:/app/javadoc-storage
    restart: unless-stopped
    ports:
      - "127.0.0.1:8086:8080"
```

Volume mapping:

| Mount | Purpose |
|---|---|
| `jdvs.yml:/app/jdvs.yml:ro` | Injects the repository configuration (which artifacts to track) as read-only — the only file you need to edit |
| `database:/app/database` | Persists the H2 database so tracked version metadata survives restarts and image updates |
| `javadoc-storage:/app/javadoc-storage` | Persists generated Javadoc HTML so it is not lost on container recreation |


## Configuration

### `application.yml`

Key defaults:

| Property | Default                          | Description                        |
|---|----------------------------------|------------------------------------|
| `jdvs.base-dir` | `./javadoc-storage`              | Storage root for generated Javadoc |
| `jdvs.cron` | `0 0/30 * * * ?`                 | Update schedule (every 30 min)     |
| `jdvs.clean-on-start` | `false`                          | Wipe storage and DB on startup     |
| `jdvs.run-on-start` | `false`                          | Run update immediately on startup  |
| `jdvs.maven-central-url` | `https://repo1.maven.org/maven2` | Default Maven repository           |

### `jdvs.yml`

Defines the repositories to track and individual settings (all key defaults can be overridden):

```yaml
jdvs:
  repositories:
    - name: my-lib
      group-id: com.example
      artifact-id: my-lib
      # maven-repo-url: https://custom.repo/maven  # optional, defaults to Maven Central
```
