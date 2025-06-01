/**
 * Barrel export for all model interfaces and types
 */

// Common models
export * from './common.model';

// Domain models
export * from './user.model';
export * from './apartment-building.model';
export * from './flat.model';
export * from './payment.model';
export * from './monthly-due.model';
export * from './expense.model';
export * from './dashboard.model';
export * from './contract.model';

// Re-export specific interfaces to clarify which version to use
export type { FlatSummary } from './flat.model';
export type { PaymentFlatSummary } from './payment.model';
export type { BuildingSummary } from './apartment-building.model';
export type { ExpenseBuildingSummary } from './expense.model';