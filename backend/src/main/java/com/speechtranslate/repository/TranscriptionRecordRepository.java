package com.speechtranslate.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.speechtranslate.entity.TranscriptionRecord;
import java.util.List;

@Repository
public interface TranscriptionRecordRepository extends JpaRepository<TranscriptionRecord, Long> {
    List<TranscriptionRecord> findBySessionId(String sessionId);
    List<TranscriptionRecord> findBySessionIdOrderByCreatedAtDesc(String sessionId);
}