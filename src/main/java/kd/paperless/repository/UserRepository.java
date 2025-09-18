package kd.paperless.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import kd.paperless.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByLoginId(String loginId);
    Optional<User> findByLoginId(String loginId);
    Optional<User> findByLoginIdAndPasswordHash(String loginId, String passwordHash);
}
