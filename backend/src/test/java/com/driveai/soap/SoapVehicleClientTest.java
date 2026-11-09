package com.driveai.soap;

import com.driveai.config.SoapConfig;
import com.driveai.config.SoapProperties;
import com.driveai.exception.SoapBusinessException;
import com.driveai.exception.SoapTechnicalException;
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

class SoapVehicleClientTest {

    private WireMockServer server;

    @BeforeEach
    void startServer() {
        server = new WireMockServer(options().dynamicPort());
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop();
    }

    private SoapVehicleClient client() {
        return new SoapVehicleClient(
                new SoapConfig().soapRestClient(),
                new SoapProperties("http://localhost:" + server.port(), "/soap/vehicles"));
    }

    private SoapVehicleClient clientWithTimeout(Duration timeout) {
        HttpClient httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).connectTimeout(timeout).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(timeout);
        return new SoapVehicleClient(
                RestClient.builder().requestFactory(factory).build(),
                new SoapProperties("http://localhost:" + server.port(), "/soap/vehicles"));
    }

    private static String envelope(String body) {
        return "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">"
                + "<soapenv:Body>" + body + "</soapenv:Body></soapenv:Envelope>";
    }

    @Test
    void parsesSuccessfulQuote() {
        server.stubFor(post(urlEqualTo("/soap/vehicles")).willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "text/xml")
                .withBody(envelope(
                        "<GetVehicleQuoteResponse xmlns=\"http://driveai.com/soap/vehicles\">"
                                + "<available>true</available><quote>25000.50</quote>"
                                + "<currency>USD</currency><message>OK</message>"
                                + "</GetVehicleQuoteResponse>"))));

        VehicleQuote quote = client().getQuote(7L);

        assertThat(quote.carId()).isEqualTo(7L);
        assertThat(quote.available()).isTrue();
        assertThat(quote.quote()).isEqualByComparingTo("25000.50");
        assertThat(quote.currency()).isEqualTo("USD");
        assertThat(quote.fallback()).isFalse();
    }

    @Test
    void businessFaultIsMappedToConflictException() {
        server.stubFor(post(urlEqualTo("/soap/vehicles")).willReturn(aResponse()
                .withStatus(500)
                .withHeader("Content-Type", "text/xml")
                .withBody(envelope(
                        "<soapenv:Fault><faultcode>soapenv:Client</faultcode>"
                                + "<faultstring>NOT_AVAILABLE</faultstring></soapenv:Fault>"))));

        assertThatThrownBy(() -> client().getQuote(7L))
                .isInstanceOf(SoapBusinessException.class)
                .hasMessageContaining("no está disponible");
    }

    @Test
    void serverFaultIsMappedToTechnicalException() {
        server.stubFor(post(urlEqualTo("/soap/vehicles")).willReturn(aResponse()
                .withStatus(500)
                .withHeader("Content-Type", "text/xml")
                .withBody(envelope(
                        "<soapenv:Fault><faultcode>soapenv:Server</faultcode>"
                                + "<faultstring>Internal error</faultstring></soapenv:Fault>"))));

        assertThatThrownBy(() -> client().getQuote(7L))
                .isInstanceOf(SoapTechnicalException.class)
                .hasMessageContaining("error técnico");
    }

    @Test
    void nonXmlErrorBodyIsTechnicalFailure() {
        server.stubFor(post(urlEqualTo("/soap/vehicles")).willReturn(aResponse()
                .withStatus(502)
                .withBody("bad gateway")));

        assertThatThrownBy(() -> client().getQuote(7L))
                .isInstanceOf(SoapTechnicalException.class)
                .hasMessageContaining("respondió con error");
    }

    @Test
    void incompleteResponseIsTechnicalFailure() {
        server.stubFor(post(urlEqualTo("/soap/vehicles")).willReturn(aResponse()
                .withStatus(200)
                .withBody(envelope("<GetVehicleQuoteResponse><available>true</available>"
                        + "</GetVehicleQuoteResponse>"))));

        assertThatThrownBy(() -> client().getQuote(7L))
                .isInstanceOf(SoapTechnicalException.class)
                .hasMessageContaining("incompleta");
    }

    @Test
    void xxePayloadIsRejected() {
        server.stubFor(post(urlEqualTo("/soap/vehicles")).willReturn(aResponse()
                .withStatus(200)
                .withBody("<?xml version=\"1.0\"?>"
                        + "<!DOCTYPE foo [<!ENTITY xxe SYSTEM \"file:///etc/passwd\">]>"
                        + "<GetVehicleQuoteResponse><available>&xxe;</available>"
                        + "<quote>1</quote></GetVehicleQuoteResponse>")));

        assertThatThrownBy(() -> client().getQuote(7L))
                .isInstanceOf(SoapTechnicalException.class);
    }

    @Test
    void timeoutIsTechnicalFailure() {
        server.stubFor(post(urlEqualTo("/soap/vehicles")).willReturn(aResponse()
                .withStatus(200)
                .withFixedDelay(1500)
                .withBody(envelope("<GetVehicleQuoteResponse><available>true</available>"
                        + "<quote>1</quote></GetVehicleQuoteResponse>"))));

        assertThatThrownBy(() -> clientWithTimeout(Duration.ofMillis(300)).getQuote(7L))
                .isInstanceOf(SoapTechnicalException.class);
    }

    @Test
    void technicalFallbackReturnsPublishedPriceMessage() {
        VehicleQuote quote = client().fallback(7L, new SoapTechnicalException("down", null));

        assertThat(quote.fallback()).isTrue();
        assertThat(quote.available()).isTrue();
        assertThat(quote.quote()).isNull();
        assertThat(quote.message()).contains("precio publicado");
    }

    @Test
    void businessFallbackIsNotHidden() {
        assertThatThrownBy(() -> client().fallback(7L, new SoapBusinessException("no disponible")))
                .isInstanceOf(SoapBusinessException.class);
    }
}
