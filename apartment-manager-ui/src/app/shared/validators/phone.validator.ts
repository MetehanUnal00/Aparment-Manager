import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

/**
 * Phone number validator factory function
 * Creates a validator that checks if the input is a valid phone number
 * @param country Optional country code for specific validation (default: 'US')
 * @returns Validator function
 */
export function phoneValidator(country: string = 'US'): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    if (!control.value) {
      return null; // Don't validate empty values (use required validator for that)
    }

    const value = control.value.toString();
    
    // Remove all non-numeric characters for validation
    const cleaned = value.replace(/\D/g, '');
    
    let isValid = false;
    
    switch (country) {
      case 'US':
        // US phone numbers: 10 digits
        isValid = /^[0-9]{10}$/.test(cleaned);
        break;
      case 'UK':
        // UK phone numbers: 11 digits starting with 0
        isValid = /^0[0-9]{10}$/.test(value) || /^[0-9]{11}$/.test(cleaned);
        break;
      case 'TR':
        // Turkish phone numbers: 10-11 digits, may start with 0
        isValid = /^0?[0-9]{10}$/.test(cleaned);
        break;
      case 'INTL':
        // International: 7-15 digits (ITU-T E.164)
        isValid = /^[0-9]{7,15}$/.test(cleaned);
        break;
      default:
        // Default: allow 7-15 digits
        isValid = /^[0-9]{7,15}$/.test(cleaned);
    }
    
    return isValid ? null : { phoneNumber: { value: control.value, country } };
  };
}

/**
 * US phone number validator
 */
export const usPhoneValidator: ValidatorFn = phoneValidator('US');

/**
 * UK phone number validator
 */
export const ukPhoneValidator: ValidatorFn = phoneValidator('UK');

/**
 * Turkish phone number validator
 */
export const trPhoneValidator: ValidatorFn = phoneValidator('TR');

/**
 * International phone number validator
 */
export const internationalPhoneValidator: ValidatorFn = phoneValidator('INTL');