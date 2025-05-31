import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';

/**
 * Standalone confirmation dialog component for user confirmations
 * Uses the project's dark theme color palette
 */
@Component({
  selector: 'app-confirm-dialog',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="confirm-dialog-overlay" *ngIf="isOpen" (click)="onCancel()">
      <div class="confirm-dialog" (click)="$event.stopPropagation()">
        <div class="dialog-header">
          <h3>{{ title }}</h3>
        </div>
        <div class="dialog-content">
          <p>{{ message }}</p>
        </div>
        <div class="dialog-actions">
          <button 
            type="button" 
            class="btn btn-secondary" 
            (click)="onCancel()">
            {{ cancelText }}
          </button>
          <button 
            type="button" 
            class="btn" 
            [class.btn-danger]="isDestructive" 
            [class.btn-primary]="!isDestructive"
            (click)="onConfirm()">
            {{ confirmText }}
          </button>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .confirm-dialog-overlay {
      position: fixed;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: rgba(10, 14, 26, 0.8); /* Dark overlay using primary dark color */
      display: flex;
      justify-content: center;
      align-items: center;
      z-index: 1000;
      animation: fadeIn 0.2s ease-out;
    }

    .confirm-dialog {
      background: #1a1d29; /* Card background color */
      border-radius: 8px;
      border: 1px solid #2c2e33; /* Border color */
      box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.5), 0 4px 6px -2px rgba(0, 0, 0, 0.3);
      max-width: 400px;
      width: 90%;
      animation: slideIn 0.3s ease-out;
    }

    .dialog-header {
      padding: 20px;
      border-bottom: 1px solid #2c2e33; /* Border color */
    }

    .dialog-header h3 {
      margin: 0;
      font-size: 1.25rem;
      font-weight: 500;
      color: #FFFFFF; /* White text */
    }

    .dialog-content {
      padding: 20px;
    }

    .dialog-content p {
      margin: 0;
      color: #868e96; /* Gray text */
      line-height: 1.5;
    }

    .dialog-actions {
      padding: 15px 20px;
      display: flex;
      justify-content: flex-end;
      gap: 10px;
      border-top: 1px solid #2c2e33; /* Border color */
    }

    .btn {
      padding: 8px 16px;
      border: none;
      border-radius: 4px;
      font-size: 14px;
      font-weight: 500;
      cursor: pointer;
      transition: all 0.2s ease;
    }

    .btn:hover {
      transform: translateY(-1px);
      box-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
    }

    .btn-primary {
      background: #4dabf7; /* Electric blue */
      color: #0a0e1a; /* Dark text on light button */
    }

    .btn-primary:hover {
      background: #339af0; /* Darker blue on hover */
    }

    .btn-danger {
      background: #e64980; /* Pink for destructive actions */
      color: white;
    }

    .btn-danger:hover {
      background: #c92a5a; /* Darker pink on hover */
    }

    .btn-secondary {
      background: transparent;
      color: #868e96; /* Gray text */
      border: 1px solid #2c2e33; /* Border color */
    }

    .btn-secondary:hover {
      background: rgba(134, 142, 150, 0.1); /* Slight gray background on hover */
      color: #FFFFFF;
    }

    @keyframes fadeIn {
      from { opacity: 0; }
      to { opacity: 1; }
    }

    @keyframes slideIn {
      from {
        transform: translateY(-20px);
        opacity: 0;
      }
      to {
        transform: translateY(0);
        opacity: 1;
      }
    }
  `]
})
export class ConfirmDialogComponent {
  /**
   * Whether the dialog is open
   */
  @Input() isOpen = false;
  
  /**
   * Dialog title
   */
  @Input() title = 'Confirm Action';
  
  /**
   * Dialog message
   */
  @Input() message = 'Are you sure you want to proceed?';
  
  /**
   * Confirm button text
   */
  @Input() confirmText = 'Confirm';
  
  /**
   * Cancel button text
   */
  @Input() cancelText = 'Cancel';
  
  /**
   * Whether this is a destructive action (changes button color to red)
   */
  @Input() isDestructive = false;
  
  /**
   * Event emitted when user confirms
   */
  @Output() confirm = new EventEmitter<void>();
  
  /**
   * Event emitted when user cancels
   */
  @Output() cancel = new EventEmitter<void>();

  /**
   * Handle confirm action
   */
  onConfirm(): void {
    this.confirm.emit();
    this.isOpen = false;
  }

  /**
   * Handle cancel action
   */
  onCancel(): void {
    this.cancel.emit();
    this.isOpen = false;
  }
}