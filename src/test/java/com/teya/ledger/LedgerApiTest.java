package com.teya.ledger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises the REST API end to end. The context is recreated after each test
 * so that every test starts with an empty in-memory ledger.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class LedgerApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldCreateDepositTransaction() throws Exception {
        postTransaction("""
                { "type": "DEPOSIT", "amount": 100.00 }
                """)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(notNullValue()))
                .andExpect(jsonPath("$.type").value("DEPOSIT"))
                .andExpect(jsonPath("$.amount").value(100.00))
                .andExpect(jsonPath("$.createdAt").value(notNullValue()));
    }

    @Test
    void shouldCreateWithdrawalTransaction() throws Exception {
        postTransaction("""
                { "type": "DEPOSIT", "amount": 100.00 }
                """);

        postTransaction("""
                { "type": "WITHDRAWAL", "amount": 40.00 }
                """)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("WITHDRAWAL"))
                .andExpect(jsonPath("$.amount").value(40.00));
    }

    @Test
    void shouldReturnTransactionHistoryOldestFirst() throws Exception {
        postTransaction("""
                { "type": "DEPOSIT", "amount": 100.00 }
                """);
        postTransaction("""
                { "type": "WITHDRAWAL", "amount": 40.00 }
                """);

        mockMvc.perform(get("/api/v1/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].type").value("DEPOSIT"))
                .andExpect(jsonPath("$[0].amount").value(100.00))
                .andExpect(jsonPath("$[1].type").value("WITHDRAWAL"))
                .andExpect(jsonPath("$[1].amount").value(40.00));
    }

    @Test
    void shouldReturnEmptyTransactionHistory() throws Exception {
        mockMvc.perform(get("/api/v1/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void shouldReturnCurrentBalance() throws Exception {
        postTransaction("""
                { "type": "DEPOSIT", "amount": 100.00 }
                """);
        postTransaction("""
                { "type": "WITHDRAWAL", "amount": 40.00 }
                """);

        mockMvc.perform(get("/api/v1/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(60.00));
    }

    @Test
    void shouldReturnZeroBalanceWhenNoTransactions() throws Exception {
        mockMvc.perform(get("/api/v1/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(0));
    }

    @Test
    void shouldRejectWithdrawalExceedingBalance() throws Exception {
        postTransaction("""
                { "type": "DEPOSIT", "amount": 50.00 }
                """);

        postTransaction("""
                { "type": "WITHDRAWAL", "amount": 100.00 }
                """)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_FUNDS"))
                .andExpect(jsonPath("$.message").value("Insufficient funds: requested 100.00, available 50.00"))
                .andExpect(jsonPath("$.timestamp").value(notNullValue()));

        mockMvc.perform(get("/api/v1/balance"))
                .andExpect(jsonPath("$.balance").value(50.00));
    }

    @Test
    void shouldRejectZeroAmount() throws Exception {
        postTransaction("""
                { "type": "DEPOSIT", "amount": 0 }
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("amount: must be greater than 0"));
    }

    @Test
    void shouldRejectNegativeAmount() throws Exception {
        postTransaction("""
                { "type": "WITHDRAWAL", "amount": -50.00 }
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldRejectMissingFields() throws Exception {
        postTransaction("{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("amount: must not be null; type: must not be null"));
    }

    @Test
    void shouldRejectUnknownTransactionType() throws Exception {
        postTransaction("""
                { "type": "TRANSFER", "amount": 10.00 }
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldRejectMalformedJson() throws Exception {
        postTransaction("{ not json")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldNotRecordInvalidTransaction() throws Exception {
        postTransaction("""
                { "type": "DEPOSIT", "amount": -1 }
                """);

        mockMvc.perform(get("/api/v1/transactions"))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    private ResultActions postTransaction(String json) throws Exception {
        return mockMvc.perform(post("/api/v1/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }
}
