package kd.paperless.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import kd.paperless.entity.Sinmungo;

@Repository
public interface SinmungoRepository extends JpaRepository<Sinmungo, Long> {

  @Query("""
      SELECT s
      FROM Sinmungo s
      WHERE (:status IS NULL OR s.status = :status)
        AND (:keyword IS NULL OR (
             (:searchType = 'title' AND LOWER(s.title) LIKE LOWER(CONCAT('%', :keyword, '%')))

             OR (:searchType = 'content' AND
                 LOWER(function('dbms_lob.substr', s.content, 4000, 1))
                 LIKE LOWER(CONCAT('%', :keyword, '%')))

             OR (:searchType = 'all' AND (
                   LOWER(s.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(function('dbms_lob.substr', s.content, 4000, 1))
                      LIKE LOWER(CONCAT('%', :keyword, '%'))
                 ))
        ))
      """)
  Page<Sinmungo> search(@Param("keyword") String keyword,
      @Param("status") String status,
      @Param("searchType") String searchType,
      Pageable pageable);

  @Query("select max(s.smgId) from Sinmungo s where s.smgId < :id")
  Long findPrevId(@Param("id") Long id);

  @Query("select min(s.smgId) from Sinmungo s where s.smgId > :id")
  Long findNextId(@Param("id") Long id);

}