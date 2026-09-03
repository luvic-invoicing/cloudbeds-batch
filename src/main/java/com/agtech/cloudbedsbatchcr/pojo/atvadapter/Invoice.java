/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.agtech.cloudbedsbatchcr.pojo.atvadapter;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 *
 * @author 
 */

@JsonInclude(Include.NON_NULL)
public class Invoice {
    
    private Long secuencia;
    private String idErp;
    private String fiscalConsecutive;
    private String billKey;
    //Se usa para especificar la clave de la factura que se desea anular y el texto indicando una justificación.
    private String reference;
    private String otherText;
    private String clientName;
    private String clientEmail;
    private String clientId;
    private String clientIdType; //01 Cédula Física, 02 Cédula Jurídica, 03 DIMEX, 04 NITE

    //Si es "1", se usa el atv_activity_client, si es "2" se usa el atv_activity_client2, ambos de la tabla customer
    //Sirve para indicar cual codigo de actividad usar desde la pantalla de Factura.
    //Si es nulo, se usa por default atv_activity_client
    private String activityCode;

    @JsonInclude(Include.NON_NULL)
    private String codeSellCondition;

    private String code;
    private Double exchange;
    @JsonManagedReference
    private CodesPaid codesPaid;

    private Date authorizationDate;
    @JsonInclude(Include.NON_NULL)
    private Date registryDate;

    private double totalSellExempt;
    private double totalSellTaxed;
    private double totalSell;
    private double totalDiscount;
    private double totalNetSell;
    private double totalTaxes;
    private double totalBill;
    private String applicationType;
    private String customerId;
    private DocumentType documentType;
    private String source;
    private Integer supplierId;

    private Boolean esExento;

    //Usuario que agregra la factura.
    private long userIdAdd;
    private String userNameAdd;

    @JsonManagedReference
    private List<InvoiceLine> lines;

    private List<InvoiceOtherCharges> invoiceOtherCharges;

    private ErpSystem erpSystem;

    private Integer pointOfSale;

    public ErpSystem getErpSystem() {
        return erpSystem;
    }

    public void setErpSystem(ErpSystem erpSystem) {
        this.erpSystem = erpSystem;
    }

    public Integer getPointOfSale() {
        return pointOfSale;
    }

    public void setPointOfSale(Integer pointOfSale) {
        this.pointOfSale = pointOfSale;
    }

    public ErpSystem getSystem() {
        return erpSystem;
    }

    public void setSystem(ErpSystem erpSystem) {
        this.erpSystem = erpSystem;
    }

    public String getFiscalConsecutive() {
        return fiscalConsecutive;
    }

    public void setFiscalConsecutive(String fiscalConsecutive) {
        this.fiscalConsecutive = fiscalConsecutive;
    }

    public String getBillKey() {
        return billKey;
    }

    public void setBillKey(String billKey) {
        this.billKey = billKey;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getOtherText() {
        return otherText;
    }

    public void setOtherText(String otherText) {
        this.otherText = otherText;
    }

    public Invoice() {
        registryDate = new Date();
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getClientEmail() {
        return clientEmail;
    }

    public void setClientEmail(String clientEmail) {
        this.clientEmail = clientEmail;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientIdType() {
        return clientIdType;
    }

    public void setClientIdType(String clientIdType) {
        this.clientIdType = clientIdType;
    }

    public double getTotalSell() {
        return totalSell;
    }

    public void setTotalSell(double totalSell) {
        this.totalSell = totalSell;
    }

    public double getTotalDiscount() {
        return totalDiscount;
    }

    public void setTotalDiscount(double totalDiscount) {
        this.totalDiscount = totalDiscount;
    }

    public double getTotalNetSell() {
        return totalNetSell;
    }

    public void setTotalNetSell(double totalNetSell) {
        this.totalNetSell = totalNetSell;
    }

    public double getTotalTaxes() {
        return totalTaxes;
    }

    public void setTotalTaxes(double totalTaxes) {
        this.totalTaxes = totalTaxes;
    }

    public double getTotalBill() {
        return totalBill;
    }

    public void setTotalBill(double totalBill) {
        this.totalBill = totalBill;
    }

    public double getTotalSellExempt() {
        return totalSellExempt;
    }

    public void setTotalSellExempt(double totalSellExempt) {
        this.totalSellExempt = totalSellExempt;
    }

    public String getCodeSellCondition() {
        return codeSellCondition;
    }

    public void setCodeSellCondition(String codeSellCondition) {
        this.codeSellCondition = codeSellCondition;
    }

    public Date getAuthorizationDate() {
        return authorizationDate;
    }

    public void setAuthorizationDate(Date authorizationDate) {
        this.authorizationDate = authorizationDate;
    }

    public Date getRegistryDate() {
        return registryDate;
    }

    public void setRegistryDate(Date registryDate) {
        this.registryDate = registryDate;
    }

    public List<InvoiceLine> getLines() {
        if(lines == null) {
            lines = new ArrayList<InvoiceLine>();
        }

        return lines;
    }

    public void setLines(List<InvoiceLine> lines) {
        this.lines = lines;
    }

    public long getSecuencia() {
        return secuencia;
    }

    public void setSecuencia(long Secuencia) {
        this.secuencia = Secuencia;
    }

    public String getIdErp() {
        return idErp;
    }

    public void setIdErp(String idErp) {
        this.idErp = idErp;
    }

    public CodesPaid getCodesPaid() {
        return codesPaid;
    }

    public void setCodesPaid(CodesPaid codesPaid) {
        this.codesPaid = codesPaid;
    }

    public double getTotalSellTaxed() {
        return totalSellTaxed;
    }

    public void setTotalSellTaxed(double totalSellTaxed) {
        this.totalSellTaxed = totalSellTaxed;
    }

    public String getCode()
    {
        return code;
    }

    public void setCode( String code )
    {
        this.code = code;
    }

    public Double getExchange()
    {
        return exchange;
    }

    public void setExchange( Double exchange )
    {
        this.exchange = exchange;
    }

    public List<InvoiceOtherCharges> getInvoiceOtherCharges() {
        if(invoiceOtherCharges == null) {
            invoiceOtherCharges = new ArrayList<InvoiceOtherCharges>();
        }

        return invoiceOtherCharges;
    }

    public void setInvoiceOtherCharges(List<InvoiceOtherCharges> invoiceOtherCharges) {
        this.invoiceOtherCharges = invoiceOtherCharges;
    }

    public String getApplicationType()
    {
        return applicationType;
    }

    public void setApplicationType( String applicationType )
    {
        this.applicationType = applicationType;
    }

    public String getCustomerId()
    {
        return customerId;
    }

    public void setCustomerId( String customerId )
    {
        this.customerId = customerId;
    }

    public DocumentType getDocumentType()
    {
        return documentType;
    }

    public void setDocumentType( DocumentType documentType )
    {
        this.documentType = documentType;
    }

    public String getSource()
    {
        return source;
    }

    public void setSource( String source )
    {
        this.source = source;
    }

    public Integer getSupplierId()
    {
        return supplierId;
    }

    public void setSupplierId( Integer supplierId )
    {
        this.supplierId = supplierId;
    }

    public long getUserIdAdd() {
        return userIdAdd;
    }

    public void setUserIdAdd(long userIdAdd) {
        this.userIdAdd = userIdAdd;
    }

    public String getUserNameAdd() {
        return userNameAdd;
    }

    public void setUserNameAdd(String userNameAdd) {
        this.userNameAdd = userNameAdd;
    }

    public String getActivityCode() {
        return activityCode;
    }

    public void setActivityCode(String activityCode) {
        this.activityCode = activityCode;
    }

    public Boolean getEsExento() {
        return esExento;
    }

    public void setEsExento(Boolean esExento) {
        this.esExento = esExento;
    }

    @Override
    public String toString()
    {
        return "Invoice{" +
                "secuencia=" + secuencia +
                ", idErp='" + idErp + '\'' +
                ", fiscalConsecutive='" + fiscalConsecutive + '\'' +
                ", billKey='" + billKey + '\'' +
                ", reference='" + reference + '\'' +
                ", otherText='" + otherText + '\'' +
                ", clientName='" + clientName + '\'' +
                ", clientEmail='" + clientEmail + '\'' +
                ", clientId='" + clientId + '\'' +
                ", clientIdType='" + clientIdType + '\'' +
                ", codeSellCondition='" + codeSellCondition + '\'' +
                ", code='" + code + '\'' +
                ", exchange=" + exchange +
                ", codesPaid=" + codesPaid +
                ", authorizationDate=" + authorizationDate +
                ", registryDate=" + registryDate +
                ", totalSellExempt=" + totalSellExempt +
                ", totalSellTaxed=" + totalSellTaxed +
                ", totalSell=" + totalSell +
                ", totalDiscount=" + totalDiscount +
                ", totalNetSell=" + totalNetSell +
                ", totalTaxes=" + totalTaxes +
                ", totalBill=" + totalBill +
                ", lines=" + lines +
                ", otherCharges=" + invoiceOtherCharges +
                '}';
    }
}
