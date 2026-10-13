package com.example.sharded_wallet21.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.sharded_wallet21.services.saga.SagaStepInterface;
import com.example.sharded_wallet21.services.saga.steps.CreditDestinationWalletStep;
import com.example.sharded_wallet21.services.saga.steps.DebitSourceWalletStep;
import com.example.sharded_wallet21.services.saga.steps.SagaStepFactory.SagaStepType;
import com.example.sharded_wallet21.services.saga.steps.UpdateTransactionStatus;

@Configuration
public class SagaConfiguration {

    @Bean
    public Map<String, SagaStepInterface> sagaStepMap(
            DebitSourceWalletStep debitSourceWalletStep,
            CreditDestinationWalletStep creditDestinationWalletStep,
            UpdateTransactionStatus updateTransactionStatus) {
        Map<String, SagaStepInterface> map = new HashMap<>();
        map.put(SagaStepType.DEBIT_SOURCE_WALLET_STEP.toString(), debitSourceWalletStep);
        map.put(SagaStepType.CREDIT_DESTINATION_WALLET_STEP.toString(), creditDestinationWalletStep);
        map.put(SagaStepType.UPDATE_TRANSACTION_STATUS_STEP.toString(), updateTransactionStatus);
        return map;
    }
}
