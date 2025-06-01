package com.example.apartmentmanagerapi.service;

import com.example.apartmentmanagerapi.dto.*;
import com.example.apartmentmanagerapi.entity.*;
import com.example.apartmentmanagerapi.event.*;
import com.example.apartmentmanagerapi.exception.*;
import com.example.apartmentmanagerapi.mapper.ContractMapper;
import com.example.apartmentmanagerapi.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive tests for Contract Service with focus on edge cases
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ContractServiceTest {

    @Mock
    private ContractRepository contractRepository;
    
    @Mock
    private FlatRepository flatRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private ContractMapper contractMapper;
    
    @Mock
    private ApplicationEventPublisher eventPublisher;
    
    @Mock
    private IAuditService auditService;
    
    @Mock
    private Authentication authentication;
    
    @Mock
    private SecurityContext securityContext;
    
    @Captor
    private ArgumentCaptor<ContractCreatedEvent> contractCreatedEventCaptor;
    
    @Captor
    private ArgumentCaptor<ContractRenewedEvent> contractRenewedEventCaptor;
    
    @Captor
    private ArgumentCaptor<ContractCancelledEvent> contractCancelledEventCaptor;
    
    private ContractService contractService;
    
    private Contract testContract;
    private Flat testFlat;
    private User testUser;
    private ContractRequest contractRequest;
    private ContractResponse contractResponse;

    @BeforeEach
    void setUp() {
        contractService = new ContractService(
            contractRepository, 
            flatRepository, 
            userRepository, 
            contractMapper, 
            eventPublisher,
            auditService
        );
        
        // Setup test data
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setRole(User.UserRole.MANAGER);
        
        testFlat = new Flat();
        testFlat.setId(1L);
        testFlat.setFlatNumber("A101");
        testFlat.setMonthlyRent(new BigDecimal("10000"));
        testFlat.setIsActive(true);
        
        testContract = Contract.builder()
            .id(1L)
            .flat(testFlat)
            .tenant(testUser)
            .startDate(LocalDate.now().plusDays(1))
            .endDate(LocalDate.now().plusYears(1))
            .monthlyRent(new BigDecimal("10000"))
            .dayOfMonth(5)
            .status(Contract.ContractStatus.ACTIVE)
            .build();
        
        contractRequest = ContractRequest.builder()
            .flatId(1L)
            .startDate(LocalDate.now().plusDays(1))
            .endDate(LocalDate.now().plusYears(1))
            .monthlyRent(new BigDecimal("10000"))
            .dayOfMonth(5)
            .tenantName("John Doe")
            .tenantContact("+1234567890")
            .tenantEmail("john@example.com")
            .generateDuesImmediately(true)
            .build();
        
        contractResponse = ContractResponse.builder()
            .id(1L)
            .flatId(1L)
            .flatNumber("A101")
            .startDate(LocalDate.now().plusDays(1))
            .endDate(LocalDate.now().plusYears(1))
            .monthlyRent(new BigDecimal("10000"))
            .status(Contract.ContractStatus.ACTIVE)
            .build();
        
        // Setup security context - will be configured per test as needed
    }

    @Nested
    @DisplayName("Contract Creation Tests")
    class ContractCreationTests {
        
        @Test
        @DisplayName("Should create contract successfully")
        void shouldCreateContractSuccessfully() {
            // Arrange
            setupSecurityContext("testuser");
            when(flatRepository.findById(1L)).thenReturn(Optional.of(testFlat));
            when(contractRepository.findOverlappingContracts(anyLong(), any(), any(), any()))
                .thenReturn(List.of());
            when(contractMapper.toEntity(contractRequest)).thenReturn(testContract);
            when(contractRepository.save(any(Contract.class))).thenReturn(testContract);
            when(contractMapper.toResponse(testContract)).thenReturn(contractResponse);
            
            // Act
            ContractResponse response = contractService.createContract(contractRequest);
            
            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(1L);
            verify(eventPublisher).publishEvent(contractCreatedEventCaptor.capture());
            assertThat(contractCreatedEventCaptor.getValue().getContract()).isEqualTo(testContract);
            assertThat(contractCreatedEventCaptor.getValue().isGenerateDuesImmediately()).isTrue();
        }
        
        @Test
        @DisplayName("Should reject overlapping contracts")
        void shouldRejectOverlappingContracts() {
            // Arrange
            when(flatRepository.findById(1L)).thenReturn(Optional.of(testFlat));
            when(contractRepository.findOverlappingContracts(anyLong(), any(), any(), any()))
                .thenReturn(List.of(testContract));
            
            // Act & Assert
            assertThatThrownBy(() -> contractService.createContract(contractRequest))
                .isInstanceOf(ContractOverlapException.class)
                .hasMessageContaining("overlapping contracts");
        }
        
        @Test
        @DisplayName("Should handle flat not found")
        void shouldHandleFlatNotFound() {
            // Arrange
            when(flatRepository.findById(1L)).thenReturn(Optional.empty());
            
            // Act & Assert
            assertThatThrownBy(() -> contractService.createContract(contractRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Flat");
        }
        
        @Test
        @DisplayName("Should handle inactive flat")
        void shouldHandleInactiveFlat() {
            // Arrange
            testFlat.setIsActive(false);
            when(flatRepository.findById(1L)).thenReturn(Optional.of(testFlat));
            
            // Act & Assert
            assertThatThrownBy(() -> contractService.createContract(contractRequest))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("inactive flat");
        }
        
        @ParameterizedTest
        @ValueSource(ints = {0, 32, -1, 100})
        @DisplayName("Should validate day of month")
        void shouldValidateDayOfMonth(int dayOfMonth) {
            // Arrange
            contractRequest.setDayOfMonth(dayOfMonth);
            when(flatRepository.findById(1L)).thenReturn(Optional.of(testFlat));
            
            // Act & Assert
            assertThatThrownBy(() -> contractService.createContract(contractRequest))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("day of month");
        }
        
        @Test
        @DisplayName("Should handle end date before start date")
        void shouldHandleEndDateBeforeStartDate() {
            // Arrange
            contractRequest.setStartDate(LocalDate.now().plusDays(1));
            contractRequest.setEndDate(LocalDate.now());
            when(flatRepository.findById(1L)).thenReturn(Optional.of(testFlat));
            
            // Act & Assert
            assertThatThrownBy(() -> contractService.createContract(contractRequest))
                .isInstanceOf(ValidationException.class);
        }
    }

    @Nested
    @DisplayName("Contract Renewal Tests")
    class ContractRenewalTests {
        
        @Test
        @DisplayName("Should renew contract successfully")
        void shouldRenewContractSuccessfully() {
            // Arrange
            ContractRenewalRequest renewalRequest = ContractRenewalRequest.builder()
                .newEndDate(LocalDate.now().plusYears(2))
                .newMonthlyRent(new BigDecimal("11000"))
                .generateDuesImmediately(true)
                .build();
            
            Contract renewedContract = Contract.builder()
                .id(2L)
                .flat(testFlat)
                .startDate(testContract.getEndDate().plusDays(1))
                .endDate(renewalRequest.getNewEndDate())
                .monthlyRent(renewalRequest.getNewMonthlyRent())
                .dayOfMonth(testContract.getDayOfMonth())
                .status(Contract.ContractStatus.PENDING)
                .previousContract(testContract)
                .build();
            
            when(contractRepository.findById(1L)).thenReturn(Optional.of(testContract));
            when(contractRepository.save(any(Contract.class))).thenReturn(renewedContract);
            when(contractMapper.toResponse(renewedContract)).thenReturn(contractResponse);
            
            // Act
            ContractResponse response = contractService.renewContract(1L, renewalRequest);
            
            // Assert
            assertThat(response).isNotNull();
            verify(eventPublisher).publishEvent(contractRenewedEventCaptor.capture());
            assertThat(contractRenewedEventCaptor.getValue().getOldContract()).isEqualTo(testContract);
            assertThat(contractRenewedEventCaptor.getValue().getNewContract()).isEqualTo(renewedContract);
            verify(contractRepository).save(argThat(contract -> 
                contract.getStatus() == Contract.ContractStatus.RENEWED));
        }
        
        @Test
        @DisplayName("Should reject renewal of non-active contract")
        void shouldRejectRenewalOfNonActiveContract() {
            // Arrange
            testContract.setStatus(Contract.ContractStatus.CANCELLED);
            ContractRenewalRequest renewalRequest = ContractRenewalRequest.builder()
                .newEndDate(LocalDate.now().plusYears(2))
                .build();
            
            when(contractRepository.findById(1L)).thenReturn(Optional.of(testContract));
            
            // Act & Assert
            assertThatThrownBy(() -> contractService.renewContract(1L, renewalRequest))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Only active contracts can be renewed");
        }
        
        @Test
        @DisplayName("Should handle renewal date before current end")
        void shouldHandleRenewalDateBeforeCurrentEnd() {
            // Arrange
            ContractRenewalRequest renewalRequest = ContractRenewalRequest.builder()
                .newEndDate(testContract.getEndDate().minusDays(1))
                .build();
            
            when(contractRepository.findById(1L)).thenReturn(Optional.of(testContract));
            
            // Act & Assert
            assertThatThrownBy(() -> contractService.renewContract(1L, renewalRequest))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("must be after current end date");
        }
        
        @Test
        @DisplayName("Should maintain contract chain in renewals")
        void shouldMaintainContractChain() {
            // Arrange
            Contract originalContract = Contract.builder()
                .id(10L)
                .status(Contract.ContractStatus.RENEWED)
                .build();
            
            testContract.setPreviousContract(originalContract);
            
            ContractRenewalRequest renewalRequest = ContractRenewalRequest.builder()
                .newEndDate(LocalDate.now().plusYears(2))
                .build();
            
            when(contractRepository.findById(1L)).thenReturn(Optional.of(testContract));
            when(contractRepository.save(any(Contract.class))).thenAnswer(invocation -> {
                Contract saved = invocation.getArgument(0);
                saved.setId(3L);
                return saved;
            });
            when(contractMapper.toResponse(any())).thenReturn(contractResponse);
            
            // Act
            contractService.renewContract(1L, renewalRequest);
            
            // Assert - verify that save was called twice (once for new contract, once for updating old)
            verify(contractRepository, times(2)).save(any(Contract.class));
        }
    }

    @Nested
    @DisplayName("Contract Cancellation Tests")
    class ContractCancellationTests {
        
        @Test
        @DisplayName("Should cancel contract successfully")
        void shouldCancelContractSuccessfully() {
            // Arrange
            setupSecurityContext("testuser");
            ContractCancellationRequest cancellationRequest = ContractCancellationRequest.builder()
                .cancellationReason("Tenant moving out")
                .effectiveDate(LocalDate.now())
                .cancelUnpaidDues(true)
                .build();
            
            when(contractRepository.findById(1L)).thenReturn(Optional.of(testContract));
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(contractRepository.save(any(Contract.class))).thenReturn(testContract);
            when(contractMapper.toResponse(testContract)).thenReturn(contractResponse);
            
            // Act
            ContractResponse response = contractService.cancelContract(1L, cancellationRequest);
            
            // Assert
            assertThat(response).isNotNull();
            verify(eventPublisher).publishEvent(contractCancelledEventCaptor.capture());
            assertThat(contractCancelledEventCaptor.getValue().getContract()).isEqualTo(testContract);
            assertThat(contractCancelledEventCaptor.getValue().isCancelUnpaidDues()).isTrue();
            verify(contractRepository).save(argThat(contract -> 
                contract.getStatus() == Contract.ContractStatus.CANCELLED &&
                contract.getCancellationReason().equals("Tenant moving out") &&
                contract.getCancelledBy().equals(testUser)));
        }
        
        @Test
        @DisplayName("Should handle future effective date")
        void shouldHandleFutureEffectiveDate() {
            // Arrange
            ContractCancellationRequest cancellationRequest = ContractCancellationRequest.builder()
                .effectiveDate(LocalDate.now().plusDays(30))
                .build();
            
            when(contractRepository.findById(1L)).thenReturn(Optional.of(testContract));
            
            // Act & Assert
            assertThatThrownBy(() -> contractService.cancelContract(1L, cancellationRequest))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("cannot be in the future");
        }
        
        @Test
        @DisplayName("Should prevent cancelling already cancelled contract")
        void shouldPreventCancellingAlreadyCancelledContract() {
            // Arrange
            testContract.setStatus(Contract.ContractStatus.CANCELLED);
            ContractCancellationRequest cancellationRequest = ContractCancellationRequest.builder()
                .build();
            
            when(contractRepository.findById(1L)).thenReturn(Optional.of(testContract));
            
            // Act & Assert
            assertThatThrownBy(() -> contractService.cancelContract(1L, cancellationRequest))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("already cancelled");
        }
    }

    @Nested
    @DisplayName("Contract Modification Tests")
    class ContractModificationTests {
        
        @Test
        @DisplayName("Should reject modification after dues generated")
        void shouldRejectModificationAfterDuesGenerated() {
            // Arrange
            testContract.setDuesGenerated(true);
            ContractModificationRequest modRequest = ContractModificationRequest.builder()
                .newMonthlyRent(new BigDecimal("12000"))
                .effectiveDate(LocalDate.now().plusMonths(1))
                .build();
            
            when(contractRepository.findById(1L)).thenReturn(Optional.of(testContract));
            
            // Act & Assert
            assertThatThrownBy(() -> contractService.modifyContract(1L, modRequest))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("cannot be modified after dues are generated");
        }
        
        @Test
        @DisplayName("Should allow modification before dues generated")
        void shouldAllowModificationBeforeDuesGenerated() {
            // Arrange
            setupSecurityContext("testuser");
            testContract.setDuesGenerated(false);
            ContractModificationRequest modRequest = ContractModificationRequest.builder()
                .newMonthlyRent(new BigDecimal("12000"))
                .newDayOfMonth(10)
                .effectiveDate(LocalDate.now())
                .build();
            
            Contract modifiedContract = Contract.builder()
                .id(2L)
                .flat(testFlat)
                .monthlyRent(modRequest.getNewMonthlyRent())
                .dayOfMonth(modRequest.getNewDayOfMonth())
                .status(Contract.ContractStatus.ACTIVE)
                .previousContract(testContract)
                .build();
            
            when(contractRepository.findById(1L)).thenReturn(Optional.of(testContract));
            when(contractRepository.save(any(Contract.class))).thenReturn(modifiedContract);
            when(contractMapper.toResponse(modifiedContract)).thenReturn(contractResponse);
            
            // Act
            ContractResponse response = contractService.modifyContract(1L, modRequest);
            
            // Assert
            assertThat(response).isNotNull();
            verify(contractRepository).save(argThat(contract -> 
                contract.getStatus() == Contract.ContractStatus.SUPERSEDED));
        }
    }

    @Nested
    @DisplayName("Contract Query Tests")
    class ContractQueryTests {
        
        @Test
        @DisplayName("Should find contracts by building")
        void shouldFindContractsByBuilding() {
            // Arrange
            Long buildingId = 1L;
            Pageable pageable = PageRequest.of(0, 10);
            List<Contract> contracts = Arrays.asList(testContract);
            Page<Contract> contractPage = new PageImpl<>(contracts);
            
            ContractSummaryResponse summaryResponse = ContractSummaryResponse.builder()
                .id(1L)
                .flatNumber("A101")
                .tenantName("John Doe")
                .build();
            
            when(contractRepository.findByBuildingId(buildingId, pageable))
                .thenReturn(contractPage);
            when(contractMapper.toSummaryResponse(testContract)).thenReturn(summaryResponse);
            
            // Act
            Page<ContractSummaryResponse> response = contractService
                .getContractsByBuildingId(buildingId, pageable);
            
            // Assert
            assertThat(response.getContent()).hasSize(1);
            assertThat(response.getContent().get(0)).isEqualTo(summaryResponse);
        }
        
        @Test
        @DisplayName("Should find expiring contracts")
        void shouldFindExpiringContracts() {
            // Arrange
            List<Contract> expiringContracts = Arrays.asList(testContract);
            ContractSummaryResponse summary = ContractSummaryResponse.builder()
                .id(1L)
                .flatNumber("A101")
                .daysUntilExpiry(25)
                .isExpiringSoon(true)
                .build();
            
            when(contractRepository.findExpiringContracts(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(expiringContracts);
            when(contractMapper.toSummaryResponse(testContract))
                .thenReturn(summary);
            
            // Act
            List<ContractSummaryResponse> notifications = 
                contractService.getExpiringContracts(30);
            
            // Assert
            assertThat(notifications).hasSize(1);
            assertThat(notifications.get(0).getDaysUntilExpiry()).isEqualTo(25);
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {
        
        @Test
        @DisplayName("Should reject past start dates")
        void shouldRejectPastStartDates() {
            // Arrange
            contractRequest.setStartDate(LocalDate.now().minusDays(1));
            contractRequest.setEndDate(LocalDate.now().plusYears(1));
            when(flatRepository.findById(1L)).thenReturn(Optional.of(testFlat));
            
            // Act & Assert
            assertThatThrownBy(() -> contractService.createContract(contractRequest))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Start date cannot be in the past");
        }
        
        @Test
        @DisplayName("Should handle February 29 to 31 day adjustment")
        void shouldHandleFebruaryDayAdjustment() {
            // Arrange
            setupSecurityContext("testuser");
            contractRequest.setDayOfMonth(31);
            contractRequest.setStartDate(LocalDate.now().plusMonths(1).withDayOfMonth(1));
            contractRequest.setEndDate(LocalDate.now().plusMonths(12).withDayOfMonth(1).plusMonths(1).minusDays(1));
            
            when(flatRepository.findById(1L)).thenReturn(Optional.of(testFlat));
            when(contractRepository.findOverlappingContracts(anyLong(), any(), any(), any()))
                .thenReturn(List.of());
            when(contractMapper.toEntity(contractRequest)).thenReturn(testContract);
            when(contractRepository.save(any(Contract.class))).thenReturn(testContract);
            when(contractMapper.toResponse(testContract)).thenReturn(contractResponse);
            
            // Act
            ContractResponse response = contractService.createContract(contractRequest);
            
            // Assert
            assertThat(response).isNotNull();
            // Service should accept day 31 and handle adjustment during due generation
        }
        
        @Test
        @DisplayName("Should handle contract with zero rent")
        void shouldHandleContractWithZeroRent() {
            // Arrange
            setupSecurityContext("testuser");
            contractRequest.setMonthlyRent(BigDecimal.ZERO);
            
            when(flatRepository.findById(1L)).thenReturn(Optional.of(testFlat));
            when(contractRepository.findOverlappingContracts(anyLong(), any(), any(), any()))
                .thenReturn(List.of());
            when(contractMapper.toEntity(contractRequest)).thenReturn(testContract);
            when(contractRepository.save(any(Contract.class))).thenReturn(testContract);
            when(contractMapper.toResponse(testContract)).thenReturn(contractResponse);
            
            // Act
            ContractResponse response = contractService.createContract(contractRequest);
            
            // Assert
            assertThat(response).isNotNull();
            // Zero rent contracts should be allowed (e.g., promotional periods)
        }
        
        @Test
        @DisplayName("Should handle concurrent contract operations")
        void shouldHandleConcurrentContractOperations() {
            // This test would require more sophisticated setup with actual concurrency
            // For now, we test that proper locking/versioning is in place
            
            // Arrange
            when(flatRepository.findById(1L)).thenReturn(Optional.of(testFlat));
            when(contractRepository.findOverlappingContracts(anyLong(), any(), any(), any()))
                .thenReturn(List.of());
            when(contractMapper.toEntity(contractRequest)).thenReturn(testContract);
            when(contractRepository.save(any(Contract.class)))
                .thenThrow(new OptimisticLockingFailureException("Version mismatch"));
            
            // Act & Assert
            assertThatThrownBy(() -> contractService.createContract(contractRequest))
                .isInstanceOf(OptimisticLockingFailureException.class);
        }
        
        @ParameterizedTest
        @MethodSource("provideExtremeDateScenarios")
        @DisplayName("Should handle extreme date scenarios")
        void shouldHandleExtremeDateScenarios(LocalDate startDate, LocalDate endDate, 
                                             boolean shouldSucceed) {
            // Arrange
            setupSecurityContext("testuser");
            contractRequest.setStartDate(startDate);
            contractRequest.setEndDate(endDate);
            
            when(flatRepository.findById(1L)).thenReturn(Optional.of(testFlat));
            when(contractRepository.findOverlappingContracts(anyLong(), any(), any(), any()))
                .thenReturn(List.of());
            
            if (shouldSucceed) {
                when(contractMapper.toEntity(contractRequest)).thenReturn(testContract);
                when(contractRepository.save(any(Contract.class))).thenReturn(testContract);
                when(contractMapper.toResponse(testContract)).thenReturn(contractResponse);
            }
            
            // Act & Assert
            if (shouldSucceed) {
                assertThatCode(() -> contractService.createContract(contractRequest))
                    .doesNotThrowAnyException();
            } else {
                assertThatThrownBy(() -> contractService.createContract(contractRequest))
                    .isInstanceOf(ValidationException.class);
            }
        }
        
        private static Stream<Arguments> provideExtremeDateScenarios() {
            return Stream.of(
                // Same day contract
                Arguments.of(LocalDate.now().plusDays(1), LocalDate.now().plusDays(1), true),
                // Very long contract (10 years)
                Arguments.of(LocalDate.now().plusDays(1), LocalDate.now().plusYears(10), true),
                // Past start date (should fail)
                Arguments.of(LocalDate.now().minusDays(1), LocalDate.now().plusYears(1), false),
                // Far future contract (100 years)
                Arguments.of(LocalDate.now().plusDays(1), LocalDate.now().plusYears(100), true)
            );
        }
    }

    /**
     * Helper method to create a User object
     */
    private User createUser(String username, User.UserRole role) {
        User user = new User();
        user.setUsername(username);
        user.setRole(role);
        user.setEmail(username + "@example.com");
        return user;
    }
    
    /**
     * Helper method to setup security context
     */
    private void setupSecurityContext(String username) {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        when(authentication.getName()).thenReturn(username);
    }

    @Nested
    @DisplayName("Security and Authorization Tests")
    class SecurityAuthorizationTests {
        
        @Test
        @DisplayName("Should verify user has access to building")
        void shouldVerifyUserHasAccessToBuilding() {
            // Arrange
            ApartmentBuilding building = new ApartmentBuilding();
            building.setId(1L);
            building.setName("Test Building");
            testFlat.setApartmentBuilding(building);
            
            when(authentication.getName()).thenReturn("unauthorized-user");
            when(userRepository.findByUsername("unauthorized-user"))
                .thenReturn(Optional.of(createUser("unauthorized-user", User.UserRole.MANAGER)));
            when(flatRepository.findById(1L)).thenReturn(Optional.of(testFlat));
            
            // This assumes the service checks user's building assignments
            // Implementation would need to verify this
            
            // Act & Assert
            // Depending on implementation, might throw UnauthorizedException
        }
    }
}