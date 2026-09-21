package com.transferapp.transferservice.repository;

import com.transferapp.transferservice.entity.Transfer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TransferRepository extends JpaRepository<Transfer,UUID> {
}
