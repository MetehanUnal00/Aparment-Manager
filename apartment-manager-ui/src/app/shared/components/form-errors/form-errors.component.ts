import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ValidationErrors } from '@angular/forms';

/**
 * Standalone component for displaying form validation errors
 * Automatically formats and displays appropriate error messages
 */
@Component({
  selector: 'app-form-errors',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="form-errors" *ngIf="errors && showErrors">
      <p class="error-message" *ngFor="let error of errorMessages">
        {{ error }}
      </p>
    </div>
  `,
  styles: [`
    .form-errors {
      margin-top: 4px;
    }

    .error-message {
      color: #e64980; /* Error color - pink */
      font-size: 12px;
      margin: 2px 0;
      display: flex;
      align-items: center;
      gap: 4px;
    }

    .error-message::before {
      content: '⚠';
      font-size: 14px;
    }
  `]
})
export class FormErrorsComponent {
  /**
   * Validation errors object from form control
   */
  @Input() errors: ValidationErrors | null = null;
  
  /**
   * Whether to show errors (typically when field is touched)
   */
  @Input() showErrors = true;
  
  /**
   * Field name for more descriptive error messages
   */
  @Input() fieldName = 'Field';

  /**
   * Get formatted error messages
   */
  get errorMessages(): string[] {
    if (!this.errors) return [];
    
    const messages: string[] = [];
    
    Object.keys(this.errors).forEach(key => {
      const error = this.errors![key];
      messages.push(this.getErrorMessage(key, error));
    });
    
    return messages;
  }

  /**
   * Get human-readable error message for validation error
   */
  private getErrorMessage(errorKey: string, errorValue: any): string {
    const errorMessages: { [key: string]: (error: any) => string } = {
      required: () => `${this.fieldName} is required`,
      email: () => `${this.fieldName} must be a valid email address`,
      minlength: (error) => `${this.fieldName} must be at least ${error.requiredLength} characters`,
      maxlength: (error) => `${this.fieldName} must not exceed ${error.requiredLength} characters`,
      min: (error) => `${this.fieldName} must be at least ${error.min}`,
      max: (error) => `${this.fieldName} must not exceed ${error.max}`,
      pattern: () => `${this.fieldName} format is invalid`,
      phoneNumber: () => `${this.fieldName} must be a valid phone number`,
      dateRange: () => 'End date must be after start date',
      passwordMatch: () => 'Passwords do not match',
      unique: () => `${this.fieldName} already exists`,
      futureDate: () => `${this.fieldName} must be a future date`,
      pastDate: () => `${this.fieldName} must be a past date`,
      numeric: () => `${this.fieldName} must be a number`,
      alphanumeric: () => `${this.fieldName} must contain only letters and numbers`,
      url: () => `${this.fieldName} must be a valid URL`,
      strongPassword: () => `${this.fieldName} must contain uppercase, lowercase, number, and special character`
    };

    const messageFunc = errorMessages[errorKey];
    return messageFunc ? messageFunc(errorValue) : `${this.fieldName} is invalid`;
  }
}