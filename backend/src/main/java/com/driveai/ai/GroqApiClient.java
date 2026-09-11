package com.driveai.ai;

/**
 * Abstracción sobre la llamada HTTP a Groq para permitir tests unitarios
 * sin servidor real.
 */
public interface GroqApiClient {

    /**
     * Ejecuta un chat completion con Groq y devuelve el contenido del
     * message completado.
     *
     * @param systemPrompt instrucciones + catálogo compacto de vehículos
     * @param userPrompt   consulta del cliente
     * @return el contenido textual (JSON) generado por el modelo
     */
    String chatCompletion(String systemPrompt, String userPrompt);
}