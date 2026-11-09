package com.driveai.contract;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba de contrato del WSDL publicado por el mock SOAP.
 *
 * <p>Verifica que el contrato versionado en {@code docker/soap-mock/__files/vehicles.wsdl}
 * declare la operación, mensajes y binding que consume {@link com.driveai.soap.SoapVehicleClient}.
 * No requiere Docker.
 */
class WsdlContractTest {

    private static final Path WSDL = Path.of("..", "docker", "soap-mock", "__files", "vehicles.wsdl");

    private Document parse() throws Exception {
        assertThat(Files.exists(WSDL))
                .as("El WSDL debe estar versionado en docker/soap-mock/__files/vehicles.wsdl")
                .isTrue();

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

        try (InputStream input = Files.newInputStream(WSDL)) {
            return factory.newDocumentBuilder().parse(input);
        }
    }

    @Test
    void wsdlDeclaresVehicleQuoteContract() throws Exception {
        Document document = parse();
        Element definitions = document.getDocumentElement();

        assertThat(definitions.getLocalName()).isEqualTo("definitions");
        assertThat(definitions.getAttribute("targetNamespace"))
                .isEqualTo("http://driveai.com/soap/vehicles");

        assertThat(textOfFirst(document, "operation")).isEqualTo("getVehicleQuote");
        assertThat(textOfFirst(document, "portType")).isEqualTo("VehicleQuotePortType");
        assertThat(textOfFirst(document, "binding")).isEqualTo("VehicleQuoteBinding");
        assertThat(textOfFirst(document, "service")).isEqualTo("VehicleQuoteService");
    }

    @Test
    void wsdlPublishesRequestResponseAndFaultMessages() throws Exception {
        Document document = parse();

        assertThat(elements(document, "message"))
                .contains("GetVehicleQuoteInput", "GetVehicleQuoteOutput", "VehicleQuoteFaultMessage");
        assertThat(elements(document, "element"))
                .contains("GetVehicleQuoteRequest", "GetVehicleQuoteResponse", "VehicleQuoteFault");
        assertThat(elements(document, "part"))
                .contains("parameters");
    }

    @Test
    void wsdlDeclaresSoapBindingAndAddress() throws Exception {
        Document document = parse();

        NodeList bindings = document.getElementsByTagNameNS(
                "http://schemas.xmlsoap.org/wsdl/soap/", "binding");
        assertThat(bindings.getLength()).isEqualTo(1);
        assertThat(((Element) bindings.item(0)).getAttribute("style")).isEqualTo("document");
        assertThat(((Element) bindings.item(0)).getAttribute("transport"))
                .isEqualTo("http://schemas.xmlsoap.org/soap/http");

        NodeList addresses = document.getElementsByTagNameNS(
                "http://schemas.xmlsoap.org/wsdl/soap/", "address");
        assertThat(addresses.getLength()).isEqualTo(1);
        assertThat(((Element) addresses.item(0)).getAttribute("location"))
                .endsWith("/soap/vehicles");
    }

    private static String textOfFirst(Document document, String localName) {
        NodeList nodes = document.getElementsByTagNameNS("*", localName);
        assertThat(nodes.getLength()).as("Debe existir <%s>", localName).isGreaterThan(0);
        return ((Element) nodes.item(0)).getAttribute("name");
    }

    private static java.util.List<String> elements(Document document, String localName) {
        NodeList nodes = document.getElementsByTagNameNS("*", localName);
        java.util.List<String> names = new java.util.ArrayList<>();
        for (int i = 0; i < nodes.getLength(); i++) {
            names.add(((Element) nodes.item(i)).getAttribute("name"));
        }
        return names;
    }
}
