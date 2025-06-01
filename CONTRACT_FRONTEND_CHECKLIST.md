# Contract Frontend Implementation Checklist

## Pre-Implementation Setup

### Environment Setup
- [ ] Update Angular dependencies if needed
- [ ] Verify all existing tests pass
- [ ] Create feature branch: `feature/contract-management`
- [ ] Set up feature flags in environment files

### Module Creation
- [ ] Generate contracts module: `ng generate module dashboard/contracts --routing`
- [ ] Add lazy loading route in dashboard-routing.module.ts
- [x] Create contracts folder structure as per plan - COMPLETED
  - Created all component directories under dashboard/components/

### Contract Models and Types
- [x] Create contract.model.ts with all DTOs - COMPLETED
- [x] Align all fields with backend DTOs - COMPLETED with thorough verification
  - Fixed field names: isCurrentlyActive, canBeRenewed, canBeModified, canBeCancelled
  - Added missing fields: statusDisplayName, cancelledByUsername, totalAmountDue, etc.
  - Added ModificationReason enum (was completely missing)
  - Fixed CancellationReasonCategory (added RENOVATION, removed ILLEGAL_ACTIVITY)
  - Added ManagerInfo interface for notifications
  - Updated all request/response interfaces to match backend exactly
- [x] Add to shared/models/index.ts barrel export - COMPLETED
- [x] Create helper functions for enums - COMPLETED
  - Added getModificationReasonDisplay() function
  - Added isCurrentlyActive() type guard

## Phase 1: Core Components

### ContractService Implementation
- [ ] Create contract.service.ts with proper imports
- [ ] Implement all CRUD methods
- [ ] Add caching with 5-minute TTL for lists
- [ ] Add caching with 15-minute TTL for details
- [ ] Implement cache invalidation on mutations
- [ ] Add error handling with proper typing
- [ ] Write unit tests (minimum 80% coverage)

### ContractValidationService Implementation
- [ ] Create contract-validation.service.ts
- [ ] Implement checkOverlap method with backend call
- [ ] Implement validateDates with business rules
- [ ] Implement canCancelContract logic
- [ ] Add calculateDuePreview method
- [ ] Write comprehensive unit tests

### ContractListComponent
- [ ] Generate component with CLI
- [ ] Create responsive table template
- [ ] Implement search with debounce (300ms)
- [ ] Add status filter dropdown
- [ ] Add building filter (admin only)
- [ ] Implement sorting (date, rent)
- [ ] Add pagination with PageEvent
- [ ] Apply ResponsiveTableDirective
- [ ] Add loading state with LoadingSpinnerComponent
- [ ] Implement empty state with EmptyStateComponent
- [ ] Add quick action buttons with role checks
- [ ] Write component tests
- [ ] Test mobile responsiveness

### ContractFormComponent
- [ ] Generate component with routing
- [ ] Create multi-section form layout
- [ ] Implement flat selection with active contract check
- [ ] Add date range pickers with validation
- [ ] Add financial details section
- [ ] Add tenant information section
- [ ] Implement real-time overlap validation
- [ ] Add due generation preview
- [ ] Add form submission with loading state
- [ ] Implement error handling with FormErrorsComponent
- [ ] Add success navigation
- [ ] Write form validation tests
- [ ] Test all edge cases

### ContractDetailsComponent
- [ ] Generate component with resolver
- [ ] Create summary card section
- [ ] Add tenant information display
- [ ] Add financial summary with calculations
- [ ] Implement action buttons with role checks
- [ ] Add contract history section
- [ ] Make sections collapsible for mobile
- [ ] Add print stylesheet
- [ ] Implement loading and error states
- [ ] Write component tests

### ContractStatusBadgeComponent
- [ ] Generate as standalone component
- [ ] Implement status to color mapping
- [ ] Add expiring soon warning
- [ ] Add tooltip with status description
- [ ] Make it reusable across components
- [ ] Write unit tests

## Phase 2: Contract Operations

### ContractRenewalFormComponent
- [ ] Generate component as modal/page
- [ ] Pre-populate from current contract
- [ ] Add overdue payment check
- [ ] Implement rent adjustment with percentage
- [ ] Add new end date validation
- [ ] Show comparison with current terms
- [ ] Add submission with loading
- [ ] Handle success/error states
- [ ] Write tests for renewal logic

### ContractCancellationDialogComponent
- [ ] Generate as modal component
- [ ] Create multi-step wizard
- [ ] Add reason category dropdown
- [ ] Add detailed reason textarea
- [ ] Add unpaid dues checkbox
- [ ] Add deposit handling options
- [ ] Show consequences summary
- [ ] Implement confirmation step
- [ ] Add loading during submission
- [ ] Write cancellation flow tests

### Integration Tasks
- [ ] Add contract buttons to flat list
- [ ] Add contract section to flat details
- [ ] Update flat form to check active contracts
- [ ] Link payments to contracts
- [ ] Update monthly due list with contract info
- [ ] Test all integration points

## Phase 3: Advanced Features

### ContractTimelineComponent
- [ ] Generate timeline component
- [ ] Implement horizontal timeline layout
- [ ] Add contract cards with key info
- [ ] Make cards clickable for navigation
- [ ] Add status indicators on timeline
- [ ] Implement mobile responsive view
- [ ] Add animation for better UX
- [ ] Write visual regression tests

### ContractModificationFormComponent
- [ ] Generate modification form component
- [ ] Add eligibility check on load
- [ ] Implement effective date picker
- [ ] Add rent change with reason
- [ ] Show due regeneration preview
- [ ] Add side-by-side comparison
- [ ] Implement submission flow
- [ ] Write modification tests

### DuePreviewTableComponent
- [ ] Generate preview component
- [ ] Create month-by-month table
- [ ] Handle month-end adjustments
- [ ] Add total calculation
- [ ] Make table exportable
- [ ] Style for print/export
- [ ] Write calculation tests

### Dashboard Integration
- [ ] Add contract statistics cards
- [ ] Create expiring contracts widget
- [ ] Add monthly rent total card
- [ ] Implement overdue contracts list
- [ ] Add contract status chart
- [ ] Test dashboard performance

## Phase 4: Polish & Testing

### Performance Optimization
- [ ] Implement virtual scrolling for large lists
- [ ] Add OnPush change detection where applicable
- [ ] Optimize bundle size with lazy loading
- [ ] Profile and fix memory leaks
- [ ] Add performance monitoring

### Error Handling
- [ ] Test all error scenarios
- [ ] Add user-friendly error messages
- [ ] Implement retry mechanisms
- [ ] Add offline handling
- [ ] Test error recovery flows

### Accessibility
- [ ] Add proper ARIA labels
- [ ] Test keyboard navigation
- [ ] Verify screen reader compatibility
- [ ] Check color contrast ratios
- [ ] Fix any accessibility warnings

### Mobile Testing
- [ ] Test on iPhone (Safari)
- [ ] Test on Android (Chrome)
- [ ] Verify touch targets (44px min)
- [ ] Test landscape orientation
- [ ] Verify responsive breakpoints

### Unit Testing
- [ ] Achieve 80% code coverage minimum
- [ ] Test all service methods
- [ ] Test all component interactions
- [ ] Test all validators
- [ ] Test error scenarios

### Integration Testing
- [ ] Test flat-contract integration
- [ ] Test payment-contract flow
- [ ] Test due generation flow
- [ ] Test role-based access
- [ ] Test cache behavior

### E2E Testing
- [ ] Write contract creation E2E test
- [ ] Write renewal flow E2E test
- [ ] Write cancellation E2E test
- [ ] Write search/filter E2E test
- [ ] Run on multiple browsers

## Final Steps

### Code Quality
- [ ] Run linter and fix all issues
- [ ] Run formatter (prettier)
- [ ] Remove all console.logs
- [ ] Add proper TypeScript types
- [ ] Review and optimize imports

### Documentation
- [ ] Document all public methods
- [ ] Add component usage examples
- [ ] Update main README.md
- [ ] Create user guide
- [ ] Add inline help tooltips

### Deployment Preparation
- [ ] Test build in production mode
- [ ] Verify feature flags work
- [ ] Test rollback procedure
- [ ] Prepare monitoring dashboards
- [ ] Create deployment runbook

### Review & Approval
- [ ] Code review by senior developer
- [ ] UX review by designer
- [ ] Security review
- [ ] Performance review
- [ ] Product owner approval

## Post-Deployment

### Monitoring
- [ ] Monitor error rates
- [ ] Check performance metrics
- [ ] Monitor API response times
- [ ] Track user adoption
- [ ] Gather user feedback

### Bug Fixes
- [ ] Address critical bugs immediately
- [ ] Plan minor fixes for next sprint
- [ ] Update documentation as needed
- [ ] Communicate changes to users

## Important Backend Alignment Notes

### Critical Field Mappings
1. **Contract Response Fields**: 
   - Backend uses `isCurrentlyActive`, frontend was using `isActive` - FIXED
   - Backend uses `canBeRenewed/canBeModified/canBeCancelled`, frontend was using `canRenew/canModify/canCancel` - FIXED
   - Backend has `hasRenewal` field to check if contract was renewed

2. **Date Handling**:
   - All dates from backend come as ISO strings (LocalDate/LocalDateTime)
   - Frontend uses string type for all dates
   - Need to handle date conversions in service layer

3. **Monetary Values**:
   - Backend uses BigDecimal for all money fields
   - Frontend uses number type
   - Need to ensure proper decimal handling

4. **Enum Handling**:
   - ContractExpiryNotification.urgencyLevel comes as string from backend, not enum
   - Need to handle string-to-enum conversion if needed

5. **Missing Backend Features in Frontend**:
   - ModificationReason enum was completely missing - ADDED
   - Several fields in ContractModificationRequest were missing - ADDED
   - Security deposit fields in renewal request - ADDED

## Notes

- Mark items with ✅ when complete
- Add notes about blockers or issues
- Update estimates if needed
- Communicate progress daily

## Blocked Items Tracking

| Item | Blocker | Date | Resolution |
|------|---------|------|------------|
| | | | |

## Risk Log

| Risk | Impact | Status | Mitigation |
|------|--------|--------|------------|
| | | | |

---

Remember: Quality over speed. Test thoroughly at each step.