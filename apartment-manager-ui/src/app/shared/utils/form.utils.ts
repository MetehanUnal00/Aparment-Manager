import { AbstractControl, FormGroup } from '@angular/forms';

/**
 * Utility functions for working with Angular reactive forms
 */

/**
 * Mark all controls in a form group as touched
 * Useful for showing validation errors on form submission
 * @param formGroup The form group to mark as touched
 */
export function markFormGroupTouched(formGroup: FormGroup): void {
  Object.keys(formGroup.controls).forEach(key => {
    const control = formGroup.get(key);
    control?.markAsTouched();

    if (control instanceof FormGroup) {
      markFormGroupTouched(control);
    }
  });
}

/**
 * Get all validation errors from a form group
 * @param formGroup The form group to get errors from
 * @returns Object with field names as keys and error arrays as values
 */
export function getFormValidationErrors(formGroup: FormGroup): { [key: string]: any } {
  const errors: { [key: string]: any } = {};

  Object.keys(formGroup.controls).forEach(key => {
    const control = formGroup.get(key);
    
    if (control && control.errors && control.touched) {
      errors[key] = control.errors;
    }

    if (control instanceof FormGroup) {
      const groupErrors = getFormValidationErrors(control);
      if (Object.keys(groupErrors).length > 0) {
        errors[key] = groupErrors;
      }
    }
  });

  return errors;
}

/**
 * Reset form to initial values and clear validation
 * @param formGroup The form group to reset
 * @param initialValues Optional initial values to reset to
 */
export function resetForm(formGroup: FormGroup, initialValues?: any): void {
  formGroup.reset(initialValues);
  formGroup.markAsUntouched();
  formGroup.markAsPristine();
}

/**
 * Check if a form control has a specific error and is touched
 * @param control The form control to check
 * @param errorName The error name to check for
 * @returns True if the control has the error and is touched
 */
export function hasError(control: AbstractControl | null, errorName: string): boolean {
  return !!(control && control.hasError(errorName) && control.touched);
}

/**
 * Get error message for a form control
 * @param control The form control
 * @param fieldName The field name for error messages
 * @returns Error message string or null
 */
export function getErrorMessage(control: AbstractControl | null, fieldName: string): string | null {
  if (!control || !control.errors || !control.touched) {
    return null;
  }

  const errors = control.errors;
  const errorKey = Object.keys(errors)[0];
  const error = errors[errorKey];

  // Map of error messages
  const errorMessages: { [key: string]: (error: any) => string } = {
    required: () => `${fieldName} is required`,
    email: () => `Please enter a valid email address`,
    minlength: (err) => `${fieldName} must be at least ${err.requiredLength} characters`,
    maxlength: (err) => `${fieldName} must not exceed ${err.requiredLength} characters`,
    min: (err) => `${fieldName} must be at least ${err.min}`,
    max: (err) => `${fieldName} must not exceed ${err.max}`,
    pattern: () => `${fieldName} format is invalid`,
    phoneNumber: () => `Please enter a valid phone number`,
    dateRange: () => 'End date must be after start date',
    passwordMatch: () => 'Passwords do not match'
  };

  const messageFunc = errorMessages[errorKey];
  return messageFunc ? messageFunc(error) : `${fieldName} is invalid`;
}

/**
 * Disable or enable all controls in a form group
 * @param formGroup The form group
 * @param disable True to disable, false to enable
 */
export function setFormGroupDisabled(formGroup: FormGroup, disable: boolean): void {
  Object.keys(formGroup.controls).forEach(key => {
    const control = formGroup.get(key);
    
    if (disable) {
      control?.disable();
    } else {
      control?.enable();
    }

    if (control instanceof FormGroup) {
      setFormGroupDisabled(control, disable);
    }
  });
}

/**
 * Get changed values from a form group compared to initial values
 * @param formGroup The form group
 * @param initialValues The initial values to compare against
 * @returns Object containing only changed values
 */
export function getChangedValues(formGroup: FormGroup, initialValues: any): any {
  const changes: any = {};
  const currentValues = formGroup.value;

  Object.keys(currentValues).forEach(key => {
    if (currentValues[key] !== initialValues[key]) {
      changes[key] = currentValues[key];
    }
  });

  return changes;
}

/**
 * Update form values without triggering value changes
 * @param formGroup The form group
 * @param values The values to update
 */
export function updateFormValues(formGroup: FormGroup, values: any): void {
  formGroup.patchValue(values, { emitEvent: false });
}