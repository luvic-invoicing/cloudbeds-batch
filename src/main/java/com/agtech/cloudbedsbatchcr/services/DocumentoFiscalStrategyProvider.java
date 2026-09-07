package com.agtech.cloudbedsbatchcr.services;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DocumentoFiscalStrategyProvider {

    private final List<DocumentoFiscalStrategy> strategies;

    public DocumentoFiscalStrategyProvider(List<DocumentoFiscalStrategy> strategies) {
        this.strategies = strategies;
    }

    public DocumentoFiscalStrategy resolve(String status) {
        return strategies.stream()
                .filter(strategy -> strategy.supports(status))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(String.format("No hay estrategia para el status %s", status)));
    }
}

