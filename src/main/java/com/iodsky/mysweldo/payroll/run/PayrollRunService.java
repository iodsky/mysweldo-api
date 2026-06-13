package com.iodsky.mysweldo.payroll.run;


import com.iodsky.mysweldo.benefit.Benefit;
import com.iodsky.mysweldo.benefit.BenefitService;
import com.iodsky.mysweldo.common.DateRange;
import com.iodsky.mysweldo.deduction.Deduction;
import com.iodsky.mysweldo.deduction.DeductionService;
import com.iodsky.mysweldo.employee.EmployeeService;
import com.iodsky.mysweldo.attendance.AttendanceService;
import com.iodsky.mysweldo.payroll.core.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Stream;


@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PayrollRunService {

    private final PayrollRunRepository repository;
    private final PayrollRunMapper mapper;
    private final EmployeeService employeeService;
    private final AttendanceService attendanceService;
    private final PayrollCalculator calculator;
    private final PayrollItemRepository payrollItemRepository;
    private final PayrollBuilder payrollBuilder;
    private final PayrollItemMapper payrollItemMapper;
    private final DeductionService deductionService;
    private final BenefitService benefitService;

    public PayrollRunDto createPayrollRun(PayrollRunRequest request) {
        PayrollPeriod period;
        try {
            period = PayrollPeriod.of(
                    request.getPeriodStartDate(),
                    request.getPeriodEndDate(),
                    request.getPayrollFrequency()
            );
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        PayrollRun payrollRun = PayrollRun.builder()
                .period(period)
                .type(request.getType())
                .status(PayrollRunStatus.DRAFT)
                .notes(request.getNotes())
                .build();

        repository.save(payrollRun);
        return mapper.toDto(payrollRun);
    }

    public GeneratePayrollResponse generatePayroll(UUID id, GeneratePayrollRequest request) {
        PayrollRun run = findPayrollRun(id);

        if (!run.getStatus().equals(PayrollRunStatus.DRAFT)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payroll run " + id + " already processed");
        }

        List<Long> employeeIds = request.getEmployeeIds();
        if (employeeIds == null || employeeIds.isEmpty()) {
            employeeIds = employeeService.getAllActiveEmployeeIds();
        }
 
        if (employeeIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No active employees found");
        }

        PayrollConfiguration configuration = calculator.loadConfiguration(run.getPeriod().getEndDate());

        List<PayrollItem> payrollItems = new ArrayList<>();
        List<Long> skippedIds = new ArrayList<>();
        for (Long employeeId: employeeIds) {
            if (payrollItemRepository.existsByPayrollRun_IdAndEmployee_Id(run.getId(), employeeId)) {
                log.warn("Payroll exists for employee: {} run: {}", employeeId, run.getId());
                skippedIds.add(employeeId);
                continue;
            }

            if (!attendanceService.hasAttendance(employeeId, run.getPeriod().getStartDate(), run.getPeriod().getEndDate())) {
                log.warn("No attendance records for employee: {} in period: {} - {}", employeeId, run.getPeriod().getStartDate(), run.getPeriod().getEndDate());
                skippedIds.add(employeeId);
                continue;
            }

            PayrollItem payrollItem = payrollBuilder.buildPayroll(employeeId, run, configuration);

            payrollItems.add(payrollItem);
        }

        payrollItemRepository.saveAll(payrollItems);

        computeRunTotals(run);

        repository.save(run);

        return GeneratePayrollResponse.builder()
                .payrollRun(mapper.toDto(run))
                .skippedEmployeeIds(skippedIds)
                .build();
    }

    public Page<PayrollRunDto> getAllPayrollRuns(LocalDate periodStartDate, LocalDate periodEndDate, PayrollRunType type, PayrollRunStatus status, Integer pageNo, Integer limit) {
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        Pageable pageable = PageRequest.of(pageNo, limit, sort);

        Page<PayrollRun> result;

        if (periodStartDate != null && periodEndDate != null) {
            DateRange range = new DateRange(periodStartDate, periodEndDate);
            result = repository.getAllByPeriod_StartDateGreaterThanEqualAndPeriod_EndDateLessThanEqual(range.startDate(), range.endDate(), pageable);
        } else if (type != null && status != null) {
            result = repository.getAllByTypeAndStatus(type, status, pageable);
        } else if (type != null) {
            result = repository.getAllByType(type, pageable);
        } else if (status != null) {
            result = repository.getAllByStatus(status, pageable);
        } else {
            result = repository.findAll(pageable);
        }

        return result.map(mapper::toDto);
    }

    public PayrollRunDto getPayrollRunById(UUID id) {
        return mapper.toDto(findPayrollRun(id));
    }

    public Page<PayrollItemDto> getPayrollItems(UUID id, Integer pageNo, Integer limit) {
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        Pageable pageable = PageRequest.of(pageNo, limit, sort);

        Page<PayrollItem> items = payrollItemRepository.findAllByPayrollRun_Id(id, pageable);

        return items.map(payrollItemMapper::toDto);
    }

    public PayrollItemDto getPayrollItem(UUID id, UUID itemId) {
        PayrollItem item = findPayrollItem(id, itemId);

        return payrollItemMapper.toDto(item);
    }

    public PayrollItemDto updatePayrollDeductions(UUID id, UUID itemId, UpdatePayrollDeductionRequest request) {
        PayrollRun run = findPayrollRun(id);

        if (!run.getStatus().equals(PayrollRunStatus.DRAFT)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payroll run " + id + " has been already " + run.getStatus());
        }

        PayrollItem item = findPayrollItem(id, itemId);

        for (LineItemEntry entry : request.getDeductions()) {
            //      - Look up an existing PayrollDeduction in item.getDeductions() where deduction.getCode() == entry.getCode()
            Optional<PayrollDeduction> existing = item.getDeductions().stream()
                    .filter(d -> d.getDeduction().getCode().equals(entry.getCode()))
                    .findFirst();
            //      - If found  → update its amount (override)
            if (existing.isPresent()) {
                existing.get().setAmount(entry.getAmount());
            } else {
                Deduction deduction = deductionService.getDeductionByCode(entry.getCode());

                PayrollDeduction newDeduction = PayrollDeduction.builder()
                        .deduction(deduction)
                        .amount(entry.getAmount())
                        .payrollItem(item)
                        .build();

                item.getDeductions().add(newDeduction);
            }
        }

        item.setTotalDeductions(item.getDeductions().stream()
                .map(PayrollDeduction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        item.setNetPay(item.getGrossPay()
                .add(item.getTotalBenefits())
                .subtract(item.getTotalDeductions()));

        PayrollItem updated = payrollItemRepository.save(item);
        return payrollItemMapper.toDto(updated);
    }

    public PayrollItemDto updatePayrollBenefits(UUID id, UUID itemId, UpdatePayrollBenefitRequest request) {
        PayrollRun run = findPayrollRun(id);

        if (!run.getStatus().equals(PayrollRunStatus.DRAFT)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payroll run " + id + " has been already " + run.getStatus());
        }

        PayrollItem item = findPayrollItem(id, itemId);

        for (LineItemEntry entry : request.getBenefits()) {
            Optional<PayrollBenefit> existing = item.getBenefits().stream()
                    .filter(b -> b.getBenefit().getCode().equals(entry.getCode()))
                    .findFirst();

            if (existing.isPresent()) {
                existing.get().setAmount(entry.getAmount());
            } else {
                Benefit benefit = benefitService.getBenefitByCode(entry.getCode());

                PayrollBenefit newBenefit = PayrollBenefit.builder()
                        .benefit(benefit)
                        .amount(entry.getAmount())
                        .payrollItem(item)
                        .build();

                item.getBenefits().add(newBenefit);
            }
        }

        item.setTotalBenefits(item.getBenefits().stream()
                .map(PayrollBenefit::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        item.setNetPay(item.getGrossPay()
                .add(item.getTotalBenefits())
                .subtract(item.getTotalDeductions()));

        PayrollItem updated = payrollItemRepository.save(item);
        return payrollItemMapper.toDto(updated);
    }

    public PayrollRunDto updatePayrollRunStatus(UUID id, PayrollRunStatus status) {
        PayrollRun run = findPayrollRun(id);

        if (run.getStatus().equals(PayrollRunStatus.PROCESSED)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payroll run " + id + " has already been processed");
        }

        boolean validTransition = switch (run.getStatus()) {
            case DRAFT -> status == PayrollRunStatus.APPROVED;
            case APPROVED -> status == PayrollRunStatus.PROCESSED;
            default -> false;
        };

        if (!validTransition) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid status transition from " + run.getStatus() + " to " + status);
        }

        run.setStatus(status);
        repository.save(run);
        return mapper.toDto(run);
    }

    public void deletePayrollItem(UUID id, UUID itemId) {
        PayrollRun run = findPayrollRun(id);

        if (!run.getStatus().equals(PayrollRunStatus.DRAFT)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Payroll run is not in DRAFT status"
            );
        }

        PayrollItem item = findPayrollItem(id, itemId);

        item.setDeletedAt(Instant.now());
        payrollItemRepository.save(item);

        computeRunTotals(run);
        repository.save(run);
    }

    private PayrollItem findPayrollItem(UUID id, UUID itemId) {
        return payrollItemRepository.findByPayrollRun_IdAndId(id, itemId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Payroll item " + itemId + " not found"));
    }

    private PayrollRun findPayrollRun(UUID id) {
        return  repository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Payroll run " + id + " not found"));
    }

    private void computeRunTotals(PayrollRun run) {
        List<PayrollItem> allItems = payrollItemRepository.findAllByPayrollRun_Id(run.getId());
        run.setTotalGrossPay(allItems.stream().map(PayrollItem::getGrossPay).reduce(BigDecimal.ZERO, BigDecimal::add));
        run.setTotalBenefits(allItems.stream().map(PayrollItem::getTotalBenefits).reduce(BigDecimal.ZERO, BigDecimal::add));
        run.setTotalDeductions(allItems.stream().map(PayrollItem::getTotalDeductions).reduce(BigDecimal.ZERO, BigDecimal::add));
        run.setTotalNetPay(allItems.stream().map(PayrollItem::getNetPay).reduce(BigDecimal.ZERO, BigDecimal::add));

        run.setTotalEmployerCost(allItems.stream()
                .flatMap(item -> item.getEmployerContributions() == null
                        ? Stream.empty()
                        : item.getEmployerContributions().stream())
                .map(EmployerContribution::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

}
