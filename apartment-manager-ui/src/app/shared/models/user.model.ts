/**
 * User-related interfaces and types
 */

import { BaseEntity } from './common.model';

/**
 * User role enum matching backend
 */
export enum UserRole {
  ADMIN = 'ADMIN',
  MANAGER = 'MANAGER',
  VIEWER = 'VIEWER'
}

/**
 * User entity interface
 */
export interface User extends BaseEntity {
  username: string;
  email: string;
  firstName?: string;
  lastName?: string;
  role: UserRole;
  buildingAssignments?: UserBuildingAssignment[];
}

/**
 * User-Building assignment interface
 */
export interface UserBuildingAssignment {
  id?: number;
  userId?: number;
  buildingId?: number;
  assignedDate: string;
  unassignedDate?: string;
  assignedBy?: UserSummary;
  notes?: string;
  isActive: boolean;
  createdAt?: string;
  updatedAt?: string;
}

/**
 * Login request interface
 */
export interface LoginRequest {
  username: string;
  password: string;
}

/**
 * Registration request interface
 */
export interface SignupRequest {
  username: string;
  email: string;
  password: string;
  role?: UserRole;
}

/**
 * JWT response interface
 */
export interface JwtResponse {
  accessToken: string;
  tokenType: string;
  expiresIn?: number;
  username: string;
  email: string;
  role: UserRole;
  id: number;
}

/**
 * User profile update request
 */
export interface UserUpdateRequest {
  email?: string;
  currentPassword?: string;
  newPassword?: string;
}

/**
 * User summary for lists
 */
export interface UserSummary {
  id: number;
  username: string;
  email: string;
  role: UserRole;
}