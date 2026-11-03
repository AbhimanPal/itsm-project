package com.itsm.itsm.report;

import com.itsm.itsm.assignment.AssignmentService;
import com.itsm.itsm.model.*;
import com.itsm.itsm.repository.ITServiceRepository;
import com.itsm.itsm.repository.IncidentRepository;
import com.itsm.itsm.repository.TechnicianRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

// Counts incidents for the admin dashboard. The numbers are worked out in Java
// from the list of incidents, because SLA status depends on the current time.
@Service
public class ReportService {

    private final IncidentRepository incidentRepository;
    private final TechnicianRepository technicianRepository;
    private final ITServiceRepository serviceRepository;
    private final AssignmentService assignmentService;

    public ReportService(IncidentRepository incidentRepository, TechnicianRepository technicianRepository,
                         ITServiceRepository serviceRepository, AssignmentService assignmentService) {
        this.incidentRepository = incidentRepository;
        this.technicianRepository = technicianRepository;
        this.serviceRepository = serviceRepository;
        this.assignmentService = assignmentService;
    }

    public List<Incident> allIncidents() {
        return incidentRepository.findAll();
    }

    // EnumMap: a Map whose keys are the values of one enum, kept in the enum's order.
    // We start every key at 0 so the dashboard also shows statuses with no incidents.
    public Map<IncidentStatus, Long> countByStatus(List<Incident> incidents) {
        Map<IncidentStatus, Long> counts = new EnumMap<>(IncidentStatus.class);
        for (IncidentStatus status : IncidentStatus.values()) {
            counts.put(status, 0L);
        }
        for (Incident incident : incidents) {
            counts.merge(incident.getStatus(), 1L, Long::sum); // add 1 to this status's count
        }
        return counts;
    }

    public Map<Priority, Long> countByPriority(List<Incident> incidents) {
        Map<Priority, Long> counts = new EnumMap<>(Priority.class);
        for (Priority priority : Priority.values()) {
            counts.put(priority, 0L);
        }
        for (Incident incident : incidents) {
            if (incident.getPriority() != null) {
                counts.merge(incident.getPriority(), 1L, Long::sum);
            }
        }
        return counts;
    }

    public Map<SlaStatus, Long> countBySlaStatus(List<Incident> incidents) {
        Map<SlaStatus, Long> counts = new EnumMap<>(SlaStatus.class);
        for (SlaStatus slaStatus : SlaStatus.values()) {
            counts.put(slaStatus, 0L);
        }
        for (Incident incident : incidents) {
            counts.merge(incident.getSlaStatus(), 1L, Long::sum);
        }
        return counts;
    }

    public long countOpen(List<Incident> incidents) {
        return incidents.stream().filter(i -> i.getStatus().isOpen()).count();
    }

    public long countUnassigned(List<Incident> incidents) {
        return incidents.stream().filter(i -> i.getStatus() == IncidentStatus.CLASSIFIED).count();
    }

    public List<TechnicianWorkload> technicianWorkloads(List<Incident> incidents) {
        List<TechnicianWorkload> rows = new ArrayList<>();
        for (Technician technician : technicianRepository.findAll(Sort.by("name"))) {
            long resolved = 0;
            long breached = 0;
            for (Incident incident : incidents) {
                Technician assigned = incident.getAssignedTechnician();
                if (assigned != null && assigned.getId().equals(technician.getId())) {
                    if (incident.getResolvedAt() != null) {
                        resolved++;
                    }
                    if (incident.getSlaStatus() == SlaStatus.BREACHED) {
                        breached++;
                    }
                }
            }
            rows.add(new TechnicianWorkload(technician, assignmentService.workloadOf(technician), resolved, breached));
        }
        return rows;
    }

    public List<ServiceReport> serviceReports(List<Incident> incidents) {
        List<ServiceReport> rows = new ArrayList<>();
        for (ITService service : serviceRepository.findAllByOrderByNameAsc()) {
            long total = 0;
            long open = 0;
            long breached = 0;
            for (Incident incident : incidents) {
                if (incident.getService().getId().equals(service.getId())) {
                    total++;
                    if (incident.getStatus().isOpen()) {
                        open++;
                    }
                    if (incident.getSlaStatus() == SlaStatus.BREACHED) {
                        breached++;
                    }
                }
            }
            rows.add(new ServiceReport(service, total, open, breached));
        }
        return rows;
    }
}
