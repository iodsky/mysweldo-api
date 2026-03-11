package com.iodsky.mysweldo.payroll.core;

import com.iodsky.mysweldo.pagIbig.PagibigRateTable;
import com.iodsky.mysweldo.philhealth.PhilhealthRateTable;
import com.iodsky.mysweldo.sss.SssRate;
import com.iodsky.mysweldo.tax.TaxBracket;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Holds preloaded payroll configuration data to avoid repeated database queries
 * when calculating multiple payrolls for the same period.
 */
@Getter
@Builder
public class PayrollConfiguration {
    private PhilhealthRateTable philhealthRateTable;
    private PagibigRateTable pagibigRateTable;
    private SssRate sssRateTable;
    private List<TaxBracket> incomeTaxBrackets;
}
