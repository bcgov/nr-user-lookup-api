package ca.bc.gov.nrs.userlookup.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Class-level constraint requiring at least one of the named fields to be
 * present (non-null and, for strings, non-blank). Ports the proxy's
 * {@code @AtLeastOneOf} decorator.
 */
@Documented
@Constraint(validatedBy = AtLeastOneOfValidator.class)
@Target(TYPE)
@Retention(RUNTIME)
public @interface AtLeastOneOf {

  String message() default "At least one of the specified fields must be provided.";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};

  /** The field names, at least one of which must be supplied. */
  String[] fields();
}
