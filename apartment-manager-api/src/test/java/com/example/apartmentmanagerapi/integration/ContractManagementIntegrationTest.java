package com.example.apartmentmanagerapi.integration;

import com.example.apartmentmanagerapi.dto.*;
import com.example.apartmentmanagerapi.entity.*;
import com.example.apartmentmanagerapi.repository.*;
import com.example.apartmentmanagerapi.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.data.domain.PageRequest;

/**
 * Integration tests for complete contract management flow
 * Tests the entire system including events, database, and business rules
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ContractManagementIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private FlatRepository flatRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ApartmentBuildingRepository buildingRepository;

    @Autowired
    private MonthlyDueRepository monthlyDueRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    private ApartmentBuilding testBuilding;
    private Flat testFlat;
    private User testManager;
    private User testTenant;
    private Long createdContractId;

    @BeforeAll
    void setupTestData() {
        // Clean up any existing test data
        monthlyDueRepository.deleteAll();
        contractRepository.deleteAll();
        flatRepository.deleteAll();
        userRepository.deleteAll();
        buildingRepository.deleteAll();
        auditLogRepository.deleteAll();

        // Create test building
        testBuilding = new ApartmentBuilding();
        testBuilding.setName("Integration Test Building");
        testBuilding.setAddress("123 Test Street");
        testBuilding = buildingRepository.save(testBuilding);

        // Create test users
        testManager = new User();
        testManager.setUsername("manager@test.com");
        testManager.setEmail("manager@test.com");
        testManager.setPassword("$2a$10$dummy"); // Already encoded
        testManager.setRole(User.UserRole.MANAGER);
        testManager = userRepository.save(testManager);

        testTenant = new User();
        testTenant.setUsername("tenant@test.com");
        testTenant.setEmail("tenant@test.com");
        testTenant.setPassword("$2a$10$dummy");
        testTenant.setRole(User.UserRole.VIEWER);
        testTenant = userRepository.save(testTenant);

        // Create test flat
        testFlat = new Flat();
        testFlat.setFlatNumber("A101");
        testFlat.setNumberOfRooms(3);
        testFlat.setMonthlyRent(new BigDecimal("10000"));
        testFlat.setApartmentBuilding(testBuilding);
        testFlat.setIsActive(true);
        testFlat = flatRepository.save(testFlat);
    }

    @AfterAll
    void cleanupTestData() {
        monthlyDueRepository.deleteAll();
        contractRepository.deleteAll();
        flatRepository.deleteAll();
        userRepository.deleteAll();
        buildingRepository.deleteAll();
        auditLogRepository.deleteAll();
    }

    @Nested
    @DisplayName("Complete Contract Lifecycle Tests")
    class CompleteContractLifecycleTests {

        @Test
        @Order(1)
        @DisplayName("Should create contract and generate dues automatically")
        @WithMockUser(username = "manager@test.com", roles = {"MANAGER"})
        @Transactional
        void shouldCreateContractAndGenerateDues() throws Exception {
            // Arrange
            ContractRequest request = ContractRequest.builder()
                .flatId(testFlat.getId())
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusYears(1))
                .monthlyRent(new BigDecimal("10000"))
                .dayOfMonth(15)
                .tenantName("John Doe")
                .tenantContact("+1234567890")
                .tenantEmail("john.doe@test.com")
                .securityDeposit(new BigDecimal("20000"))
                .generateDuesImmediately(true)
                .notes("Integration test contract")
                .build();

            // Act
            String response = mockMvc.perform(post("/api/contracts")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.duesGenerated").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();

            ContractResponse contractResponse = objectMapper.readValue(response, ContractResponse.class);
            createdContractId = contractResponse.getId();

            // Assert - Verify contract created
            Contract savedContract = contractRepository.findById(createdContractId).orElseThrow();
            assertThat(savedContract).isNotNull();
            assertThat(savedContract.getStatus()).isEqualTo(Contract.ContractStatus.ACTIVE);
            assertThat(savedContract.isDuesGenerated()).isTrue();

            // Assert - Verify dues generated (wait for async event)
            await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
                List<MonthlyDue> generatedDues = monthlyDueRepository.findByFlatIdOrderByDueDateDesc(testFlat.getId());
                assertThat(generatedDues).hasSize(12); // 12 months
                assertThat(generatedDues).allSatisfy(due -> {
                    assertThat(due.getDueAmount()).isEqualByComparingTo("10000");
                    assertThat(due.isPaid()).isFalse();
                    assertThat(due.getDueDate().getDayOfMonth()).isEqualTo(15);
                });
            });

            // Assert - Verify audit log created
            await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
                List<AuditLog> auditLogs = auditLogRepository.findAll();
                assertThat(auditLogs.stream()
                    .anyMatch(log -> log.getAction() == AuditLog.AuditAction.CONTRACT_CREATED))
                    .isTrue();
            });
        }

        @Test
        @Order(2)
        @DisplayName("Should prevent overlapping contracts")
        @WithMockUser(username = "manager@test.com", roles = {"MANAGER"})
        void shouldPreventOverlappingContracts() throws Exception {
            // Arrange - Try to create overlapping contract
            ContractRequest overlappingRequest = ContractRequest.builder()
                .flatId(testFlat.getId())
                .startDate(LocalDate.now().plusMonths(6))
                .endDate(LocalDate.now().plusMonths(18))
                .monthlyRent(new BigDecimal("12000"))
                .dayOfMonth(20)
                .tenantName("Jane Doe")
                .tenantContact("+0987654321")
                .build();

            // Act & Assert
            mockMvc.perform(post("/api/contracts")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(overlappingRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.error", containsString("overlapping")));
        }

        @Test
        @Order(3)
        @DisplayName("Should renew contract successfully")
        @WithMockUser(username = "manager@test.com", roles = {"MANAGER"})
        @Transactional
        void shouldRenewContractSuccessfully() throws Exception {
            // Arrange
            ContractRenewalRequest renewalRequest = ContractRenewalRequest.builder()
                .newEndDate(LocalDate.now().plusYears(2))
                .newMonthlyRent(new BigDecimal("11000"))
                .generateDuesImmediately(true)
                .renewalNotes("Renewal with 10% increase")
                .build();

            // Act
            mockMvc.perform(post("/api/contracts/" + createdContractId + "/renew")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(renewalRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.previousContractId").value(createdContractId))
                .andExpect(jsonPath("$.monthlyRent").value(11000));

            // Assert - Verify old contract marked as renewed
            Contract oldContract = contractRepository.findById(createdContractId).orElseThrow();
            assertThat(oldContract.getStatus()).isEqualTo(Contract.ContractStatus.RENEWED);

            // Assert - Verify new dues generated for extension period
            await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
                List<MonthlyDue> allDues = monthlyDueRepository.findByFlatIdOrderByDueDateDesc(testFlat.getId());
                // Should have original 12 + 12 extension = 24 total
                assertThat(allDues.size()).isGreaterThanOrEqualTo(24);
                
                // Verify new dues have increased amount
                List<MonthlyDue> newDues = allDues.stream()
                    .filter(due -> due.getDueAmount().compareTo(new BigDecimal("11000")) == 0)
                    .toList();
                assertThat(newDues).isNotEmpty();
            });
        }

        @Test
        @Order(4)
        @DisplayName("Should handle contract cancellation with unpaid dues")
        @WithMockUser(username = "manager@test.com", roles = {"MANAGER"})
        @Transactional
        void shouldHandleContractCancellation() throws Exception {
            // Arrange - Get the renewed contract
            Contract activeContract = contractRepository.findByFlatIdAndStatus(testFlat.getId(), Contract.ContractStatus.ACTIVE)
                .orElseThrow();

            ContractCancellationRequest cancellationRequest = ContractCancellationRequest.builder()
                .reasonCategory(ContractCancellationRequest.CancellationReasonCategory.TENANT_REQUEST)
                .cancellationReason("Tenant moving out early")
                .effectiveDate(LocalDate.now())
                .cancelUnpaidDues(true)
                .build();

            // Act
            mockMvc.perform(post("/api/contracts/" + activeContract.getId() + "/cancel")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(cancellationRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancellationReason").value("Tenant moving out early"));

            // Assert - Verify contract cancelled
            Contract cancelledContract = contractRepository.findById(activeContract.getId()).orElseThrow();
            assertThat(cancelledContract.getStatus()).isEqualTo(Contract.ContractStatus.CANCELLED);
            assertThat(cancelledContract.getCancellationDate()).isNotNull();

            // Assert - Verify unpaid dues cancelled
            await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
                List<MonthlyDue> unpaidDues = monthlyDueRepository.findByFlatIdOrderByDueDateDesc(testFlat.getId())
                    .stream()
                    .filter(due -> !due.isPaid() && 
                           due.getDueDate().isAfter(LocalDate.now()))
                    .toList();
                
                assertThat(unpaidDues).allSatisfy(due -> 
                    assertThat(due.getStatus()).isEqualTo(MonthlyDue.PaymentStatus.CANCELLED)
                );
            });
        }
    }

    @Nested
    @DisplayName("Edge Case Integration Tests")
    class EdgeCaseIntegrationTests {

        @Test
        @DisplayName("Should handle February 29 contract across leap years")
        @WithMockUser(username = "manager@test.com", roles = {"MANAGER"})
        @Transactional
        void shouldHandleLeapYearContract() throws Exception {
            // Arrange - Create new flat for this test
            Flat tempFlat = new Flat();
            tempFlat.setFlatNumber("B201");
            tempFlat.setNumberOfRooms(2);
            tempFlat.setMonthlyRent(new BigDecimal("8000"));
            tempFlat.setApartmentBuilding(testBuilding);
            tempFlat.setIsActive(true);
            final Flat leapYearFlat = flatRepository.save(tempFlat);

            ContractRequest request = ContractRequest.builder()
                .flatId(leapYearFlat.getId())
                .startDate(LocalDate.of(2024, 2, 29)) // Leap year
                .endDate(LocalDate.of(2025, 2, 28))   // Non-leap year
                .monthlyRent(new BigDecimal("8000"))
                .dayOfMonth(29)
                .tenantName("Leap Year Tenant")
                .generateDuesImmediately(true)
                .build();

            // Act
            String response = mockMvc.perform(post("/api/contracts")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

            ContractResponse contractResponse = objectMapper.readValue(response, ContractResponse.class);

            // Assert - Verify dues adjusted for non-leap year
            await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
                List<MonthlyDue> dues = monthlyDueRepository.findByFlatIdOrderByDueDateDesc(leapYearFlat.getId());
                
                // Find February 2025 due
                MonthlyDue feb2025Due = dues.stream()
                    .filter(due -> due.getDueDate().getYear() == 2025 && 
                                  due.getDueDate().getMonth().getValue() == 2)
                    .findFirst()
                    .orElseThrow();
                
                // Should be adjusted to Feb 28 (last day of month)
                assertThat(feb2025Due.getDueDate()).isEqualTo(LocalDate.of(2025, 2, 28));
            });
        }

        @Test
        @DisplayName("Should handle concurrent contract operations")
        @WithMockUser(username = "manager@test.com", roles = {"MANAGER"})
        void shouldHandleConcurrentOperations() throws Exception {
            // This test simulates race conditions by creating multiple threads
            // trying to create contracts for the same flat

            // Arrange - Create new flat
            Flat tempFlat = new Flat();
            tempFlat.setFlatNumber("C301");
            tempFlat.setNumberOfRooms(1);
            tempFlat.setMonthlyRent(new BigDecimal("6000"));
            tempFlat.setApartmentBuilding(testBuilding);
            tempFlat.setIsActive(true);
            final Flat concurrentFlat = flatRepository.save(tempFlat);

            ContractRequest request = ContractRequest.builder()
                .flatId(concurrentFlat.getId())
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusYears(1))
                .monthlyRent(new BigDecimal("6000"))
                .dayOfMonth(1)
                .tenantName("Concurrent Tenant")
                .build();

            // Act - Try to create same contract from two threads
            // In a real scenario, this would be from different users/sessions
            Thread thread1 = new Thread(() -> {
                try {
                    mockMvc.perform(post("/api/contracts")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)));
                } catch (Exception e) {
                    // Expected for one thread
                }
            });

            Thread thread2 = new Thread(() -> {
                try {
                    mockMvc.perform(post("/api/contracts")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)));
                } catch (Exception e) {
                    // Expected for one thread
                }
            });

            thread1.start();
            thread2.start();
            thread1.join();
            thread2.join();

            // Assert - Only one contract should be created
            List<Contract> contracts = contractRepository.findByFlatIdOrderByStartDateDesc(
                concurrentFlat.getId());
            assertThat(contracts).hasSize(1);
        }

        @Test
        @DisplayName("Should handle contract modification before dues generation")
        @WithMockUser(username = "manager@test.com", roles = {"MANAGER"})
        @Transactional
        void shouldHandleContractModificationBeforeDues() throws Exception {
            // Arrange - Create contract without immediate due generation
            Flat tempFlat = new Flat();
            tempFlat.setFlatNumber("D401");
            tempFlat.setNumberOfRooms(2);
            tempFlat.setMonthlyRent(new BigDecimal("7000"));
            tempFlat.setApartmentBuilding(testBuilding);
            tempFlat.setIsActive(true);
            final Flat modFlat = flatRepository.save(tempFlat);

            ContractRequest request = ContractRequest.builder()
                .flatId(modFlat.getId())
                .startDate(LocalDate.now().plusDays(30)) // Future start
                .endDate(LocalDate.now().plusDays(395))
                .monthlyRent(new BigDecimal("7000"))
                .dayOfMonth(10)
                .tenantName("Future Tenant")
                .generateDuesImmediately(false) // Don't generate dues yet
                .build();

            String response = mockMvc.perform(post("/api/contracts")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

            ContractResponse contractResponse = objectMapper.readValue(response, ContractResponse.class);

            // Act - Modify the contract
            ContractModificationRequest modRequest = ContractModificationRequest.builder()
                .effectiveDate(LocalDate.now())
                .newMonthlyRent(new BigDecimal("7500"))
                .reason(ContractModificationRequest.ModificationReason.NEGOTIATED_CHANGE)
                .modificationDetails("Test modification")
                .newDayOfMonth(15)
                .build();

            mockMvc.perform(post("/api/contracts/" + contractResponse.getId() + "/modify")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(modRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monthlyRent").value(7500))
                .andExpect(jsonPath("$.dayOfMonth").value(15));

            // Assert - Original contract should be superseded
            Contract originalContract = contractRepository.findById(contractResponse.getId())
                .orElseThrow();
            assertThat(originalContract.getStatus()).isEqualTo(Contract.ContractStatus.SUPERSEDED);
        }
    }

    @Nested
    @DisplayName("Query and Reporting Integration Tests")
    class QueryReportingIntegrationTests {

        @Test
        @DisplayName("Should generate comprehensive contract report")
        @WithMockUser(username = "manager@test.com", roles = {"MANAGER"})
        void shouldGenerateContractReport() throws Exception {
            // Act - Get all contracts for building
            mockMvc.perform(get("/api/contracts/building/" + testBuilding.getId())
                    .param("page", "0")
                    .param("size", "20")
                    .param("sort", "startDate,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(greaterThan(0)))
                .andExpect(jsonPath("$.content[*].flat.apartmentBuilding.id", everyItem(equalTo(testBuilding.getId().intValue()))));
        }

        @Test
        @DisplayName("Should find contracts with unpaid dues")
        @WithMockUser(username = "manager@test.com", roles = {"MANAGER"})
        void shouldFindContractsWithUnpaidDues() throws Exception {
            // Act
            mockMvc.perform(get("/api/contracts/building/" + testBuilding.getId() + "/unpaid-dues"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
        }

        @Test
        @DisplayName("Should track contract history for flat")
        @WithMockUser(username = "manager@test.com", roles = {"MANAGER"})
        void shouldTrackContractHistory() throws Exception {
            // Act
            mockMvc.perform(get("/api/contracts/flat/" + testFlat.getId() + "/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(greaterThan(1))); // Multiple contracts from tests
        }
    }
}