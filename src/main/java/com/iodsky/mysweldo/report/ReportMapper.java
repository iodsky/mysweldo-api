package com.iodsky.mysweldo.report;

import org.springframework.stereotype.Component;

@Component
public class ReportMapper {

    public ReportDto toDto(Report report) {
        if (report == null) return null;

        return ReportDto.builder()
                .id(report.getId())
                .type(report.getType())
                .format(report.getFormat())
                .fileName(report.getFileName())
                .periodStart(report.getPeriodStart())
                .periodEnd(report.getPeriodEnd())
                .payrollRunId(report.getPayrollRun() == null ? null : report.getPayrollRun().getId())
                .createdAt(report.getCreatedAt())
                .build();
    }

}