package kd.paperless.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import kd.paperless.entity.Notice;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

  @Query(value = """
        SELECT n
        FROM Notice n
        LEFT JOIN FETCH n.admin a
        WHERE n.status = 'POSTED'
        AND targetAudience = 'USER'
        ORDER BY CASE WHEN n.isPinned = 'Y' THEN 0 ELSE 1 END,
                 n.createdAt DESC, n.noticeId DESC
      """, countQuery = """
        SELECT COUNT(n)
        FROM Notice n
        WHERE n.status = 'POSTED'
        AND targetAudience = 'USER'
      """)
  Page<Notice> list(Pageable pageable);

  @Query(value = "select max(n.notice_id) from notice n where n.status='POSTED' and n.notice_id < :id", nativeQuery = true)
  Long findPrevId(@Param("id") Long id);

  @Query(value = "select min(n.notice_id) from notice n where n.status='POSTED' and n.notice_id > :id", nativeQuery = true)
  Long findNextId(@Param("id") Long id);

  @Query("""
        SELECT n
        FROM Notice n
        LEFT JOIN FETCH n.admin a
        WHERE n.status = 'POSTED'
        AND targetAudience = 'USER'
        ORDER BY CASE WHEN n.isPinned = 'Y' THEN 0 ELSE 1 END,
                 n.createdAt DESC, n.noticeId DESC
      """)
  List<Notice> findPostedTop(Pageable pageable); // Page 말고 List로
}