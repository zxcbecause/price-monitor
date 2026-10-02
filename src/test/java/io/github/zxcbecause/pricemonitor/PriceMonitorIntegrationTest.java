package io.github.zxcbecause.pricemonitor;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.zxcbecause.pricemonitor.domain.Marketplace;
import io.github.zxcbecause.pricemonitor.marketplace.MarketplaceClient;
import io.github.zxcbecause.pricemonitor.marketplace.MarketplaceClientRegistry;
import io.github.zxcbecause.pricemonitor.marketplace.ProductPrice;
import io.github.zxcbecause.pricemonitor.notification.Notifier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;

/**
 * Full flow on real PostgreSQL and Kafka:
 * REST -> price check -> snapshot in DB -> event in Kafka -> listener -> notification.
 * Only the marketplace API and Telegram are replaced with mocks.
 */
@SpringBootTest(properties = "monitor.scheduling.enabled=false")
@AutoConfigureMockMvc
@Testcontainers
class PriceMonitorIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    static KafkaContainer kafka = new KafkaContainer("apache/kafka:3.8.0");

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @MockitoBean
    MarketplaceClientRegistry clients;

    @MockitoBean
    Notifier notifier;

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    @Test
    void priceDropBelowTargetSendsAlert() throws Exception {
        MarketplaceClient wb = mock(MarketplaceClient.class);
        when(clients.get(Marketplace.WILDBERRIES)).thenReturn(wb);
        when(wb.fetchPrice("123456"))
                .thenReturn(new ProductPrice("Кроссовки", 100_000))
                .thenReturn(new ProductPrice("Кроссовки", 85_000));

        String created = mvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"marketplace": "WILDBERRIES", "sku": "123456", "targetPrice": 900, "chatId": 42}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = json.readTree(created).path("id").asLong();

        // first check only remembers the price
        mvc.perform(post("/api/products/{id}/check", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPrice", is(1000.0)))
                .andExpect(jsonPath("$.title", is("Кроссовки")));

        // second check sees the drop and publishes an event to Kafka
        mvc.perform(post("/api/products/{id}/check", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPrice", is(850.0)));

        verify(notifier, timeout(30_000)).send(eq(42L), contains("Цель 900 ₽ достигнута"));

        mvc.perform(get("/api/products/{id}/history", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        mvc.perform(get("/api/products/{id}/stats", id))
                .andExpect(jsonPath("$.min", is(850.0)))
                .andExpect(jsonPath("$.max", is(1000.0)));
    }

    @Test
    void duplicateProductIsRejected() throws Exception {
        String body = """
                {"marketplace": "OZON", "sku": "999"}
                """;
        mvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void invalidSkuIsRejected() throws Exception {
        JsonNode problem = json.readTree(mvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"marketplace": "WILDBERRIES", "sku": "abc"}
                                """))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString());
        org.assertj.core.api.Assertions.assertThat(problem.path("status").asInt()).isEqualTo(400);
    }
}
