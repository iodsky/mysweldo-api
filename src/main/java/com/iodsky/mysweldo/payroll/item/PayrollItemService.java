package com.iodsky.mysweldo.payroll.item;

import com.iodsky.mysweldo.security.user.User;
import com.iodsky.mysweldo.security.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;

@Service
@RequiredArgsConstructor
public class PayrollItemService {


    private final PayrollItemRepository payrollRepository;
    private final UserService userService;

    public Page<PayrollItem> getAllEmployeePayroll(int page, int limit, YearMonth period) {
        User user = userService.getAuthenticatedUser();
        Pageable pageable = PageRequest.of(page, limit);

        Long id = user.getEmployee().getId();

        if (period != null) {
            LocalDate startDate = period.atDay(1);
            LocalDate endDate = period.atEndOfMonth();

            return payrollRepository.findAllByEmployee_IdAndPayrollRun_Period_StartDateLessThanEqualAndPayrollRun_Period_EndDateGreaterThanEqual(
                    id,
                    endDate,
                    startDate,
                    pageable
            );
        }

        return payrollRepository.findAllByEmployee_Id(id, pageable);
    }

}
