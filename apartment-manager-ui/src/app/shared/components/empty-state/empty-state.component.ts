import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ButtonComponent } from '../button/button.component';

/**
 * Standalone component for displaying empty states in the application
 * Uses the dark theme color palette
 */
@Component({
  selector: 'app-empty-state',
  standalone: true,
  imports: [CommonModule, ButtonComponent],
  template: `
    <div class="empty-state">
      <div class="empty-state-icon" *ngIf="icon">
        <i [class]="'bi bi-' + icon"></i>
      </div>
      <h5 class="empty-state-title">{{ title }}</h5>
      <p class="empty-state-message" *ngIf="message">{{ message }}</p>
      <app-button 
        *ngIf="actionLabel" 
        [variant]="actionVariant"
        [icon]="actionIcon"
        (clicked)="onAction()">
        {{ actionLabel }}
      </app-button>
    </div>
  `,
  styles: [`
    .empty-state {
      text-align: center;
      padding: 48px 24px;
      max-width: 400px;
      margin: 0 auto;
    }

    .empty-state-icon {
      margin-bottom: 24px;
    }

    .empty-state-icon i {
      font-size: 64px;
      color: #868e96; /* Warm gray */
      opacity: 0.5;
    }

    .empty-state-title {
      color: #FFFFFF;
      margin-bottom: 8px;
      font-weight: 500;
    }

    .empty-state-message {
      color: #868e96; /* Warm gray */
      margin-bottom: 24px;
      line-height: 1.5;
    }
  `]
})
export class EmptyStateComponent {
  /**
   * Bootstrap icon name (without 'bi-' prefix)
   */
  @Input() icon?: string;
  
  /**
   * Title text
   */
  @Input() title = 'No data found';
  
  /**
   * Optional message text
   */
  @Input() message?: string;
  
  /**
   * Optional action button label
   */
  @Input() actionLabel?: string;
  
  /**
   * Action button variant
   */
  @Input() actionVariant: 'primary' | 'accent' | 'cyan' = 'primary';
  
  /**
   * Optional action button icon
   */
  @Input() actionIcon?: string;
  
  /**
   * Action handler function
   */
  @Input() actionHandler?: () => void;

  /**
   * Handle action button click
   */
  onAction(): void {
    if (this.actionHandler) {
      this.actionHandler();
    }
  }
}