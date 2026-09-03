/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.agtech.cloudbedsbatchcr.pojo.atvadapter;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 *
 * @author Universal
 */

@JsonInclude(JsonInclude.Include.NON_NULL)
public class InvoiceLineTax {
    //private int id;
    private String code;
    private double rate;
    private double amount;

    private String taxCode;
    private String rateTaxCode;
    
    @JsonBackReference
    private InvoiceLine invoiceLine;

    public double getRate() {
        return rate;
    }

    public void setRate(double rate) {
        this.rate = rate;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public InvoiceLine getInvoiceLine() {
        return invoiceLine;
    }

    public void setInvoiceLine(InvoiceLine invoiceLine) {
        this.invoiceLine = invoiceLine;
    }

//    public int getId() {
//        return id;
//    }
//
//    public void setId(int id) {
//        this.id = id;
//    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getTaxCode() {
        return taxCode;
    }

    public void setTaxCode(String taxCode) {
        this.taxCode = taxCode;
    }

    public String getRateTaxCode() {
        return rateTaxCode;
    }

    public void setRateTaxCode(String rateTaxCode) {
        this.rateTaxCode = rateTaxCode;
    }
}
