package com.example.apartmentmanagerapi.controller;

import com.example.apartmentmanagerapi.config.JwtAuthEntryPoint;
import com.example.apartmentmanagerapi.config.JwtAuthTokenFilter;
import com.example.apartmentmanagerapi.dto.*;
import com.example.apartmentmanagerapi.entity.Contract;
import com.example.apartmentmanagerapi.exception.*;
import com.example.apartmentmanagerapi.service.IContractService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive tests for Contract Controller with edge cases
 */
@WebMvcTest(ContractController.class)
@WithMockUser(username = "testuser", roles = {"MANAGER"})
class ContractControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IContractService contractService;

    @MockBean
    private JwtAuthTokenFilter jwtAuthTokenFilter;

    @MockBean
    private JwtAuthEntryPoint jwtAuthEntryPoint;

    private ObjectMapper objectMapper;
    private ContractRequest validContractRequest;
    private ContractResponse contractResponse;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        validContractRequest = ContractRequest.builder()
            .flatId(1L)
            .startDate(LocalDate.now())
            .endDate(LocalDate.now().plusYears(1))
            .monthlyRent(new BigDecimal("10000"))
            .dayOfMonth(5)
            .tenantName("John Doe")
            .tenantContact("+1234567890")
            .tenantEmail("john@example.com")
            .securityDeposit(new BigDecimal("20000"))
            // autoRenew field removed - not in ContractRequest DTO
            .generateDuesImmediately(true)
            .notes("Standard 1-year lease")
            .build();

        contractResponse = ContractResponse.builder()
            .id(1L)
            .flatId(1L)
            .flatNumber("A101")
            .buildingName("Test Building")
            .startDate(LocalDate.now())
            .endDate(LocalDate.now().plusYears(1))
            .monthlyRent(new BigDecimal("10000"))
            .dayOfMonth(5)
            .status(Contract.ContractStatus.ACTIVE)
            .tenantName("John Doe")
            .totalDuesGenerated(12)
            // totalContractValue removed - not in DTO
            .outstandingBalance(BigDecimal.ZERO)
            .daysUntilExpiry(365)
            .build();
    }

    @Nested
    @DisplayName("Contract Creation Tests")
    class ContractCreationTests {

        @Test
        @DisplayName("Should create contract successfully")
        void shouldCreateContractSuccessfully() throws Exception {
            // Arrange
            when(contractService.createContract(any(ContractRequest.class)))
                .thenReturn(contractResponse);

            // Act & Assert
            mockMvc.perform(post("/api/contracts")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validContractRequest)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.flatNumber").value("A101"))
                .andExpect(jsonPath("$.monthlyRent").value(10000))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.totalDuesGenerated").value(12));

            verify(contractService).createContract(any(ContractRequest.class));
        }

        @Test
        @DisplayName("Should validate required fields")
        void shouldValidateRequiredFields() throws Exception {
            // Arrange
            ContractRequest invalidRequest = ContractRequest.builder().build();

            // Act & Assert
            mockMvc.perform(post("/api/contracts")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").exists())
                .andExpect(jsonPath("$.errors", hasSize(greaterThan(0))));
        }

        @ParameterizedTest
        @ValueSource(ints = {0, -1, 32, 100})
        @DisplayName("Should validate day of month")
        void shouldValidateDayOfMonth(int dayOfMonth) throws Exception {
            // Arrange
            validContractRequest.setDayOfMonth(dayOfMonth);

            // Act & Assert
            mockMvc.perform(post("/api/contracts")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validContractRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field == 'dayOfMonth')]").exists());
        }

        @Test
        @DisplayName("Should handle overlapping contracts")
        void shouldHandleOverlappingContracts() throws Exception {
            // Arrange
            when(contractService.createContract(any(ContractRequest.class)))
                .thenThrow(new ContractOverlapException("Overlapping contracts exist"));

            // Act & Assert
            mockMvc.perform(post("/api/contracts")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validContractRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Overlapping contracts exist"));
        }

        @Test
        @DisplayName("Should validate date range")
        void shouldValidateDateRange() throws Exception {
            // Arrange
            validContractRequest.setStartDate(LocalDate.now());
            validContractRequest.setEndDate(LocalDate.now().minusDays(1));

            // Act & Assert
            mockMvc.perform(post("/api/contracts")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validContractRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field == 'endDate')]").exists());
        }

        @Test
        @DisplayName("Should validate monthly rent")
        void shouldValidateMonthlyRent() throws Exception {
            // Arrange
            validContractRequest.setMonthlyRent(new BigDecimal("-1000"));

            // Act & Assert
            mockMvc.perform(post("/api/contracts")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validContractRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field == 'monthlyRent')]").exists());
        }

        @Test
        @DisplayName("Should validate email format")
        void shouldValidateEmailFormat() throws Exception {
            // Arrange
            validContractRequest.setTenantEmail("invalid-email");

            // Act & Assert
            mockMvc.perform(post("/api/contracts")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validContractRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field == 'tenantEmail')]").exists());
        }
    }

    @Nested
    @DisplayName("Contract Renewal Tests")
    class ContractRenewalTests {

        @Test
        @DisplayName("Should renew contract successfully")
        void shouldRenewContractSuccessfully() throws Exception {
            // Arrange
            ContractRenewalRequest renewalRequest = ContractRenewalRequest.builder()
                .newEndDate(LocalDate.now().plusYears(2))
                .newMonthlyRent(new BigDecimal("11000"))
                .generateDuesImmediately(true)
                .build();

            when(contractService.renewContract(eq(1L), any(ContractRenewalRequest.class)))
                .thenReturn(contractResponse);

            // Act & Assert
            mockMvc.perform(post("/api/contracts/1/renew")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(renewalRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists());
        }

        @Test
        @DisplayName("Should handle non-existent contract renewal")
        void shouldHandleNonExistentContractRenewal() throws Exception {
            // Arrange
            ContractRenewalRequest renewalRequest = ContractRenewalRequest.builder()
                .newEndDate(LocalDate.now().plusYears(2))
                .build();

            when(contractService.renewContract(eq(999L), any(ContractRenewalRequest.class)))
                .thenThrow(new ContractNotFoundException(999L));

            // Act & Assert
            mockMvc.perform(post("/api/contracts/999/renew")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(renewalRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
        }
    }

    @Nested
    @DisplayName("Contract Cancellation Tests")
    class ContractCancellationTests {

        @Test
        @DisplayName("Should cancel contract successfully")
        void shouldCancelContractSuccessfully() throws Exception {
            // Arrange
            ContractCancellationRequest cancellationRequest = ContractCancellationRequest.builder()
                .cancellationReason("Tenant request")
                .effectiveDate(LocalDate.now())
                .cancelUnpaidDues(true)
                .build();

            when(contractService.cancelContract(eq(1L), any(ContractCancellationRequest.class)))
                .thenReturn(contractResponse);

            // Act & Assert
            mockMvc.perform(post("/api/contracts/1/cancel")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(cancellationRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists());
        }

        @Test
        @DisplayName("Should validate cancellation reason length")
        void shouldValidateCancellationReasonLength() throws Exception {
            // Arrange
            String longReason = "a".repeat(501); // Exceeds 500 char limit
            ContractCancellationRequest cancellationRequest = ContractCancellationRequest.builder()
                .cancellationReason(longReason)
                .effectiveDate(LocalDate.now())
                .build();

            // Act & Assert
            mockMvc.perform(post("/api/contracts/1/cancel")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(cancellationRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field == 'cancellationReason')]").exists());
        }
    }

    @Nested
    @DisplayName("Contract Query Tests")
    class ContractQueryTests {

        @Test
        @DisplayName("Should get contract by ID")
        void shouldGetContractById() throws Exception {
            // Arrange
            when(contractService.getContractById(1L)).thenReturn(contractResponse);

            // Act & Assert
            mockMvc.perform(get("/api/contracts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.flatNumber").value("A101"));
        }

        @Test
        @DisplayName("Should handle contract not found")
        void shouldHandleContractNotFound() throws Exception {
            // Arrange
            when(contractService.getContractById(999L))
                .thenThrow(new ContractNotFoundException(999L));

            // Act & Assert
            mockMvc.perform(get("/api/contracts/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
        }

        @Test
        @DisplayName("Should get contracts by building with pagination")
        void shouldGetContractsByBuildingWithPagination() throws Exception {
            // Arrange
            ContractSummaryResponse summaryResponse = ContractSummaryResponse.builder()
                .id(1L)
                .flatId(1L)
                .flatNumber("A101")
                .buildingName("Test Building")
                .tenantName("John Doe")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusYears(1))
                .monthlyRent(new BigDecimal("10000"))
                .status(Contract.ContractStatus.ACTIVE)
                .build();
            
            List<ContractSummaryResponse> contracts = Arrays.asList(summaryResponse);
            Page<ContractSummaryResponse> contractPage = new PageImpl<>(
                contracts, PageRequest.of(0, 10), 1);

            when(contractService.getContractsByBuildingId(eq(1L), any()))
                .thenReturn(contractPage);

            // Act & Assert
            mockMvc.perform(get("/api/contracts/building/1")
                    .param("page", "0")
                    .param("size", "10")
                    .param("sort", "startDate,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
        }

        @Test
        @DisplayName("Should get active contracts for flat")
        void shouldGetActiveContractsForFlat() throws Exception {
            // Arrange
            when(contractService.getActiveContractByFlatId(1L))
                .thenReturn(contractResponse);

            // Act & Assert
            mockMvc.perform(get("/api/contracts/flat/1/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
        }

        @Test
        @DisplayName("Should handle no active contract for flat")
        void shouldHandleNoActiveContractForFlat() throws Exception {
            // Arrange
            when(contractService.getActiveContractByFlatId(1L))
                .thenThrow(ContractNotFoundException.noActiveContract(1L));

            // Act & Assert
            mockMvc.perform(get("/api/contracts/flat/1/active"))
                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should get expiring contracts")
        void shouldGetExpiringContracts() throws Exception {
            // Arrange

            List<ContractSummaryResponse> expiringContracts = Arrays.asList(
                ContractSummaryResponse.builder()
                    .id(1L)
                    .flatId(1L)
                    .flatNumber("A101")
                    .tenantName("John Doe")
                    .endDate(LocalDate.now().plusDays(25))
                    .status(Contract.ContractStatus.ACTIVE)
                    .monthlyRent(new BigDecimal("10000"))
                    .build()
            );
            
            when(contractService.getExpiringContracts(30))
                .thenReturn(expiringContracts);

            // Act & Assert
            mockMvc.perform(get("/api/contracts/expiring")
                    .param("days", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].endDate").exists());
        }
    }

    @Nested
    @DisplayName("Contract Modification Tests")
    class ContractModificationTests {

        @Test
        @DisplayName("Should modify contract successfully")
        void shouldModifyContractSuccessfully() throws Exception {
            // Arrange
            ContractModificationRequest modRequest = ContractModificationRequest.builder()
                .newMonthlyRent(new BigDecimal("12000"))
                .newDayOfMonth(10)
                .effectiveDate(LocalDate.now().plusMonths(1))
                .regenerateDues(true)
                .build();

            when(contractService.modifyContract(eq(1L), any(ContractModificationRequest.class)))
                .thenReturn(contractResponse);

            // Act & Assert
            mockMvc.perform(post("/api/contracts/1/modify")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(modRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists());
        }

        @Test
        @DisplayName("Should handle modification after dues generated")
        void shouldHandleModificationAfterDuesGenerated() throws Exception {
            // Arrange
            ContractModificationRequest modRequest = ContractModificationRequest.builder()
                .newMonthlyRent(new BigDecimal("12000"))
                .effectiveDate(LocalDate.now())
                .build();

            when(contractService.modifyContract(eq(1L), any(ContractModificationRequest.class)))
                .thenThrow(new BusinessRuleException("Cannot modify contract after dues are generated"));

            // Act & Assert
            mockMvc.perform(post("/api/contracts/1/modify")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(modRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Cannot modify contract after dues are generated"));
        }
    }

    @Nested
    @DisplayName("Authorization Tests")
    class AuthorizationTests {

        @Test
        @DisplayName("Should deny access for viewer role")
        @WithMockUser(roles = "VIEWER")
        void shouldDenyAccessForViewerRole() throws Exception {
            // Act & Assert
            mockMvc.perform(post("/api/contracts")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validContractRequest)))
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should allow access for admin role")
        @WithMockUser(roles = "ADMIN")
        void shouldAllowAccessForAdminRole() throws Exception {
            // Arrange
            when(contractService.createContract(any(ContractRequest.class)))
                .thenReturn(contractResponse);

            // Act & Assert
            mockMvc.perform(post("/api/contracts")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validContractRequest)))
                .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("Should require authentication")
        void shouldRequireAuthentication() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/api/contracts/1"))
                .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle very large contract values")
        void shouldHandleVeryLargeContractValues() throws Exception {
            // Arrange
            validContractRequest.setMonthlyRent(new BigDecimal("999999999.99"));
            when(contractService.createContract(any(ContractRequest.class)))
                .thenReturn(contractResponse);

            // Act & Assert
            mockMvc.perform(post("/api/contracts")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validContractRequest)))
                .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("Should handle special characters in tenant name")
        void shouldHandleSpecialCharactersInTenantName() throws Exception {
            // Arrange
            validContractRequest.setTenantName("José María O'Brien-Smith");
            when(contractService.createContract(any(ContractRequest.class)))
                .thenReturn(contractResponse);

            // Act & Assert
            mockMvc.perform(post("/api/contracts")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validContractRequest)))
                .andExpect(status().isCreated());
        }

        @ParameterizedTest
        @MethodSource("provideMalformedDateInputs")
        @DisplayName("Should handle malformed date inputs")
        void shouldHandleMalformedDateInputs(String startDate, String endDate) throws Exception {
            // Arrange
            String requestJson = String.format(
                "{\"flatId\":1,\"startDate\":\"%s\",\"endDate\":\"%s\"," +
                "\"monthlyRent\":10000,\"dayOfMonth\":5,\"tenantName\":\"John\"}",
                startDate, endDate
            );

            // Act & Assert
            mockMvc.perform(post("/api/contracts")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson))
                .andExpect(status().isBadRequest());
        }

        private static Stream<Arguments> provideMalformedDateInputs() {
            return Stream.of(
                Arguments.of("2024-13-01", "2025-01-01"), // Invalid month
                Arguments.of("2024-02-30", "2025-01-01"), // Invalid day
                Arguments.of("invalid", "2025-01-01"),    // Not a date
                Arguments.of("", "2025-01-01"),           // Empty
                Arguments.of(null, "2025-01-01")          // Null
            );
        }

        @Test
        @DisplayName("Should handle concurrent requests")
        void shouldHandleConcurrentRequests() throws Exception {
            // This test simulates what happens when the same contract
            // is created twice due to double-click or race condition

            // First request succeeds
            when(contractService.createContract(any(ContractRequest.class)))
                .thenReturn(contractResponse);

            mockMvc.perform(post("/api/contracts")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validContractRequest)))
                .andExpect(status().isCreated());

            // Second request fails due to overlap
            when(contractService.createContract(any(ContractRequest.class)))
                .thenThrow(new ContractOverlapException("Contract already exists"));

            mockMvc.perform(post("/api/contracts")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validContractRequest)))
                .andExpect(status().isConflict());
        }
    }
}