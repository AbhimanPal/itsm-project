package com.itsm.itsm.audit;

import com.itsm.itsm.model.Incident;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditRecordRepository extends JpaRepository<AuditRecord, Long> {

    List<AuditRecord> findByIncidentOrderByTimestampAscIdAsc(Incident incident);

    List<AuditRecord> findAllByOrderByTimestampDescIdDesc();
}
