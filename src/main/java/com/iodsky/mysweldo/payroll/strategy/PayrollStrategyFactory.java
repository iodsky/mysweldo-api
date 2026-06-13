package com.iodsky.mysweldo.payroll.strategy;

import com.iodsky.mysweldo.payroll.run.PayrollFrequency;
import com.iodsky.mysweldo.payroll.run.PayrollRunException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Factory for resolving the appropriate PayrollComputationStrategy based on payroll frequency.
 *
 * This component enables extensibility by allowing new payroll types to be added
 * by simply registering new strategy implementations.
 */
@Component
@RequiredArgsConstructor
public class PayrollStrategyFactory {

    private final SemiMonthlyPayrollStrategy semiMonthlyPayrollStrategy;

    public PayrollComputationStrategy getStrategy(PayrollFrequency frequency) {
        return switch (frequency) {
            case SEMI_MONTHLY, MONTHLY, WEEKLY, BI_WEEKLY -> semiMonthlyPayrollStrategy;
        };
    }
}
