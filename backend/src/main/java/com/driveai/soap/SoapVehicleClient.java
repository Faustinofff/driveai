package com.driveai.soap;

import com.driveai.config.SoapProperties;
import com.driveai.exception.SoapBusinessException;
import com.driveai.exception.SoapTechnicalException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.math.BigDecimal;
import java.util.Locale;

/**
 * Cliente SOAP del servicio externo de cotización de vehículos.
 *
 * <p>Resiliencia:
 * <ul>
 *   <li>Timeout de conexión y lectura de 5 s (configurado en {@code SoapConfig}).</li>
 *   <li>Reintento acotado y circuit breaker (Resilience4j).</li>
 *   <li>Fallback con mensaje claro si el proveedor está caído.</li>
 * </ul>
 *
 * <p>Seguridad: el parser XML deshabilita DTD y entidades externas (XXE).
 * Los Fault de negocio se traducen a {@link SoapBusinessException} (409) y los
 * técnicos a {@link SoapTechnicalException}; nunca se expone el XML crudo.
 */
@Component
@Slf4j
public class SoapVehicleClient {

    private static final String SOAP_ACTION = "getVehicleQuote";

    private final RestClient restClient;
    private final String endpoint;

    public SoapVehicleClient(RestClient soapRestClient, SoapProperties properties) {
        this.restClient = soapRestClient;
        this.endpoint = properties.baseUrl() + properties.path();
    }

    @Retry(name = "soap")
    @CircuitBreaker(name = "soap", fallbackMethod = "fallback")
    public VehicleQuote getQuote(Long carId) {
        String request = buildRequest(carId);
        String response;
        try {
            response = restClient.post()
                    .uri(endpoint)
                    .contentType(MediaType.TEXT_XML)
                    .header("SOAPAction", SOAP_ACTION)
                    .body(request)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException ex) {
            // Un SOAP Fault suele llegar con HTTP 500 y cuerpo XML.
            String body = ex.getResponseBodyAsString();
            if (body != null && !body.isBlank()) {
                parseFaultOrThrow(body);
            }
            throw new SoapTechnicalException("El servicio de cotización respondió con error.", ex);
        } catch (ResourceAccessException ex) {
            throw new SoapTechnicalException("El servicio de cotización no respondió a tiempo.", ex);
        }

        if (response == null || response.isBlank()) {
            throw new SoapTechnicalException("El servicio de cotización devolvió una respuesta vacía.", null);
        }
        parseFaultOrThrow(response);
        return parseQuote(carId, response);
    }

    /**
     * Fallback para fallos técnicos: el flujo de compra continúa con el precio
     * publicado y se informa al usuario.
     */
    VehicleQuote fallback(Long carId, Throwable ex) {
        log.warn("Servicio SOAP no disponible para carId={}: {}", carId, ex.toString());
        return new VehicleQuote(carId, true, null, "USD",
                "La cotización en línea no está disponible en este momento. Puedes continuar con el precio publicado del catálogo.",
                true);
    }

    /** Un Fault de negocio no debe ocultarse con el fallback: se propaga como 409. */
    VehicleQuote fallback(Long carId, SoapBusinessException ex) {
        throw ex;
    }

    private String buildRequest(Long carId) {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                                  xmlns:veh="http://driveai.com/soap/vehicles">
                  <soapenv:Header/>
                  <soapenv:Body>
                    <veh:GetVehicleQuoteRequest>
                      <veh:vehicleId>%d</veh:vehicleId>
                    </veh:GetVehicleQuoteRequest>
                  </soapenv:Body>
                </soapenv:Envelope>
                """.formatted(carId);
    }

    private VehicleQuote parseQuote(Long carId, String xml) {
        Document document = parseXml(xml);
        String available = text(document, "available");
        String quote = text(document, "quote");
        String currency = text(document, "currency");
        String message = text(document, "message");
        if (available == null || quote == null) {
            throw new SoapTechnicalException("Respuesta SOAP incompleta.", null);
        }
        return new VehicleQuote(
                carId,
                Boolean.parseBoolean(available),
                new BigDecimal(quote),
                currency == null ? "USD" : currency,
                message,
                false);
    }

    private void parseFaultOrThrow(String xml) {
        Document document;
        try {
            document = parseXml(xml);
        } catch (SoapTechnicalException ex) {
            // El cuerpo no era XML: se trata como fallo técnico.
            return;
        }
        NodeList faults = document.getElementsByTagNameNS("*", "Fault");
        if (faults.getLength() == 0) {
            return;
        }
        String faultCode = safeText(document, "faultcode");
        String faultString = safeText(document, "faultstring");
        String detailCode = safeText(document, "code");
        String normalized = (faultCode + " " + detailCode + " " + faultString).toUpperCase(Locale.ROOT);
        if (normalized.contains("CLIENT")
                || normalized.contains("NOT_AVAILABLE")
                || normalized.contains("NO DISPONIBLE")) {
            throw new SoapBusinessException(
                    "El vehículo no está disponible para cotización en este momento.");
        }
        throw new SoapTechnicalException("El servicio de cotización informó un error técnico.", null);
    }

    private Document parseXml(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            factory.setNamespaceAware(true);
            return factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
        } catch (Exception ex) {
            throw new SoapTechnicalException("No se pudo interpretar la respuesta SOAP.", ex);
        }
    }

    private String text(Document document, String tag) {
        NodeList nodes = document.getElementsByTagNameNS("*", tag);
        if (nodes.getLength() == 0) {
            return null;
        }
        return nodes.item(0).getTextContent();
    }

    private String safeText(Document document, String tag) {
        String value = text(document, tag);
        return value == null ? "" : value;
    }
}
