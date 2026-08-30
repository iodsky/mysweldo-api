package com.iodsky.mysweldo.payroll.calc;

import com.iodsky.mysweldo.payroll.item.PayrollItemRepository;
import com.iodsky.mysweldo.payroll.run.PayrollFrequency;
import com.iodsky.mysweldo.payroll.run.PayrollPeriod;
import com.iodsky.mysweldo.payroll.run.PayrollRun;
import com.iodsky.mysweldo.payroll.run.PayrollRunStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatutorySchedulePolicyTest {

    @InjectMocks
    private StatutorySchedulePolicy policy;

    @Mock
    private PayrollItemRepository payrollItemRepository;

    private static final List<PayrollRunStatus> FINALIZED =
            List.of(PayrollRunStatus.APPROVED, PayrollRunStatus.PROCESSED);

    private PayrollRun runWithFrequency(PayrollFrequency frequency) {
        LocalDate start;
        LocalDate end;
        switch (frequency) {
            case SEMI_MONTHLY -> { start = LocalDate.of(2025, 6, 1); end = LocalDate.of(2025, 6, 15); }
            case MONTHLY -> { start = LocalDate.of(2025, 6, 1); end = LocalDate.of(2025, 6, 30); }
            case WEEKLY -> { start = LocalDate.of(2025, 6, 2); end = LocalDate.of(2025, 6, 8); }
            default -> { start = LocalDate.of(2025, 6, 2); end = LocalDate.of(2025, 6, 15); }
        }
        return PayrollRun.builder()
                .period(PayrollPeriod.of(start, end, frequency))
                .build();
    }

    @Nested
    class AlwaysCollect {

        @Test
        void semiMonthlyRunAlwaysReturnsTrue() {
            boolean result = policy.shouldCollectStatutory(1L, runWithFrequency(PayrollFrequency.SEMI_MONTHLY));

            assertThat(result).isTrue();
            verify(payrollItemRepository, never()).existsByEmployeeInMonthWithStatus(anyLong(), any(), any(), any());
        }

        @Test
        void monthlyRunAlwaysReturnsTrue() {
            boolean result = policy.shouldCollectStatutory(1L, runWithFrequency(PayrollFrequency.MONTHLY));

            assertThat(result).isTrue();
            verify(payrollItemRepository, never()).existsByEmployeeInMonthWithStatus(anyLong(), any(), any(), any());
        }
    }

    @Nested
    class WeeklyFrequency {

        private final PayrollRun weeklyRun = runWithFrequency(PayrollFrequency.WEEKLY);
        private final LocalDate monthStart = LocalDate.of(2025, 6, 1);
        private final LocalDate monthEnd = LocalDate.of(2025, 6, 30);

        @Test
        void noPriorFinalizedRunInMonth_returnsTrue() {
            when(payrollItemRepository.existsByEmployeeInMonthWithStatus(1L, monthStart, monthEnd, FINALIZED))
                    .thenReturn(false);

            assertThat(policy.shouldCollectStatutory(1L, weeklyRun)).isTrue();
        }

        @Test
        void priorApprovedRunInMonth_returnsFalse() {
            when(payrollItemRepository.existsByEmployeeInMonthWithStatus(1L, monthStart, monthEnd, FINALIZED))
                    .thenReturn(true);

            assertThat(policy.shouldCollectStatutory(1L, weeklyRun)).isFalse();
        }

        @Test
        void priorDraftRunDoesNotBlock_returnsTrue() {
            // Repository only checks APPROVED/PROCESSED — DRAFT runs are excluded by the query
            when(payrollItemRepository.existsByEmployeeInMonthWithStatus(
                    eq(1L), eq(monthStart), eq(monthEnd), eq(FINALIZED)))
                    .thenReturn(false);

            assertThat(policy.shouldCollectStatutory(1L, weeklyRun)).isTrue();
        }
    }

    @Nested
    class BiWeeklyFrequency {

        private final PayrollRun biWeeklyRun = runWithFrequency(PayrollFrequency.BI_WEEKLY);
        private final LocalDate monthStart = LocalDate.of(2025, 6, 1);
        private final LocalDate monthEnd = LocalDate.of(2025, 6, 30);

        @Test
        void noPriorFinalizedRunInMonth_returnsTrue() {
            when(payrollItemRepository.existsByEmployeeInMonthWithStatus(1L, monthStart, monthEnd, FINALIZED))
                    .thenReturn(false);

            assertThat(policy.shouldCollectStatutory(1L, biWeeklyRun)).isTrue();
        }

        @Test
        void priorApprovedRunInMonth_returnsFalse() {
            when(payrollItemRepository.existsByEmployeeInMonthWithStatus(1L, monthStart, monthEnd, FINALIZED))
                    .thenReturn(true);

            assertThat(policy.shouldCollectStatutory(1L, biWeeklyRun)).isFalse();
        }
    }
}
