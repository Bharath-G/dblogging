package com.learning.logging.repo;

import com.learning.logging.entity.Log;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.UUID;

public interface LogRepository extends JpaRepository<Log, UUID> {
    
    @Modifying
    @Query("DELETE FROM Log l WHERE l.createdAt < :cutoffDate")
    void deleteOlderThan(@Param("cutoffDate") Instant cutoffDate);
}