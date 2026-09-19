package com.iodsky.mysweldo.report;

import com.iodsky.mysweldo.employee.Employee;
import com.iodsky.mysweldo.payroll.item.PayrollItem;
import com.iodsky.mysweldo.payroll.item.PayrollItemRepository;
import com.iodsky.mysweldo.payroll.run.PayrollRun;
import com.iodsky.mysweldo.payroll.run.PayrollRunRepository;
import com.iodsky.mysweldo.payroll.run.PayrollRunStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PayrollReportService {

    private static final List<String> HEADERS = List.of(
            "Employee ID", "Employee Name", "Bank Name", "Account Number", "Net Pay");

    private final PayrollRunRepository payrollRunRepository;
    private final PayrollItemRepository payrollItemRepository;

    public PayrollReport build(UUID runId) {
        PayrollRun run = payrollRunRepository.findById(runId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payroll run " + runId + " not found"));

        if (run.getStatus() == PayrollRunStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Payroll run " + runId + " is still in DRAFT; export requires APPROVED or PROCESSED");
        }

        List<PayrollItem> items = payrollItemRepository.findAllByPayrollRun_Id(runId);
        if (items.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No payroll items to export");
        }

        List<List<String>> rows = items.stream()
                .map(this::toRow)
                .toList();

        return new PayrollReport(run, HEADERS, rows);
    }

    private List<String> toRow(PayrollItem item) {
        Employee employee = item.getEmployee();
        return List.of(
                String.valueOf(employee.getId()),
                employee.getFirstName() + " " + employee.getLastName(),
                emptyIfNull(employee.getBankName()),
                emptyIfNull(employee.getAccountNumber()),
                item.getNetPay() == null ? "" : item.getNetPay().toPlainString()
        );
    }

    private String emptyIfNull(String value) {
        return value == null ? "" : value;
    }

    public record PayrollReport(PayrollRun run, List<String> headers, List<List<String>> rows) {
    }

}