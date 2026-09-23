package com.agtech.cloudbedsbatchcr.services;

import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Base64;

@Service
public class EmailerService {

    private final Environment env;

    public EmailerService(Environment env) {
        this.env = env;
    }

    /** Obtiene el PDF desde Emailer y lo codifica en Base64 para la API de Cloudbeds. */
    public String obtenerPdfFactura(String claveHacienda) {
        String baseUrl = env.getRequiredProperty("url.emailer.pdf-invoice");
        String company = env.getRequiredProperty("emailer.pdf-invoice.company");
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .pathSegment(claveHacienda, company)
                .toUriString();

        ResponseEntity<byte[]> response = new RestTemplate().exchange(url, HttpMethod.GET, null, byte[].class);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null || response.getBody().length == 0) {
            throw new IllegalStateException("No fue posible obtener el PDF de la factura " + claveHacienda);
        }
        return Base64.getEncoder().encodeToString(response.getBody());
    }
}