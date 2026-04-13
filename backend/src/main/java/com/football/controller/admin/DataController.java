package com.football.controller.admin;

import com.football.common.Result;
import com.football.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/data")
public class DataController {

    @Autowired
    private DashboardService dashboardService;

    @GetMapping("/dashboard")
    public Result<Object> getDashboardData() {
        return Result.success(dashboardService.getDashboardData());
    }

    @GetMapping("/export")
    public Result<Object> exportReport() {
        // 实现导出报表逻辑
        return Result.success(null);
    }
}