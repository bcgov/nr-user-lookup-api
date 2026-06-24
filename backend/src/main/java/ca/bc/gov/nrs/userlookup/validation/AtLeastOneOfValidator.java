package ca.bc.gov.nrs.userlookup.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.BeanWrapperImpl;

/**
 * Validates {@link AtLeastOneOf}: passes when at least one of the configured
 * fields is non-null and, for {@link String} values, non-blank.
 */
public class AtLeastOneOfValidator implements ConstraintValidator<AtLeastOneOf, Object> {

  private String[] fields;

  @Override
  public void initialize(AtLeastOneOf constraintAnnotation) {
    this.fields = constraintAnnotation.fields();
  }

  @Override
  public boolean isValid(Object value, ConstraintValidatorContext context) {
    if (value == null) {
      return true;
    }
    BeanWrapperImpl wrapper = new BeanWrapperImpl(value);
    for (String field : fields) {
      Object fieldValue = wrapper.getPropertyValue(field);
      if (fieldValue instanceof String str) {
        if (!str.isBlank()) {
          return true;
        }
      } else if (fieldValue != null) {
        return true;
      }
    }
    return false;
  }
}
