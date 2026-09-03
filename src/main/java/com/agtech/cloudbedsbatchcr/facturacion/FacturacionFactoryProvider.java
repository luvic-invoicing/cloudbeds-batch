package com.agtech.cloudbedsbatchcr.facturacion;

import com.agtech.cloudbedsbatchcr.facturacion.cr.CostaRicaFacturacionFactory;
import com.agtech.cloudbedsbatchcr.facturacion.pa.PanamaFacturacionFactory;
import org.springframework.stereotype.Component;

@Component
public class FacturacionFactoryProvider {

    public FacturacionAbstractFactory obtenerFactory(String countryCode) {
        if (countryCode == null) {
            return new CostaRicaFacturacionFactory();
        }

        if ("PA".equalsIgnoreCase(countryCode)) {
            return new PanamaFacturacionFactory();
        }

        return new CostaRicaFacturacionFactory();
    }
}

