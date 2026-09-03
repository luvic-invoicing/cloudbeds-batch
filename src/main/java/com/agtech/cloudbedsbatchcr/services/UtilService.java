package com.agtech.cloudbedsbatchcr.services;

import com.agtech.cloudbedsbatchcr.pojo.atvadapter.KeyGeneratorRequest;
import com.agtech.cloudbedsbatchcr.pojo.atvadapter.KeyGeneratorResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class UtilService {

    private static final Logger logger = LogManager.getLogger( UtilService.class );

    @Autowired
    private Environment env;

    public KeyGeneratorResponse generateKey(KeyGeneratorRequest request )
    {
        try
        {
            final String uri = this.env.getProperty("url.keygenerator");
            logger.info(String.format(  "generateKey url %s", uri ));
            RestTemplate restTemplate = new RestTemplate();
            KeyGeneratorResponse response = restTemplate.postForObject( uri, request, KeyGeneratorResponse.class );
            return response;
        }
        catch ( Exception e )
        {
            System.out.println( e.getMessage() );
            throw e;
        }
    }
}
