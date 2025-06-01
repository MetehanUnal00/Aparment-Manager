/**
 * Contract models for the apartment management system
 * These models correspond to the backend contract entities and DTOs
 */

import { PaginationParams, PaginatedResponse } from './common.model';

/**
 * Contract status enumeration
 * Represents the lifecycle states of a contract
 */
export enum ContractStatus {
  PENDING = 'PENDING',           // Contract with future start date
  ACTIVE = 'ACTIVE',             // Currently active contract
  EXPIRED = 'EXPIRED',           // Contract past end date
  CANCELLED = 'CANCELLED',       // Manually cancelled contract
  RENEWED = 'RENEWED',           // Contract that has been renewed
  SUPERSEDED = 'SUPERSEDED'      // Contract replaced by modification
}

/**
 * Cancellation reason categories
 * Used when cancelling a contract
 */
export enum CancellationReasonCategory {
  TENANT_REQUEST = 'TENANT_REQUEST',
  NON_PAYMENT = 'NON_PAYMENT',
  BREACH_OF_CONTRACT = 'BREACH_OF_CONTRACT',
  PROPERTY_DAMAGE = 'PROPERTY_DAMAGE',
  RENOVATION = 'RENOVATION',
  MUTUAL_AGREEMENT = 'MUTUAL_AGREEMENT',
  BUILDING_CLOSURE = 'BUILDING_CLOSURE',
  OTHER = 'OTHER'
}

/**
 * Contract urgency levels for notifications
 */
export enum ContractUrgencyLevel {
  LOW = 'LOW',             // More than 15 days until expiry
  MEDIUM = 'MEDIUM',       // 8-15 days until expiry
  HIGH = 'HIGH',           // 4-7 days until expiry
  CRITICAL = 'CRITICAL'    // Less than 4 days until expiry
}

/**
 * Modification reason enumeration
 * Used when modifying contract terms
 */
export enum ModificationReason {
  ANNUAL_INCREASE = 'ANNUAL_INCREASE',
  MARKET_ADJUSTMENT = 'MARKET_ADJUSTMENT',
  NEGOTIATED_CHANGE = 'NEGOTIATED_CHANGE',
  SERVICE_ADDITION = 'SERVICE_ADDITION',
  SERVICE_REMOVAL = 'SERVICE_REMOVAL',
  ERROR_CORRECTION = 'ERROR_CORRECTION',
  OTHER = 'OTHER'
}

/**
 * Request model for creating a new contract
 */
export interface ContractRequest {
  flatId: number;
  startDate: string;  // ISO date format
  endDate: string;    // ISO date format
  monthlyRent: number;
  dayOfMonth: number; // 1-31, day of month for due generation
  securityDeposit?: number;
  tenantName: string;
  tenantContact?: string;
  tenantEmail?: string;
  notes?: string;
  generateDuesImmediately: boolean;
}

/**
 * Request model for renewing a contract
 */
export interface ContractRenewalRequest {
  newEndDate: string;          // ISO date format
  newMonthlyRent?: number;     // Optional rent adjustment
  newSecurityDeposit?: number; // Optional security deposit change
  keepSameDayOfMonth: boolean; // Whether to keep same payment day
  newDayOfMonth?: number;      // New payment day if changing
  renewalNotes?: string;
  generateDuesImmediately: boolean;
}

/**
 * Request model for cancelling a contract
 */
export interface ContractCancellationRequest {
  reasonCategory: CancellationReasonCategory;
  cancellationReason: string;
  effectiveDate?: string;      // ISO date format
  cancelUnpaidDues: boolean;
  refundSecurityDeposit: boolean;
  securityDepositDeduction?: number;
  notes?: string;
}

/**
 * Request model for modifying a contract
 */
export interface ContractModificationRequest {
  effectiveDate: string;       // ISO date format
  newMonthlyRent: number;
  reason: ModificationReason;  // Enum for modification reason
  modificationDetails: string; // Detailed explanation
  keepOtherTerms: boolean;     // Whether to keep other contract terms
  newSecurityDeposit?: number; // Optional new security deposit
  newDayOfMonth?: number;      // Optional new payment day
  notes?: string;              // Additional notes
  endDate?: string;            // Optional new end date
  regenerateDues: boolean;
}

/**
 * Full contract response model
 */
export interface ContractResponse {
  id: number;
  flatId: number;
  flatNumber: string;
  buildingId: number;
  buildingName: string;
  startDate: string;
  endDate: string;
  contractLengthInMonths: number;
  monthlyRent: number;
  securityDeposit: number;
  dayOfMonth: number;
  status: ContractStatus;
  tenantName: string;
  tenantContact?: string;
  tenantEmail?: string;
  notes?: string;
  duesGenerated: boolean;
  previousContractId?: number;
  createdAt: string;
  updatedAt?: string;
  cancellationDate?: string;
  cancellationReason?: string;
  cancellationCategory?: CancellationReasonCategory;
  lastStatusChangeDate?: string;
  statusChangedAt?: string;
  statusChangedBy?: string;
  statusChangeReason?: string;
  
  // Calculated fields from backend
  statusDisplayName?: string;
  cancelledByUsername?: string;
  totalAmountDue?: number;
  totalAmountPaid?: number;
  nextDueDate?: string;
  paidDuesCount?: number;
  unpaidDuesCount?: number;
  isCurrentlyActive: boolean;
  canBeRenewed: boolean;
  canBeModified: boolean;
  canBeCancelled: boolean;
  daysUntilExpiry?: number;
  isExpiringSoon: boolean;
  hasOverdueDues: boolean;
  outstandingBalance?: number;
  totalDuesGenerated?: number;
  hasRenewal?: boolean;
  canRenew?: boolean;
  canModify?: boolean;
}

/**
 * Summary contract response for list views
 */
export interface ContractSummaryResponse {
  id: number;
  flatId: number;
  flatNumber: string;
  buildingName: string;
  tenantName: string;
  startDate: string;
  endDate: string;
  monthlyRent: number;
  status: ContractStatus;
  contractLengthInMonths?: number;
  statusBadgeColor?: string;
  daysUntilExpiry?: number;
  isExpiringSoon: boolean;
  hasOverdueDues: boolean;
  outstandingBalance?: number;
  isCurrentlyActive: boolean;
}

/**
 * Manager info for notifications
 */
export interface ManagerInfo {
  managerId: number;
  managerName: string;
  managerEmail: string;
}

/**
 * Contract expiry notification model
 */
export interface ContractExpiryNotification {
  contractId: number;
  flatId: number;
  flatNumber: string;
  buildingId: number;
  buildingName: string;
  tenantName: string;
  tenantContact?: string;
  tenantEmail?: string;
  monthlyRent: number;
  outstandingBalance?: number;
  endDate: string;
  daysUntilExpiry: number;
  urgencyLevel: string | ContractUrgencyLevel; // Backend sends string
  notificationDate: string;
  managerId?: number;
  managerName?: string;
  managerEmail?: string;
  assignedManagers?: ManagerInfo[];
  recommendedAction: string;
  renewalRecommended: boolean;
}

/**
 * Contract statistics model
 */
export interface ContractStatistics {
  totalContracts: number;
  activeContracts: number;
  expiredContracts: number;
  cancelledContracts: number;
  expiringThisMonth: number;
  totalMonthlyRent: number;
  averageContractLength: number;
  occupancyRate: number;
  contractsWithOverdueDues: number;
}

/**
 * Due preview model for showing monthly dues that will be generated
 */
export interface DuePreview {
  month: string;           // e.g., "January 2024"
  dueDate: string;         // ISO date format
  amount: number;
  description: string;
  adjustedForMonthEnd: boolean;  // True if date was adjusted (e.g., 31st -> 28th in Feb)
}

/**
 * Contract search/filter parameters
 */
export interface ContractSearchParams extends PaginationParams {
  buildingId?: number;
  status?: ContractStatus;
  tenantName?: string;
  expiringWithinDays?: number;
  hasOverdueDues?: boolean;
  flatId?: number;
}

/**
 * Type guard to check if a contract is active
 */
export function isContractActive(contract: ContractResponse | ContractSummaryResponse): boolean {
  return contract.status === ContractStatus.ACTIVE;
}

/**
 * Type guard to check if a contract is currently active (using backend field)
 */
export function isCurrentlyActive(contract: ContractResponse | ContractSummaryResponse): boolean {
  // ContractSummaryResponse always has isCurrentlyActive
  if ('isCurrentlyActive' in contract && contract.isCurrentlyActive !== undefined) {
    return contract.isCurrentlyActive;
  }
  // Fall back to status check
  return contract.status === ContractStatus.ACTIVE;
}

/**
 * Type guard to check if a contract can be renewed
 */
export function canRenewContract(contract: ContractResponse): boolean {
  return contract.canBeRenewed ?? false;
}

/**
 * Type guard to check if a contract can be modified
 */
export function canModifyContract(contract: ContractResponse): boolean {
  return contract.canBeModified ?? false;
}

/**
 * Type guard to check if a contract can be cancelled
 */
export function canCancelContract(contract: ContractResponse): boolean {
  return contract.canBeCancelled ?? false;
}

/**
 * Get status color for UI display
 */
export function getContractStatusColor(status: ContractStatus): string {
  const statusColors: Record<ContractStatus, string> = {
    [ContractStatus.PENDING]: 'info',
    [ContractStatus.ACTIVE]: 'success',
    [ContractStatus.EXPIRED]: 'secondary',
    [ContractStatus.CANCELLED]: 'danger',
    [ContractStatus.RENEWED]: 'primary',
    [ContractStatus.SUPERSEDED]: 'warning'
  };
  return statusColors[status] || 'secondary';
}

/**
 * Get urgency level color for UI display
 */
export function getUrgencyLevelColor(level: ContractUrgencyLevel): string {
  const urgencyColors: Record<ContractUrgencyLevel, string> = {
    [ContractUrgencyLevel.LOW]: 'success',
    [ContractUrgencyLevel.MEDIUM]: 'warning',
    [ContractUrgencyLevel.HIGH]: 'danger',
    [ContractUrgencyLevel.CRITICAL]: 'danger'
  };
  return urgencyColors[level] || 'secondary';
}

/**
 * Format contract period for display
 */
export function formatContractPeriod(startDate: string, endDate: string): string {
  const start = new Date(startDate);
  const end = new Date(endDate);
  const options: Intl.DateTimeFormatOptions = { 
    year: 'numeric', 
    month: 'short', 
    day: 'numeric' 
  };
  return `${start.toLocaleDateString('en-US', options)} - ${end.toLocaleDateString('en-US', options)}`;
}

/**
 * Calculate contract length in months
 */
export function calculateContractMonths(startDate: string, endDate: string): number {
  const start = new Date(startDate);
  const end = new Date(endDate);
  const months = (end.getFullYear() - start.getFullYear()) * 12 + 
                 (end.getMonth() - start.getMonth());
  return Math.max(1, months);
}

/**
 * Get display name for cancellation reason category
 */
export function getCancellationReasonDisplay(category: CancellationReasonCategory): string {
  const displayNames: Record<CancellationReasonCategory, string> = {
    [CancellationReasonCategory.TENANT_REQUEST]: 'Tenant Request',
    [CancellationReasonCategory.NON_PAYMENT]: 'Non-Payment',
    [CancellationReasonCategory.BREACH_OF_CONTRACT]: 'Breach of Contract',
    [CancellationReasonCategory.PROPERTY_DAMAGE]: 'Property Damage',
    [CancellationReasonCategory.RENOVATION]: 'Renovation Required',
    [CancellationReasonCategory.MUTUAL_AGREEMENT]: 'Mutual Agreement',
    [CancellationReasonCategory.BUILDING_CLOSURE]: 'Building Closure',
    [CancellationReasonCategory.OTHER]: 'Other'
  };
  return displayNames[category] || category;
}

/**
 * Get display name for modification reason
 */
export function getModificationReasonDisplay(reason: ModificationReason): string {
  const displayNames: Record<ModificationReason, string> = {
    [ModificationReason.ANNUAL_INCREASE]: 'Annual Rent Increase',
    [ModificationReason.MARKET_ADJUSTMENT]: 'Market Rate Adjustment',
    [ModificationReason.NEGOTIATED_CHANGE]: 'Negotiated Change',
    [ModificationReason.SERVICE_ADDITION]: 'Additional Services',
    [ModificationReason.SERVICE_REMOVAL]: 'Removed Services',
    [ModificationReason.ERROR_CORRECTION]: 'Error Correction',
    [ModificationReason.OTHER]: 'Other'
  };
  return displayNames[reason] || reason;
}