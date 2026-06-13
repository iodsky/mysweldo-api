package com.iodsky.mysweldo.payroll.strategy;

import com.iodsky.mysweldo.employee.PayType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Factory for resolving the appropriate PayBasisStrategy based on an
 * employee's pay type.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PayBasisStrategyFactory {

    private final MonthlyPayBasisStrategy monthlyPayBasisStrategy;
    private final DailyPayBasisStrategy dailyPayBasisStrategy;
    private final HourlyPayBasisStrategy hourlyPayBasisStrategy;

    /**
     * Resolves the PayBasisStrategy for the given pay type. A null pay type
     * (possible only on legacy salary rows that predate request validation)
     * falls back to the MONTHLY basis, preserving historical behavior.
     *
     * @param payType The employee's pay type, possibly null
     * @return The corresponding PayBasisStrategy
     */
    public PayBasisStrategy getStrategy(PayType payType) {
        if (payType == null) {
            log.warn("Salary has no payType set; defaulting to MONTHLY pay basis");
            return monthlyPayBasisStrategy;
        }

        return switch (payType) {
            case MONTHLY -> monthlyPayBasisStrategy;
            case DAILY -> dailyPayBasisStrategy;
            case HOURLY -> hourlyPayBasisStrategy;
        };
    }
}
