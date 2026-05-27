package com.iodsky.mysweldo.employee;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Page<EmployeeBasic> findAllByStatus(@Param("status") EmploymentStatus status, Pageable pageable);

    Page<EmployeeBasic> findAllByDepartment_Id(@Param("departmentId") String departmentId, Pageable pageable);

    Page<EmployeeBasic> findAllBySupervisor_Id(@Param("supervisorId") Long supervisorId, Pageable pageable);

    Page<EmployeeBasic> findAllByStatusNotIn(Collection<EmploymentStatus> statuses, Pageable pageable);

    @Query("""
        SELECT e.id
        FROM Employee e
        WHERE e.status NOT IN (
        com.iodsky.mysweldo.employee.EmploymentStatus.RESIGNED,
         com.iodsky.mysweldo.employee.EmploymentStatus.TERMINATED
         )
       """)
    List<Long> findAllActiveEmployeeIds();

    List<Employee> findAllBySupervisor_Id(Long supervisorId);

}
