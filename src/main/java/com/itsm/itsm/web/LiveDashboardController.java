package com.itsm.itsm.web;

import com.itsm.itsm.report.DashboardStats;
import com.itsm.itsm.report.ReportService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

// Innovation feature: the live charts dashboard. It has two parts:
//   /admin/live        sends the HTML page with empty charts (once)
//   /admin/api/stats   sends the numbers as JSON; the page asks for them every 10 seconds
// Both start with /admin, so SecurityConfig already lets only admins in.
@Controller
@RequestMapping("/admin")
public class LiveDashboardController {

    private final ReportService reportService;

    public LiveDashboardController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/live")
    public String livePage() {
        return "admin/live";
    }

    // @ResponseBody: send the returned object itself (as JSON), not a template name
    @GetMapping("/api/stats")
    @ResponseBody
    public DashboardStats stats() {
        return reportService.dashboardStats();
    }
}
