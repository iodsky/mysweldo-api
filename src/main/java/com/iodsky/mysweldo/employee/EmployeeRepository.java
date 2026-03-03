package com.iodsky.mysweldo.employee;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Page<Employee> findAllByStatus(Status status, Pageable pageable);

    Page<Employee> findAllByDepartment_Id(String departmentId, Pageable pageable);

    Page<Employee> findAllBySupervisor_Id(Long supervisorId, Pageable pageable);

    @Query("""
        SELECT e.id
        FROM Employee e
        WHERE e.status NOT IN (
        com.iodsky.mysweldo.employee.Status.RESIGNED,
         com.iodsky.mysweldo.employee.Status.TERMINATED
         )
       """)
    List<Long> findAllActiveEmployeeIds();

    List<Employee> findAllBySupervisor_Id(Long supervisorId);

}
