import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

/**
 * Date range validator factory function
 * Validates that end date is after start date
 * @param startDateField Name of the start date field
 * @param endDateField Name of the end date field
 * @returns Validator function
 */
export function dateRangeValidator(startDateField: string, endDateField: string): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const startDate = control.get(startDateField)?.value;
    const endDate = control.get(endDateField)?.value;
    
    if (!startDate || !endDate) {
      return null; // Don't validate if either date is missing
    }
    
    const start = new Date(startDate);
    const end = new Date(endDate);
    
    if (isNaN(start.getTime()) || isNaN(end.getTime())) {
      return null; // Don't validate if dates are invalid
    }
    
    if (end < start) {
      return { 
        dateRange: { 
          startDate: startDate, 
          endDate: endDate,
          message: 'End date must be after start date'
        } 
      };
    }
    
    return null;
  };
}

/**
 * Future date validator
 * Validates that a date is in the future
 */
export function futureDateValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    if (!control.value) {
      return null;
    }
    
    const date = new Date(control.value);
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    
    if (isNaN(date.getTime())) {
      return null; // Don't validate invalid dates
    }
    
    if (date <= today) {
      return { 
        futureDate: { 
          value: control.value,
          message: 'Date must be in the future'
        } 
      };
    }
    
    return null;
  };
}

/**
 * Past date validator
 * Validates that a date is in the past
 */
export function pastDateValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    if (!control.value) {
      return null;
    }
    
    const date = new Date(control.value);
    const today = new Date();
    today.setHours(23, 59, 59, 999);
    
    if (isNaN(date.getTime())) {
      return null; // Don't validate invalid dates
    }
    
    if (date >= today) {
      return { 
        pastDate: { 
          value: control.value,
          message: 'Date must be in the past'
        } 
      };
    }
    
    return null;
  };
}

/**
 * Min date validator factory
 * Validates that a date is after a minimum date
 */
export function minDateValidator(minDate: Date | string): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    if (!control.value) {
      return null;
    }
    
    const date = new Date(control.value);
    const min = new Date(minDate);
    
    if (isNaN(date.getTime()) || isNaN(min.getTime())) {
      return null;
    }
    
    if (date < min) {
      return { 
        minDate: { 
          value: control.value,
          min: minDate,
          message: `Date must be after ${min.toLocaleDateString()}`
        } 
      };
    }
    
    return null;
  };
}

/**
 * Max date validator factory
 * Validates that a date is before a maximum date
 */
export function maxDateValidator(maxDate: Date | string): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    if (!control.value) {
      return null;
    }
    
    const date = new Date(control.value);
    const max = new Date(maxDate);
    
    if (isNaN(date.getTime()) || isNaN(max.getTime())) {
      return null;
    }
    
    if (date > max) {
      return { 
        maxDate: { 
          value: control.value,
          max: maxDate,
          message: `Date must be before ${max.toLocaleDateString()}`
        } 
      };
    }
    
    return null;
  };
}