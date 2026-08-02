package com.nextgen.bank.common.web;

import java.util.Optional;

public interface IdempotencyStore {

    boolean exists(String idempotencyKey);

    Optional<String> getCachedResponse(String idempotencyKey);

    void save(String idempotencyKey, String responsePayload, int statusCode);
}
