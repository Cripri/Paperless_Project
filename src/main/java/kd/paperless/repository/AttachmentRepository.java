package kd.paperless.repository;

import kd.paperless.entity.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {

  List<Attachment> findByTargetTypeAndTargetIdOrderByFileIdAsc(String targetType, Long targetId);

  long deleteByTargetTypeAndTargetId(String targetType, Long targetId);

  Optional<Attachment> findTopByTargetTypeAndTargetIdOrderByFileIdDesc(String targetType, Long targetId);
}