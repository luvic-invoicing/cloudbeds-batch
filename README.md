# cloudbeds-batch
Proceso de batch para enviar factura electronica desde el punto de venta de hoteles cloudbeds en Costa Rica

Swagger local: http://localhost:8050/swagger-ui.html

El mapeo fiscal consulta Accounting por separado para el `externalRelationKind`
de las transacciones y el `originId` de sus impuestos (`taxes` o `taxLines`).
Los impuestos se consultan por su `id` desde `mapFiscalDocumentTaxes`, reutilizando
la fecha minima de las transacciones. El `originId` se asigna al `taxID`;
si falta el `id` o el `originId`, el procesamiento falla explicitamente.