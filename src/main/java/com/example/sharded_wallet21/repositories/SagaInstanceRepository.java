package com.example.sharded_wallet21.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.sharded_wallet21.entities.SagaInstance;

public interface SagaInstanceRepository extends JpaRepository<SagaInstance, Long> {
}
