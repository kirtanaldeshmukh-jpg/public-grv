package com.resolvetrack.officer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OfficerRepository extends JpaRepository<Officer, Long> {
    Optional<Officer> findByUserId(Long userId);
    Optional<Officer> findByEmployeeCode(String employeeCode);
    List<Officer> findAllByDepartmentId(Long departmentId);
}

