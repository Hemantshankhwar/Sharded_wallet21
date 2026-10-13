package com.example.sharded_wallet21.services.saga;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.sharded_wallet21.entities.SagaInstance;
import com.example.sharded_wallet21.entities.SagaStatus;
import com.example.sharded_wallet21.entities.SagaStep;
import com.example.sharded_wallet21.entities.StepStatus;
import com.example.sharded_wallet21.repositories.SagaInstanceRepository;
import com.example.sharded_wallet21.repositories.SagaStepRepository;
import com.example.sharded_wallet21.services.saga.steps.SagaStepFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SagaOrchestratorImpl implements SagaOrchestrator {

    private final ObjectMapper objectMapper;
    private final SagaInstanceRepository sagaInstanceRepository;
    private final SagaStepRepository sagaStepRepository;
    private final SagaStepFactory sagaStepFactory;

    @Override
    @Transactional
    public Long startSaga(SagaContext context) {
        try {
            String contextJson = objectMapper.writeValueAsString(context); // convert the context to a json as a string
            SagaInstance sagaInstance = SagaInstance
                    .builder()
                    .context(contextJson)
                    .status(SagaStatus.STARTED)
                    .build();

            sagaInstance = sagaInstanceRepository.save(sagaInstance);

            log.info("Started saga with id {}", sagaInstance.getId());

            return sagaInstance.getId();

        } catch (Exception e) {
            log.error("Error starting saga", e);
            throw new RuntimeException("Error starting saga", e);
        }
    }

    @Override
    @Transactional
    public boolean executeStep(Long sagaInstanceId, String stepName) {
        SagaInstance sagaInstance = sagaInstanceRepository.findById(sagaInstanceId)
                .orElseThrow(() -> new RuntimeException("Saga instance not found"));

        SagaStepInterface step = sagaStepFactory.getSagaStep(stepName);
        if (step == null) {
            log.error("Saga step not found for step name {}", stepName);
            throw new RuntimeException("Saga step not found");
        }

        SagaStep sagaStepDB = sagaStepRepository
                .findBySagaInstanceIdAndStepNameAndStatus(sagaInstanceId, stepName, StepStatus.PENDING)
                .orElse(
                        SagaStep.builder().sagaInstanceId(sagaInstanceId).stepName(stepName).status(StepStatus.PENDING).build()
                );

        if (sagaStepDB.getId() == null) {
            sagaStepDB = sagaStepRepository.save(sagaStepDB);
        }

        try {
            SagaContext sagaContext = objectMapper.readValue(sagaInstance.getContext(), SagaContext.class);
            sagaStepDB.setStatus(StepStatus.RUNNING);
            sagaStepRepository.save(sagaStepDB); // updating the status to running in db

            boolean success = step.execute(sagaContext);

            if (success) {
                sagaStepDB.setStatus(StepStatus.COMPLETED);
                sagaStepRepository.save(sagaStepDB); // updating the status to completed in db

                sagaInstance.setContext(objectMapper.writeValueAsString(sagaContext)); // steps add data that compensation needs
                sagaInstance.setCurrentStep(stepName); // step we just completed
                sagaInstance.setStatus(SagaStatus.RUNNING);
                sagaInstanceRepository.save(sagaInstance); // updating the status to running in db

                log.info("Step {} executed successfully", stepName);
                return true;
            } else {
                sagaStepDB.setStatus(StepStatus.FAILED);
                sagaStepRepository.save(sagaStepDB); // updating the status to failed in db
                log.error("Step {} failed", stepName);
                return false;
            }

        } catch (Exception e) {
            sagaStepDB.setStatus(StepStatus.FAILED);
            sagaStepRepository.save(sagaStepDB);
            log.error("Failed to execute step {}", stepName, e);
            return false;
        }
    }

    @Override
    @Transactional
    public boolean compensateStep(Long sagaInstanceId, String stepName) {
        // 1. Get the saga instance and the step implementation
        SagaInstance sagaInstance = sagaInstanceRepository.findById(sagaInstanceId)
                .orElseThrow(() -> new RuntimeException("Saga instance not found"));

        SagaStepInterface step = sagaStepFactory.getSagaStep(stepName);
        if (step == null) {
            log.error("Saga step not found for step name {}", stepName);
            throw new RuntimeException("Saga step not found");
        }

        // 2. Get the completed step record from db
        SagaStep sagaStepDB = sagaStepRepository
                .findBySagaInstanceIdAndStepNameAndStatus(sagaInstanceId, stepName, StepStatus.COMPLETED)
                .orElse(null);

        if (sagaStepDB == null) {
            log.error("No completed step {} found for saga {}", stepName, sagaInstanceId);
            return false;
        }

        try {
            // 3. Take the context from saga instance and call the compensate method of the step
            SagaContext sagaContext = objectMapper.readValue(sagaInstance.getContext(), SagaContext.class);
            sagaStepDB.setStatus(StepStatus.COMPENSATING);
            sagaStepRepository.save(sagaStepDB);

            boolean success = step.compensate(sagaContext);

            // 4. Update the appropriate status in the saga step
            if (success) {
                sagaStepDB.setStatus(StepStatus.COMPENSATED);
                sagaStepRepository.save(sagaStepDB);
                log.info("Step {} compensated successfully", stepName);
                return true;
            } else {
                sagaStepDB.setStatus(StepStatus.FAILED);
                sagaStepRepository.save(sagaStepDB);
                log.error("Compensation of step {} failed", stepName);
                return false;
            }

        } catch (Exception e) {
            sagaStepDB.setStatus(StepStatus.FAILED);
            sagaStepRepository.save(sagaStepDB);
            log.error("Failed to compensate step {}", stepName, e);
            return false;
        }
    }

    @Override
    @Transactional
    public SagaInstance getSagaInstance(Long sagaInstanceId) {
        return sagaInstanceRepository.findById(sagaInstanceId)
                .orElseThrow(() -> new RuntimeException("Saga instance not found"));
    }

    @Override
    @Transactional
    public void compensateSaga(Long sagaInstanceId) {
        SagaInstance sagaInstance = sagaInstanceRepository.findById(sagaInstanceId)
                .orElseThrow(() -> new RuntimeException("Saga instance not found"));

        // mark the saga status as compensating in db
        sagaInstance.setStatus(SagaStatus.COMPENSATING);
        sagaInstanceRepository.save(sagaInstance);

        // get all the completed steps
        List<SagaStep> completedSteps = sagaStepRepository.findCompletedStepsBySagaInstanceId(sagaInstanceId);

        boolean allCompensated = true;
        for (SagaStep completedStep : completedSteps) {
            boolean compensated = this.compensateStep(sagaInstanceId, completedStep.getStepName());
            if (!compensated) {
                allCompensated = false;
            }
        }

        if (allCompensated) {
            sagaInstance.setStatus(SagaStatus.COMPENSATED);
            sagaInstanceRepository.save(sagaInstance);
            log.info("Saga {} compensated successfully", sagaInstanceId);
        } else {
            log.error("Saga {} compensation failed", sagaInstanceId);
        }
    }

    @Override
    @Transactional
    public void failSaga(Long sagaInstanceId) {
        SagaInstance sagaInstance = sagaInstanceRepository.findById(sagaInstanceId)
                .orElseThrow(() -> new RuntimeException("Saga instance not found"));
        sagaInstance.markAsFailed();
        sagaInstanceRepository.save(sagaInstance);

        compensateSaga(sagaInstanceId);

        log.info("Saga {} failed", sagaInstanceId);
    }

    @Override
    @Transactional
    public void completeSaga(Long sagaInstanceId) {
        SagaInstance sagaInstance = sagaInstanceRepository.findById(sagaInstanceId)
                .orElseThrow(() -> new RuntimeException("Saga instance not found"));
        sagaInstance.markAsCompleted();
        sagaInstanceRepository.save(sagaInstance);
    }
}
