package codes.thischwa.jvs.repository;

import codes.thischwa.jvs.model.GitRepository;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GitRepositoryRepository extends JpaRepository<GitRepository, Long> {
  Optional<GitRepository> findByName(String name);
}
