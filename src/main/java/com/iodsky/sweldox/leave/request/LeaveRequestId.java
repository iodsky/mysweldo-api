package com.iodsky.sweldox.leave.request;

import org.hibernate.annotations.IdGeneratorType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom ID generation strategy for LeaveRequest entity.
 */

// Specifies where to use this annotation. In this case in Fields (String id) and Methods (Getter)
@Target({ElementType.FIELD, ElementType.METHOD})
// This keeps the annotation available at runtime (not just compile-time).
@Retention(RetentionPolicy.RUNTIME)
// Hibernate annotation that links the custom annotation to the ID generator class i.e., LeaveRequestIdGenerator
@IdGeneratorType(LeaveRequestIdGenerator.class)
// The @interface declares a custom annotation
public @interface LeaveRequestId  { }
