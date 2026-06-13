package com.iodsky.mysweldo.payroll.strategy;

import com.iodsky.mysweldo.attendance.AttendancePayrollSummary;
import com.iodsky.mysweldo.attendance.AttendanceService;
import com.iodsky.mysweldo.employee.Employee;
import com.iodsky.mysweldo.employee.PayType;
import com.iodsky.mysweldo.employee.Salary;
import com.iodsky.mysweldo.overtime.OvertimeRequestService;
import com.iodsky.mysweldo.pagIbig.PagibigRate;
import com.iodsky.mysweldo.payroll.core.PayrollCalculator;
import com.iodsky.mysweldo.payroll.core.PayrollConfiguration;
import com.iodsky.mysweldo.payroll.core.PayrollContext;
import com.iodsky.mysweldo.payroll.core.StatutorySchedulePolicy;
import com.iodsky.mysweldo.payroll.run.PayrollFrequency;
import com.iodsky.mysweldo.payroll.run.PayrollPeriod;
import com.iodsky.mysweldo.payroll.run.PayrollRun;
import com.iodsky.mysweldo.payroll.run.PayrollRunException;
import com.iodsky.mysweldo.philhealth.PhilhealthRate;
import com.iodsky.mysweldo.sss.SssRate;
import com.iodsky.mysweldo.tax.TaxBracket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SemiMonthlyPayrollStrategyTest {

    @Mock
    private AttendanceService attendanceService;

    @Mock
    private OvertimeRequestService overtimeRequestService;

    @Mock
    private StatutorySchedulePolicy statutorySchedulePolicy;

    private SemiMonthlyPayrollStrategy strategy;
    private PayrollRun payrollRun;
    private PayrollConfiguration configuration;

    @BeforeEach
    void setUp() {
        PayrollCalculator calculator = new PayrollCalculator(null, null, null, null);
        PayBasisStrategyFactory basisFactory = new PayBasisStrategyFactory(
                new MonthlyPayBasisStrategy(calculator),
                new DailyPayBasisStrategy(calculator),
                new HourlyPayBasisStrategy(calculator)
        );
        strategy = new SemiMonthlyPayrollStrategy(
                attendanceService, overtimeRequestService, calculator, basisFactory, statutorySchedulePolicy);

        lenient().when(statutorySchedulePolicy.shouldCollectStatutory(anyLong(), any())).thenReturn(true);

        payrollRun = PayrollRun.builder()
                .period(PayrollPeriod.of(
                        LocalDate.of(2026, 6, 1),
                        LocalDate.of(2026, 6, 15),
                        PayrollFrequency.SEMI_MONTHLY))
                .build();

        configuration = PayrollConfiguration.builder()
                .sssRateTable(SssRate.builder()
                        .employeeRate(new BigDecimal("0.045"))
                        .employerRate(new BigDecimal("0.095"))
                        .salaryBrackets(List.of(
                                new SssRate.SalaryBracket(BigDecimal.ZERO, BigDecimal.valueOf(20000), BigDecimal.valueOf(10000)),
                                new SssRate.SalaryBracket(new BigDecimal("20000.01"), null, BigDecimal.valueOf(20000))
                        ))
                        .build())
                .philhealthRateTable(PhilhealthRate.builder()
                        .premiumRate(new BigDecimal("0.05"))
                        .minSalaryFloor(BigDecimal.valueOf(10000))
                        .maxSalaryCap(BigDecimal.valueOf(100000))
                        .fixedContribution(BigDecimal.valueOf(500))
                        .build())
                .pagibigRateTable(PagibigRate.builder()
                        .employeeRate(new BigDecimal("0.02"))
                        .employerRate(new BigDecimal("0.02"))
                        .lowIncomeThreshold(BigDecimal.valueOf(1500))
                        .lowIncomeEmployeeRate(new BigDecimal("0.01"))
                        .maxSalaryCap(BigDecimal.valueOf(10000))
                        .build())
                .incomeTaxBrackets(List.of(TaxBracket.builder()
                        .minIncome(BigDecimal.ZERO)
                        .maxIncome(null)
                        .baseTax(BigDecimal.ZERO)
                        .marginalRate(new BigDecimal("0.10"))
                        .threshold(BigDecimal.ZERO)
                        .build()))
                .build();
    }

    private Employee employee(PayType payType, double rate) {
        return Employee.builder()
                .id(1L)
                .salary(Salary.builder()
                        .rate(BigDecimal.valueOf(rate))
                        .payType(payType)
                        .build())
                .benefits(List.of())
                .build();
    }

    private void stubAttendance(double daysWorked, double absenceDays, int tardy, int undertime,
                                double totalHours, double overtimeHours) {
        when(attendanceService.getAttendanceSummary(anyLong(), any(), any()))
                .thenReturn(AttendancePayrollSummary.builder()
                        .daysWorked(BigDecimal.valueOf(daysWorked))
                        .absenceDays(BigDecimal.valueOf(absenceDays))
                        .tardinessMinutes(tardy)
                        .undertimeMinutes(undertime)
                        .build());
        when(attendanceService.calculateTotalHoursByEmployeeId(anyLong(), any(), any()))
                .thenReturn(BigDecimal.valueOf(totalHours));
        when(overtimeRequestService.calculateApprovedOvertimeHours(anyLong(), any(), any()))
                .thenReturn(BigDecimal.valueOf(overtimeHours));
    }

    @Test
    void compute_monthlyGoldenPath_matchesPreBasisRefactorNumbers() {
        stubAttendance(10, 1, 30, 15, 80, 2);

        PayrollContext context = strategy.compute(employee(PayType.MONTHLY, 20000), payrollRun, configuration);

        assertThat(context.getPayType()).isEqualTo(PayType.MONTHLY);
        assertThat(context.getMonthlyRate()).isEqualByComparingTo("20000");
        assertThat(context.getSemiMonthlyRate()).isEqualByComparingTo("10000.00");
        assertThat(context.getDailyRate()).isEqualByComparingTo("919.54");
        assertThat(context.getHourlyRate()).isEqualByComparingTo("114.94");
        assertThat(context.getAbsenceDeduction()).isEqualByComparingTo("919.54");
        assertThat(context.getTardinessDeduction()).isEqualByComparingTo("57.47");
        assertThat(context.getUndertimeDeduction()).isEqualByComparingTo("28.74");
        assertThat(context.getRegularPay()).isEqualByComparingTo("8994.25");
        // 114.94 * 2 * 1.25
        assertThat(context.getOvertimePay()).isEqualByComparingTo("287.35");
        assertThat(context.getGrossPay()).isEqualByComparingTo("9281.60");
        // SSS: MSC 10000 * 0.045 / 2; PhilHealth: 20000 * 0.05 / 2 / 2; Pag-IBIG: 10000 * 0.02 / 2
        assertThat(context.getSss()).isEqualByComparingTo("225.00");
        assertThat(context.getPhilhealth()).isEqualByComparingTo("250.00");
        assertThat(context.getPagibig()).isEqualByComparingTo("100.00");
        assertThat(context.getTaxableIncome()).isEqualByComparingTo("8706.60");
        // (8706.60 * 2) * 0.10 / 2
        assertThat(context.getWithholdingTax()).isEqualByComparingTo("870.66");
        assertThat(context.getNetPay()).isEqualByComparingTo("7835.94");
    }

    @Test
    void compute_dailyEmployee_paysDaysWorkedAndUsesMonthlyEquivalentForStatutory() {
        stubAttendance(10, 3, 30, 0, 80, 2);

        PayrollContext context = strategy.compute(employee(PayType.DAILY, 800), payrollRun, configuration);

        assertThat(context.getPayType()).isEqualTo(PayType.DAILY);
        // 800 * 21.75, not 800 treated as a monthly rate
        assertThat(context.getMonthlyRate()).isEqualByComparingTo("17400.00");
        assertThat(context.getDailyRate()).isEqualByComparingTo("800");
        assertThat(context.getHourlyRate()).isEqualByComparingTo("100.00");
        // 800 * 10 - 50.00 tardiness; absences not deducted
        assertThat(context.getAbsenceDeduction()).isEqualByComparingTo("0");
        assertThat(context.getTardinessDeduction()).isEqualByComparingTo("50.00");
        assertThat(context.getRegularPay()).isEqualByComparingTo("7950.00");
        // 100 * 2 * 1.25
        assertThat(context.getOvertimePay()).isEqualByComparingTo("250.00");
        // statutory computed from 17400: SSS MSC 10000, PhilHealth 17400 * 0.05 / 4
        assertThat(context.getSss()).isEqualByComparingTo("225.00");
        assertThat(context.getPhilhealth()).isEqualByComparingTo("217.50");
        assertThat(context.getPagibig()).isEqualByComparingTo("100.00");
    }

    @Test
    void compute_hourlyEmployee_paysRegularHoursAndUsesMonthlyEquivalentForStatutory() {
        stubAttendance(10, 2, 45, 10, 88, 8);

        PayrollContext context = strategy.compute(employee(PayType.HOURLY, 150), payrollRun, configuration);

        assertThat(context.getPayType()).isEqualTo(PayType.HOURLY);
        // 150 * 8 * 21.75
        assertThat(context.getMonthlyRate()).isEqualByComparingTo("26100.00");
        assertThat(context.getDailyRate()).isEqualByComparingTo("1200.00");
        assertThat(context.getHourlyRate()).isEqualByComparingTo("150");
        // regularHours = min(88 - 8, 80) = 80
        assertThat(context.getRegularHours()).isEqualByComparingTo("80");
        assertThat(context.getRegularPay()).isEqualByComparingTo("12000.00");
        // 150 * 8 * 1.25
        assertThat(context.getOvertimePay()).isEqualByComparingTo("1500.00");
        assertThat(context.getAbsenceDeduction()).isEqualByComparingTo("0");
        assertThat(context.getTardinessDeduction()).isEqualByComparingTo("0");
        assertThat(context.getUndertimeDeduction()).isEqualByComparingTo("0");
        // statutory computed from 26100: second SSS bracket MSC 20000
        assertThat(context.getSss()).isEqualByComparingTo("450.00");
        assertThat(context.getPhilhealth()).isEqualByComparingTo("326.25");
    }

    @Test
    void compute_capsUnapprovedHoursAtStandardHours() {
        stubAttendance(10, 0, 0, 0, 90, 0);

        PayrollContext context = strategy.compute(employee(PayType.HOURLY, 150), payrollRun, configuration);

        // 10 hours beyond daysWorked * 8 without approved OT are unpaid
        assertThat(context.getRegularHours()).isEqualByComparingTo("80");
        assertThat(context.getRegularPay()).isEqualByComparingTo("12000.00");
        assertThat(context.getOvertimePay()).isEqualByComparingTo("0.00");
    }

    @Test
    void compute_clampsRegularHoursAtZeroWhenOvertimeExceedsTotalHours() {
        stubAttendance(0, 0, 0, 0, 0, 8);

        PayrollContext context = strategy.compute(employee(PayType.HOURLY, 150), payrollRun, configuration);

        assertThat(context.getRegularHours()).isEqualByComparingTo("0");
        assertThat(context.getRegularPay()).isEqualByComparingTo("0.00");
    }

    @Test
    void compute_nullPayTypeFallsBackToMonthlyBasis() {
        stubAttendance(10, 0, 0, 0, 80, 0);

        PayrollContext context = strategy.compute(employee(null, 20000), payrollRun, configuration);

        assertThat(context.getMonthlyRate()).isEqualByComparingTo("20000");
        assertThat(context.getSemiMonthlyRate()).isEqualByComparingTo("10000.00");
        assertThat(context.getRegularPay()).isEqualByComparingTo("10000.00");
    }

    @Test
    void compute_missingSalaryThrows() {
        Employee employee = Employee.builder().id(1L).benefits(List.of()).build();
        when(attendanceService.getAttendanceSummary(anyLong(), any(), any()))
                .thenReturn(AttendancePayrollSummary.builder()
                        .daysWorked(BigDecimal.TEN)
                        .absenceDays(BigDecimal.ZERO)
                        .tardinessMinutes(0)
                        .undertimeMinutes(0)
                        .build());

        assertThatThrownBy(() -> strategy.compute(employee, payrollRun, configuration))
                .isInstanceOf(PayrollRunException.class)
                .hasMessageContaining("No salary record found");
    }

    @Test
    void compute_policyReturnsFalse_zerosAllStatutoryFields() {
        when(statutorySchedulePolicy.shouldCollectStatutory(anyLong(), any())).thenReturn(false);
        stubAttendance(10, 0, 0, 0, 80, 0);

        PayrollContext context = strategy.compute(employee(PayType.MONTHLY, 20000), payrollRun, configuration);

        assertThat(context.getSss()).isEqualByComparingTo("0");
        assertThat(context.getPhilhealth()).isEqualByComparingTo("0");
        assertThat(context.getPagibig()).isEqualByComparingTo("0");
        assertThat(context.getSssEr()).isEqualByComparingTo("0");
        assertThat(context.getPhilhealthEr()).isEqualByComparingTo("0");
        assertThat(context.getPagibigEr()).isEqualByComparingTo("0");
        // withholding tax still applies — netPay = grossPay - withholdingTax (no statutory deductions)
        assertThat(context.getNetPay()).isEqualByComparingTo(
                context.getGrossPay().subtract(context.getWithholdingTax()));
    }
}
