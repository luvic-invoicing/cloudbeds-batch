/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.agtech.cloudbedsbatchcr.pojo.atvadapter;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class InvoiceLine {
    //private int id;
    private int lineNumber;

    @JsonBackReference
    private Invoice invoice;
    
    private String description;
    private int quantity;
    private String unidMeasure;
    private double unitPrice;
    private double amount;
    private double discountAmount;
    private String discountReason;
    private double subTotal;
    private double totalAmount;
    private String codigoExoneracion = "00";
    private String documentoExoneracion;
    private String institucionExoneracion;
    private Date fechaEmisionDIDI;

    //Cabys.
    private String productCode;
    
    @JsonManagedReference
    private List<InvoiceLineTax> lineTaxes;

    public Invoice getInvoice() {
        return invoice;
    }

    public void setInvoice(Invoice invoice) {
        this.invoice = invoice;
    }

    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getUnidMeasure() {
        return unidMeasure;
    }

    public void setUnidMeasure(String unidMeasure) {
        this.unidMeasure = unidMeasure;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(double unitPrice) {
        this.unitPrice = unitPrice;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public double getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(double discountAmount) {
        this.discountAmount = discountAmount;
    }

    public String getDiscountReason() {
        return discountReason;
    }

    public void setDiscountReason(String discountReason) {
        this.discountReason = discountReason;
    }

    public double getSubTotal() {
        return subTotal;
    }

    public void setSubTotal(double subTotal) {
        this.subTotal = subTotal;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public List<InvoiceLineTax> getLineTaxes() {
        return lineTaxes;
    }

    public void setLineTaxes(List<InvoiceLineTax> lineTaxes) {
        this.lineTaxes = lineTaxes;
    }

//    public int getId() {
//        return id;
//    }
//
//    public void setId(int id) {
//        this.id = id;
//    }

    public int getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public String getCodigoExoneracion() {
        return codigoExoneracion;
    }

    public void setCodigoExoneracion(String codigoExoneracion) {
        this.codigoExoneracion = codigoExoneracion;
    }

    public String getDocumentoExoneracion() {
        return documentoExoneracion;
    }

    public void setDocumentoExoneracion(String documentoExoneracion) {
        this.documentoExoneracion = documentoExoneracion;
    }

    public String getInstitucionExoneracion() {
        return institucionExoneracion;
    }

    public void setInstitucionExoneracion(String institucionExoneracion) {
        this.institucionExoneracion = institucionExoneracion;
    }

    public Date getFechaEmisionDIDI() {
        return fechaEmisionDIDI;
    }

    public void setFechaEmisionDIDI(Date fechaEmisionDIDI) {
        this.fechaEmisionDIDI = fechaEmisionDIDI;
    }

    public InvoiceLine(){
        this.lineTaxes = new ArrayList<InvoiceLineTax>();
    }
}
