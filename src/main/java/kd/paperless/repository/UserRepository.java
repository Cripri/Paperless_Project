package kd.paperless.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import kd.paperless.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByLoginId(String loginId);
    Optional<User> findByLoginId(String loginId);
    
    Optional<User> findByUserNameAndEmail(String userName, String email);
    Optional<User> findByLoginIdAndEmail(String loginId, String email);

    List<User> findAllByIdIn(List<Long> ids);
}
