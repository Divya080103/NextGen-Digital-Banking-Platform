package com.nextgen.bank.upi.repository;

import com.nextgen.bank.upi.domain.UpiProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UpiProfileRepository extends JpaRepository<UpiProfile, UUID> {

    Optional<UpiProfile> findByVpa(String vpa);

    boolean existsByVpa(String vpa);

    List<UpiProfile> findByCustomerId(UUID customerId);
}
