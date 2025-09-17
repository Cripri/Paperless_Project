package kd.paperless.repository;

import kd.paperless.entity.PaperlessDoc;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;

import java.util.Date;

public interface PaperlessDocRepository extends JpaRepository<PaperlessDoc, Long> {

  Page<PaperlessDoc> findByUserId(Long userId, Pageable pageable);

  Page<PaperlessDoc> findByStatus(String status, Pageable pageable);

  Page<PaperlessDoc> findByDocType(String docType, Pageable pageable);

  @Modifying
  @Query("update PaperlessDoc p set p.status = :status, p.processedAt = :processedAt where p.plId = :plId")
  int updateStatus(Long plId, String status, Date processedAt);
}
