import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

/**
 * Standalone loading spinner component for indicating loading states
 */
@Component({
  selector: 'app-loading-spinner',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="loading-spinner-container" [class.fullscreen]="fullscreen">
      <div class="loading-spinner" [class.small]="size === 'small'" [class.large]="size === 'large'">
        <div class="spinner"></div>
        <p class="loading-text" *ngIf="text">{{ text }}</p>
      </div>
    </div>
  `,
  styles: [`
    .loading-spinner-container {
      display: flex;
      justify-content: center;
      align-items: center;
      padding: 20px;
    }

    .loading-spinner-container.fullscreen {
      position: fixed;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: rgba(255, 255, 255, 0.9);
      z-index: 9999;
    }

    .loading-spinner {
      text-align: center;
    }

    .spinner {
      display: inline-block;
      width: 40px;
      height: 40px;
      border: 3px solid rgba(0, 0, 0, 0.1);
      border-radius: 50%;
      border-top-color: #3f51b5;
      animation: spin 1s ease-in-out infinite;
    }

    .loading-spinner.small .spinner {
      width: 20px;
      height: 20px;
      border-width: 2px;
    }

    .loading-spinner.large .spinner {
      width: 60px;
      height: 60px;
      border-width: 4px;
    }

    .loading-text {
      margin-top: 10px;
      color: #666;
      font-size: 14px;
    }

    @keyframes spin {
      to { transform: rotate(360deg); }
    }
  `]
})
export class LoadingSpinnerComponent {
  /**
   * Size of the spinner: 'small', 'medium', or 'large'
   */
  @Input() size: 'small' | 'medium' | 'large' = 'medium';
  
  /**
   * Optional loading text to display below spinner
   */
  @Input() text?: string;
  
  /**
   * Whether to show as fullscreen overlay
   */
  @Input() fullscreen = false;
}