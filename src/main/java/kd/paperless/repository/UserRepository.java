package kd.paperless.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import kd.paperless.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByLoginId(String loginId);
    Optional<User> findByLoginId(String loginId);
    
    // 나중에 암호화 적용하면 아래에 이거 삭제
    Optional<User> findByLoginIdAndPasswordHash(String loginId, String passwordHash);
    
    Optional<User> findByUserNameAndEmail(String userName, String email);
    Optional<User> findByLoginIdAndEmail(String loginId, String email);
}
