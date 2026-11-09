package com.driveai.ai;

import com.driveai.config.GroqApiProperties;
import com.driveai.exception.GroqApiException;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HttpGroqApiClientTest {

    private WireMockServer server;

    @BeforeEach
    void start() {
        server = new WireMockServer(options().dynamicPort());
        server.start();
    }

    @AfterEach
    void stop() {
        server.stop();
    }

    private HttpGroqApiClient client(Duration timeout) {
        HttpClient httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).connectTimeout(timeout).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(timeout);
        GroqApiProperties properties = new GroqApiProperties(
                "http://localhost:" + server.port(), "test-key", "test-model", 1, 1, 100, 0.2);
        return new HttpGroqApiClient(RestClient.builder(), properties, factory);
    }

    private HttpGroqApiClient client() {
        return client(Duration.ofSeconds(5));
    }

    private static String completion(String content) {
        return "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":"
                + "\"" + content + "\"}}]}";
    }

    @Test
    void returnsModelContent() {
        server.stubFor(post(urlEqualTo("/chat/completions")).willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(completion("{\\\"reasoning\\\":\\\"ok\\\"}"))));

        String result = client().chatCompletion("system", "user");

        assertThat(result).contains("reasoning");
    }

    @Test
    void maps429ToSaturationMessage() {
        server.stubFor(post(urlEqualTo("/chat/completions")).willReturn(aResponse().withStatus(429)));

        assertThatThrownBy(() -> client().chatCompletion("s", "u"))
                .isInstanceOf(GroqApiException.class)
                .hasMessageContaining("saturado");
    }

    @Test
    void maps401ToAuthenticationMessage() {
        server.stubFor(post(urlEqualTo("/chat/completions")).willReturn(aResponse().withStatus(401)));

        assertThatThrownBy(() -> client().chatCompletion("s", "u"))
                .isInstanceOf(GroqApiException.class)
                .hasMessageContaining("autenticar");
    }

    @Test
    void maps403ToAuthenticationMessage() {
        server.stubFor(post(urlEqualTo("/chat/completions")).willReturn(aResponse().withStatus(403)));

        assertThatThrownBy(() -> client().chatCompletion("s", "u"))
                .isInstanceOf(GroqApiException.class)
                .hasMessageContaining("autenticar");
    }

    @Test
    void maps5xxToUnavailableMessage() {
        server.stubFor(post(urlEqualTo("/chat/completions")).willReturn(aResponse().withStatus(503)));

        assertThatThrownBy(() -> client().chatCompletion("s", "u"))
                .isInstanceOf(GroqApiException.class)
                .hasMessageContaining("no está disponible");
    }

    @Test
    void emptyBodyIsRejected() {
        server.stubFor(post(urlEqualTo("/chat/completions")).willReturn(aResponse()
                .withStatus(200)
                .withBody("")));

        assertThatThrownBy(() -> client().chatCompletion("s", "u"))
                .isInstanceOf(GroqApiException.class)
                .hasMessageContaining("vacía");
    }

    @Test
    void malformedJsonIsRejected() {
        server.stubFor(post(urlEqualTo("/chat/completions")).willReturn(aResponse()
                .withStatus(200)
                .withBody("{not-json")));

        assertThatThrownBy(() -> client().chatCompletion("s", "u"))
                .isInstanceOf(GroqApiException.class)
                .hasMessageContaining("malformada");
    }

    @Test
    void missingContentIsRejected() {
        server.stubFor(post(urlEqualTo("/chat/completions")).willReturn(aResponse()
                .withStatus(200)
                .withBody("{\"choices\":[]}")));

        assertThatThrownBy(() -> client().chatCompletion("s", "u"))
                .isInstanceOf(GroqApiException.class)
                .hasMessageContaining("contenido");
    }

    @Test
    void timeoutIsReportedAsNoResponse() {
        server.stubFor(post(urlEqualTo("/chat/completions")).willReturn(aResponse()
                .withStatus(200)
                .withFixedDelay(1500)
                .withBody(completion("late"))));

        assertThatThrownBy(() -> client(Duration.ofMillis(300)).chatCompletion("s", "u"))
                .isInstanceOf(GroqApiException.class)
                .hasMessageContaining("no respondió a tiempo");
    }
}
