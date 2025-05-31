import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DebounceInputDirective } from '../../directives/debounce-input.directive';

/**
 * Standalone search box component with debounced input
 */
@Component({
  selector: 'app-search-box',
  standalone: true,
  imports: [CommonModule, FormsModule, DebounceInputDirective],
  template: `
    <div class="search-box" [class.search-box-sm]="size === 'sm'" [class.search-box-lg]="size === 'lg'">
      <div class="search-input-wrapper">
        <i class="bi bi-search search-icon"></i>
        <input
          type="text"
          class="form-control search-input"
          [placeholder]="placeholder"
          [value]="value"
          appDebounceInput
          [debounceTime]="debounceTime"
          (debounceInput)="onSearch($event)"
          (keyup.enter)="onEnter()">
        <button 
          type="button" 
          class="clear-button" 
          *ngIf="value && showClear"
          (click)="onClear()">
          <i class="bi bi-x-circle"></i>
        </button>
      </div>
    </div>
  `,
  styles: [`
    .search-box {
      width: 100%;
    }

    .search-input-wrapper {
      position: relative;
      display: flex;
      align-items: center;
    }

    .search-icon {
      position: absolute;
      left: 12px;
      color: #868e96; /* Warm gray */
      pointer-events: none;
      z-index: 1;
    }

    .search-input {
      padding-left: 36px;
      padding-right: 36px;
      background-color: #1a1d29; /* Card background */
      border: 1px solid #2c2e33; /* Border color */
      color: #FFFFFF;
      transition: all 0.3s ease;
    }

    .search-input:focus {
      border-color: #00d4ff; /* Bright cyan */
      box-shadow: 0 0 0 0.25rem rgba(0, 212, 255, 0.25);
    }

    .search-input::placeholder {
      color: #868e96; /* Warm gray */
    }

    .clear-button {
      position: absolute;
      right: 8px;
      background: none;
      border: none;
      color: #868e96; /* Warm gray */
      cursor: pointer;
      padding: 4px;
      transition: color 0.2s ease;
    }

    .clear-button:hover {
      color: #FFFFFF;
    }

    /* Size variations */
    .search-box-sm .search-input {
      padding-top: 0.25rem;
      padding-bottom: 0.25rem;
      font-size: 0.875rem;
    }

    .search-box-sm .search-icon {
      font-size: 0.875rem;
    }

    .search-box-lg .search-input {
      padding-top: 0.75rem;
      padding-bottom: 0.75rem;
      font-size: 1.125rem;
    }

    .search-box-lg .search-icon {
      font-size: 1.125rem;
      left: 16px;
    }

    .search-box-lg .search-input {
      padding-left: 44px;
    }
  `]
})
export class SearchBoxComponent {
  /**
   * Current search value
   */
  @Input() value = '';
  
  /**
   * Placeholder text
   */
  @Input() placeholder = 'Search...';
  
  /**
   * Debounce time in milliseconds
   */
  @Input() debounceTime = 300;
  
  /**
   * Size variant
   */
  @Input() size: 'sm' | 'md' | 'lg' = 'md';
  
  /**
   * Whether to show clear button
   */
  @Input() showClear = true;
  
  /**
   * Event emitted when search value changes (debounced)
   */
  @Output() search = new EventEmitter<string>();
  
  /**
   * Event emitted when clear button is clicked
   */
  @Output() clear = new EventEmitter<void>();
  
  /**
   * Event emitted when enter key is pressed
   */
  @Output() enter = new EventEmitter<string>();

  /**
   * Handle search input change
   */
  onSearch(value: string): void {
    this.value = value;
    this.search.emit(value);
  }

  /**
   * Handle clear button click
   */
  onClear(): void {
    this.value = '';
    this.search.emit('');
    this.clear.emit();
  }

  /**
   * Handle enter key press
   */
  onEnter(): void {
    this.enter.emit(this.value);
  }
}