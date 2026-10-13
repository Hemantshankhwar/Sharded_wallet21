package com.example.sharded_wallet21.services.saga.steps;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.example.sharded_wallet21.services.saga.SagaStepInterface;

@Component
public class SagaStepFactory {

    private final Map<String, SagaStepInterface> sagaStepMap;

    // Qualifier stops Spring from injecting all SagaStepInterface beans keyed by bean name instead.
    public SagaStepFactory(@Qualifier("sagaStepMap") Map<String, SagaStepInterface> sagaStepMap) {
        this.sagaStepMap = sagaStepMap;
    }

    public enum SagaStepType {
        DEBIT_SOURCE_WALLET_STEP,
        CREDIT_DESTINATION_WALLET_STEP,
        UPDATE_TRANSACTION_STATUS_STEP
    }

    public static final List<SagaStepType> TransferMoneySagaSteps = List.of(
            SagaStepType.DEBIT_SOURCE_WALLET_STEP,
            SagaStepType.CREDIT_DESTINATION_WALLET_STEP,
            SagaStepType.UPDATE_TRANSACTION_STATUS_STEP
    );

    public SagaStepInterface getSagaStep(String stepName) {
        return sagaStepMap.get(stepName);
    }
}
