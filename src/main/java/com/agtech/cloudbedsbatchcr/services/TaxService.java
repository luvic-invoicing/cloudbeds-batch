package com.agtech.cloudbedsbatchcr.services;

import com.agtech.cloudbedsbatchcr.entities.CbProperties;
import com.agtech.cloudbedsbatchcr.pojo.cloudbeds.CBTaxes;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class TaxService {

    private static final Logger logger = LogManager.getLogger(CBService.class);

    @Cacheable(value = "taxes", key = "T(java.util.Objects).hash(#property?.propertyId)")
    public CBTaxes getTaxes(CbProperties property, String token) throws Exception {
        return getTaxesInternal(property, token);
    }

    private CBTaxes getTaxesInternal(CbProperties property, String token) throws Exception {
        String url = String.format(
                "%s/getTaxesAndFees?propertyID=%s",
                property.getCbAccount().getApiUrl(),
                property.getPropertyId()
        );

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.add("User-Agent", "Mozilla/5.0");
        headers.add("Authorization", String.format("Bearer %s", token));
        HttpEntity<?> entity = new HttpEntity<>(headers);
        ResponseEntity<CBTaxes> response = restTemplate.exchange(url, HttpMethod.GET, entity, CBTaxes.class);

        if (response.getStatusCode() == HttpStatus.OK) {
            logger.info("Successfully retrieved tax information");
            return response.getBody();
        } else {
            logger.error("Error retrieving tax information");
            throw new Exception(response.getStatusCode().getReasonPhrase());
        }
    }

    @Autowired
    private CacheManager cacheManager;

    @Scheduled(fixedRate = 3600000) // cada hora
    public void evictTaxesCache() {
        cacheManager.getCache("taxes").clear();
    }
}
