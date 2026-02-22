package com.iodsky.sweldox.overtime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.UUID;

@Repository
public interface OvertimeRequestRepository extends JpaRepository<OvertimeRequest, UUID> {

    boolean existsByEmployee_IdAndDate(Long employeeId, LocalDate date);

    Page<OvertimeRequest> findByDateBetween(LocalDate startDate, LocalDate endDate, Pageable pageable);

    Page<OvertimeRequest> findAllByEmployee_Id(Long employeeId, Pageable pageable);

    Page<OvertimeRequest> findByEmployee_Supervisor_Id(Long supervisorId, Pageable pageable);

}
