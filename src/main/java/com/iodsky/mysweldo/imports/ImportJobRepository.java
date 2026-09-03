package com.iodsky.mysweldo.imports;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ImportJobRepository extends JpaRepository<ImportJob, UUID> {

    Page<ImportJob> findAllByType(ImportType type, Pageable pageable);

    Page<ImportJob> findAllByStatus(ImportStatus status, Pageable pageable);

    Page<ImportJob> findAllByTypeAndStatus(ImportType type, ImportStatus status, Pageable pageable);

}