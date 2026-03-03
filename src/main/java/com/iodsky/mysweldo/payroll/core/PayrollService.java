package com.iodsky.mysweldo.payroll.core;

import com.iodsky.mysweldo.security.user.User;
import com.iodsky.mysweldo.security.user.UserService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PayrollService {

    private static final Logger log = LoggerFactory.getLogger(PayrollService.class);

    private final PayrollRepository payrollRepository;
    private final PayrollBuilder payrollBuilder;
    private final UserService userService;

    public Payroll createPayroll(Long employeeId, LocalDate periodStartDate, LocalDate periodEndDate, LocalDate payDate) {

        if (payrollExistsForEmployeeAndPeriod(employeeId, periodStartDate, periodEndDate)) {
            log.warn(
                    "Payroll already exists for employee {} for period {} to {}. Skipping...",
                    employeeId, periodStartDate, periodEndDate
            );
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    String.format(
                        "Payroll already exists for employee %s for period %s to %s.",
                        employeeId, periodStartDate, periodEndDate
                    )
            );
        }

        Payroll payroll = payrollBuilder.buildPayroll(employeeId, periodStartDate, periodEndDate, payDate);

        return payrollRepository.save(payroll);
    }

    private Boolean payrollExistsForEmployeeAndPeriod(Long employeeId, LocalDate startDate, LocalDate endDate) {
        return payrollRepository.existsByEmployee_IdAndPeriodStartDateAndPeriodEndDate(employeeId, startDate, endDate);
    }

    public Payroll getPayrollById(UUID payrollId) {
        User user = userService.getAuthenticatedUser();

        Payroll payroll = payrollRepository.findById(payrollId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payroll " + payrollId + " not found"));

        if (!user.getRole().getName().equals("PAYROLL") ||
                !payroll.getEmployee().getId().equals(user.getEmployee().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You don't have permission to access this resource");
        }

        return payroll;
    }

    public Page<Payroll> getAllPayroll(int page, int limit, YearMonth period) {
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        Pageable pageable = PageRequest.of(page, limit, sort);

        if (period != null) {
            LocalDate startDate = period.atDay(1);
            LocalDate endDate = period.atEndOfMonth();

            return payrollRepository
                    .findAllByPeriodStartDateLessThanEqualAndPeriodEndDateGreaterThanEqual(
                            endDate,
                            startDate,
                            pageable
                    );
        }

        return payrollRepository.findAll(pageable);
    }

    public Page<Payroll> getAllEmployeePayroll(int page, int limit, YearMonth period) {
        User user = userService.getAuthenticatedUser();
        Pageable pageable = PageRequest.of(page, limit);

        Long id = user.getEmployee().getId();

        if (period != null) {
            LocalDate startDate = period.atDay(1);
            LocalDate endDate = period.atEndOfMonth();

            return payrollRepository.findAllByEmployee_IdAndPeriodStartDateLessThanEqualAndPeriodEndDateGreaterThanEqual(
                    id,
                    endDate,
                    startDate,
                    pageable
            );
        }

        return payrollRepository.findAllByEmployee_Id(id, pageable);
    }

}
