package com.iodsky.sweldox.leave.request;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;

import java.time.Instant;
import java.util.UUID;

public class LeaveRequestIdGenerator implements IdentifierGenerator {

    @Override
    public Object generate(SharedSessionContractImplementor sharedSessionContractImplementor, Object o) {
        LeaveRequest leaveRequest = (LeaveRequest) o;

        return Instant.now() + "-" +
                leaveRequest.getEmployee().getId() + "-" +
                UUID.randomUUID().toString().substring(0, 8);
    }
}
