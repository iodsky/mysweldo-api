package com.iodsky.mysweldo.leave.request;

import com.iodsky.mysweldo.common.RequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, String> {

    Page<LeaveRequest> findAllByEmployee_Id(Long employeeId, Pageable page);

    boolean existsByEmployee_IdAndStartDateAndEndDate(Long employeeId, LocalDate startDate, LocalDate endDate);

    boolean existsByEmployee_IdAndStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Long employeeId,
            List<RequestStatus> statuses,
            LocalDate startDate,
            LocalDate endDate
    );

    Page<LeaveRequest> findAllByEmployee_Supervisor_Id(Long supervisorId, Pageable pageable);

    Page<LeaveRequest> findAllByStartDateLessThanEqualAndEndDateGreaterThanEqual(LocalDate startDate, LocalDate endDate, Pageable pageable);

}
