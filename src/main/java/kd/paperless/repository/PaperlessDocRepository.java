package kd.paperless.repository;

import kd.paperless.entity.PaperlessDoc;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaperlessDocRepository extends JpaRepository<PaperlessDoc, Long> {
}
