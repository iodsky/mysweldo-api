package com.iodsky.mysweldo.payroll.calc;

import com.iodsky.mysweldo.contribution.Contribution;
import com.iodsky.mysweldo.deduction.Deduction;
import com.iodsky.mysweldo.pagIbig.PagibigRate;
import com.iodsky.mysweldo.philhealth.PhilhealthRate;
import com.iodsky.mysweldo.sss.SssRate;
import com.iodsky.mysweldo.tax.TaxBracket;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/** Preloaded statutory rates and deduction/contribution references for a payroll run, shared across all employees. */
@Getter
@Builder
public class StatutoryRateSnapshot {
    private PhilhealthRate philhealthRateTable;
    private PagibigRate pagibigRateTable;
    private SssRate sssRateTable;
    private List<TaxBracket> incomeTaxBrackets;

    private Deduction sssDeduction;
    private Deduction phicDeduction;
    private Deduction hdmfDeduction;
    private Deduction taxDeduction;

    private Contribution sssErContribution;
    private Contribution phicErContribution;
    private Contribution hdmfErContribution;
}
