package com.itsm.itsm.report;

// Everything the live dashboard shows. Spring turns this record into JSON
// automatically (one JSON field per record component), and the page's JavaScript reads it.
// It only holds plain numbers and text, never entities, so no passwords or other
// private fields can leak into the JSON.
public record DashboardStats(long total, long open, long unassigned, long breached,
                             ChartData byStatus, ChartData byPriority, ChartData bySla,
                             ChartData technicianWorkload, ChartData incidentsPerService,
                             String updatedAt) {
}
