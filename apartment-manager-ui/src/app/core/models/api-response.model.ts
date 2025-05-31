/**
 * Generic API response wrapper for consistent response structure
 */
export interface ApiResponse<T> {
  success: boolean;
  data?: T;
  message?: string;
  errors?: string[];
  timestamp?: Date;
}

/**
 * Paginated response model for list endpoints
 */
export interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number; // Current page number (0-based)
  first: boolean;
  last: boolean;
  numberOfElements: number;
  empty: boolean;
}

/**
 * Sort information for paginated responses
 */
export interface Sort {
  sorted: boolean;
  ascending: boolean;
  descending: boolean;
  empty: boolean;
}

/**
 * Pagination request parameters
 */
export interface PageRequest {
  page: number;
  size: number;
  sort?: string;
  direction?: 'ASC' | 'DESC';
}