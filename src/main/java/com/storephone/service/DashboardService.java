package com.storephone.service;

import com.storephone.dao.DashboardDAO;
import com.storephone.dto.DashboardMetrics;
import java.util.Objects;

public class DashboardService {
    private final DashboardDAO dashboardDAO;
    private final AuthorizationService authorizationService;

    public DashboardService(DashboardDAO dashboardDAO, AuthorizationService authorizationService) {
        this.dashboardDAO = Objects.requireNonNull(dashboardDAO);
        this.authorizationService = Objects.requireNonNull(authorizationService);
    }

    public DashboardMetrics getDashboardMetrics() {
        authorizationService.requireAdmin();
        return dashboardDAO.fetchDashboardMetrics();
    }
}
