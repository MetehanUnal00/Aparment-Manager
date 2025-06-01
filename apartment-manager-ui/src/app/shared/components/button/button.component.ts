import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';

/**
 * Standalone button component that extends Bootstrap styles with our custom theme
 * Uses the color palette defined in styles.scss
 */
@Component({
  selector: 'app-button',
  standalone: true,
  imports: [CommonModule],
  template: `
    <button
      [type]="type"
      [class]="buttonClasses"
      [disabled]="disabled || loading"
      (click)="handleClick($event)">
      <span class="spinner-border spinner-border-sm me-2" *ngIf="loading"></span>
      <i [class]="'bi bi-' + icon + ' me-2'" *ngIf="icon && !loading"></i>
      <ng-content></ng-content>
    </button>
  `,
  styles: [`
    :host {
      display: inline-block;
    }

    button {
      position: relative;
      overflow: hidden;
      transition: all 0.3s ease;
    }

    /* Ripple effect */
    button::after {
      content: '';
      position: absolute;
      top: 50%;
      left: 50%;
      width: 0;
      height: 0;
      border-radius: 50%;
      background: rgba(255, 255, 255, 0.1);
      transform: translate(-50%, -50%);
      transition: width 0.6s, height 0.6s;
    }

    button:active::after {
      width: 300px;
      height: 300px;
    }

    /* Custom button variants using our color palette */
    button.btn-accent {
      background-color: #e64980;
      border-color: #e64980;
      color: #ffffff;
    }

    button.btn-accent:hover:not(:disabled) {
      background-color: #d13366;
      border-color: #d13366;
      transform: translateY(-2px);
      box-shadow: 0 4px 12px rgba(230, 73, 128, 0.3);
    }

    button.btn-cyan {
      background-color: #00d4ff;
      border-color: #00d4ff;
      color: #0a0e1a;
    }

    button.btn-cyan:hover:not(:disabled) {
      background-color: #00a8cc;
      border-color: #00a8cc;
      transform: translateY(-2px);
      box-shadow: 0 4px 12px rgba(0, 212, 255, 0.3);
    }

    button.btn-outline-cyan {
      color: #00d4ff;
      border-color: #00d4ff;
      background-color: transparent;
    }

    button.btn-outline-cyan:hover:not(:disabled) {
      background-color: #00d4ff;
      border-color: #00d4ff;
      color: #0a0e1a;
    }

    /* Loading state */
    button.loading {
      cursor: not-allowed;
      opacity: 0.8;
    }

    /* Disabled state */
    button:disabled {
      cursor: not-allowed;
      opacity: 0.5;
    }
  `]
})
export class ButtonComponent {
  /**
   * Button type attribute
   */
  @Input() type: 'button' | 'submit' | 'reset' = 'button';
  
  /**
   * Button variant - extends Bootstrap variants with custom ones
   */
  @Input() variant: 'primary' | 'secondary' | 'success' | 'danger' | 'warning' | 'info' | 'light' | 'dark' | 'accent' | 'cyan' | 'outline-primary' | 'outline-cyan' | 'outline-light' | 'link' = 'primary';
  
  /**
   * Button size
   */
  @Input() size: 'sm' | 'md' | 'lg' = 'md';
  
  /**
   * Whether the button should take full width
   */
  @Input() fullWidth = false;
  
  /**
   * Whether the button is disabled
   */
  @Input() disabled = false;
  
  /**
   * Whether the button is in loading state
   */
  @Input() loading = false;
  
  /**
   * Optional icon to display (Bootstrap Icons name without 'bi-' prefix)
   */
  @Input() icon?: string;
  
  /**
   * Click event emitter
   */
  @Output() clicked = new EventEmitter<MouseEvent>();

  /**
   * Get computed button classes
   */
  get buttonClasses(): string {
    const classes = ['btn'];
    
    // Add variant class
    classes.push(`btn-${this.variant}`);
    
    // Add size class
    if (this.size !== 'md') {
      classes.push(`btn-${this.size}`);
    }
    
    // Add full width class
    if (this.fullWidth) {
      classes.push('w-100');
    }
    
    // Add loading class
    if (this.loading) {
      classes.push('loading');
    }
    
    return classes.join(' ');
  }

  /**
   * Handle button click
   */
  handleClick(event: MouseEvent): void {
    if (!this.disabled && !this.loading) {
      this.clicked.emit(event);
    }
  }
}