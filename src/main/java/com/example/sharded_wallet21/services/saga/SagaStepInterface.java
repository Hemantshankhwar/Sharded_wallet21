package com.example.sharded_wallet21.services.saga;

public interface SagaStepInterface {

    boolean execute(SagaContext context);

    boolean compensate(SagaContext context);

    String getStepName();
}
