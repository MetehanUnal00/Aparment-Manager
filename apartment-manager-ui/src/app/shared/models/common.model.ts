/**
 * Common interfaces and types used across the application
 */

/**
 * Base entity interface with common fields
 */
export interface BaseEntity {
  id?: number;
  createdAt?: string;
  updatedAt?: string;
  createdBy?: string;
  updatedBy?: string;
}

/**
 * Pagination request parameters
 */
export interface PaginationParams {
  page?: number;
  size?: number;
  sort?: string;
  direction?: 'ASC' | 'DESC';
}

/**
 * Paginated response wrapper
 */
export interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

/**
 * API error response
 */
export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  correlationId?: string;
  fieldErrors?: { [key: string]: string };
}

/**
 * Success message response
 */
export interface MessageResponse {
  message: string;
  timestamp?: string;
}

/**
 * Date range filter
 */
export interface DateRange {
  startDate: string;
  endDate: string;
}

/**
 * Generic ID reference
 */
export interface IdReference {
  id: number;
}

/**
 * Name-value pair for dropdowns
 */
export interface NameValuePair {
  name: string;
  value: any;
}