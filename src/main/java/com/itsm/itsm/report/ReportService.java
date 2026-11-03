package com.itsm.itsm.report;

import com.itsm.itsm.assignment.AssignmentService;
import com.itsm.itsm.model.*;
import com.itsm.itsm.repository.ITServiceRepository;
import com.itsm.itsm.repository.IncidentRepository;
import com.itsm.itsm.repository.TechnicianRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
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

    // All the numbers for the live charts page, worked out from one list of incidents.
    // It reuses the methods above, so the charts always agree with the dashboard tables.
    public DashboardStats dashboardStats() {
        List<Incident> incidents = allIncidents();

        List<String> statusLabels = new ArrayList<>();
        List<Long> statusValues = new ArrayList<>();
        for (Map.Entry<IncidentStatus, Long> entry : countByStatus(incidents).entrySet()) {
            statusLabels.add(entry.getKey().name());
            statusValues.add(entry.getValue());
        }

        List<String> priorityLabels = new ArrayList<>();
        List<Long> priorityValues = new ArrayList<>();
        for (Map.Entry<Priority, Long> entry : countByPriority(incidents).entrySet()) {
            priorityLabels.add(entry.getKey().name() + " " + entry.getKey().getLabel());
            priorityValues.add(entry.getValue());
        }

        Map<SlaStatus, Long> bySla = countBySlaStatus(incidents);
        List<String> slaLabels = new ArrayList<>();
        List<Long> slaValues = new ArrayList<>();
        for (Map.Entry<SlaStatus, Long> entry : bySla.entrySet()) {
            slaLabels.add(entry.getKey().getLabel());
            slaValues.add(entry.getValue());
        }

        List<String> technicianNames = new ArrayList<>();
        List<Long> activeCounts = new ArrayList<>();
        for (TechnicianWorkload row : technicianWorkloads(incidents)) {
            technicianNames.add(row.technician().getName());
            activeCounts.add(row.active());
        }

        List<String> serviceNames = new ArrayList<>();
        List<Long> serviceTotals = new ArrayList<>();
        for (ServiceReport row : serviceReports(incidents)) {
            serviceNames.add(row.service().getName());
            serviceTotals.add(row.total());
        }

        return new DashboardStats(incidents.size(), countOpen(incidents), countUnassigned(incidents),
                bySla.get(SlaStatus.BREACHED),
                new ChartData(statusLabels, statusValues),
                new ChartData(priorityLabels, priorityValues),
                new ChartData(slaLabels, slaValues),
                new ChartData(technicianNames, activeCounts),
                new ChartData(serviceNames, serviceTotals),
                LocalTime.now().withNano(0).toString());
    }
}
