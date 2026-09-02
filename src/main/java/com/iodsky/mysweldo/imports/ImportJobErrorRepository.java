package com.iodsky.mysweldo.imports;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ImportJobErrorRepository extends JpaRepository<ImportJobError, UUID> {

    List<ImportJobError> findAllByImportJob_IdOrderByRowNumberAsc(UUID importJobId);

}