package com.iodsky.mysweldo.payroll.strategy;

import com.iodsky.mysweldo.employee.PayType;
import com.iodsky.mysweldo.payroll.calc.PayrollCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PayBasisStrategyFactoryTest {

    private MonthlyPayBasisStrategy monthly;
    private DailyPayBasisStrategy daily;
    private HourlyPayBasisStrategy hourly;
    private PayBasisStrategyFactory factory;

    @BeforeEach
    void setUp() {
        PayrollCalculator calculator = new PayrollCalculator();
        monthly = new MonthlyPayBasisStrategy(calculator);
        daily = new DailyPayBasisStrategy(calculator);
        hourly = new HourlyPayBasisStrategy(calculator);
        factory = new PayBasisStrategyFactory(monthly, daily, hourly);
    }

    @Test
    void getStrategy_resolvesEachPayType() {
        assertThat(factory.getStrategy(PayType.MONTHLY)).isSameAs(monthly);
        assertThat(factory.getStrategy(PayType.DAILY)).isSameAs(daily);
        assertThat(factory.getStrategy(PayType.HOURLY)).isSameAs(hourly);
    }

    @Test
    void getStrategy_nullPayTypeDefaultsToMonthly() {
        assertThat(factory.getStrategy(null)).isSameAs(monthly);
    }
}
