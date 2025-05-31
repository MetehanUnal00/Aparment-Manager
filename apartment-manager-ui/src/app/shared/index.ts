/**
 * Barrel export for all shared standalone components, pipes, directives, and utilities
 */

// Components
export * from './components/loading-spinner/loading-spinner.component';
export * from './components/confirm-dialog/confirm-dialog.component';
export * from './components/button/button.component';
export * from './components/form-errors/form-errors.component';
export * from './components/empty-state/empty-state.component';
export * from './components/search-box/search-box.component';

// Directives
export * from './directives/autofocus.directive';
export * from './directives/debounce-input.directive';

// Pipes
export * from './pipes/currency-format.pipe';
export * from './pipes/phone-format.pipe';

// Validators
export * from './validators/phone.validator';
export * from './validators/date-range.validator';

// Utils
export * from './utils/form.utils';