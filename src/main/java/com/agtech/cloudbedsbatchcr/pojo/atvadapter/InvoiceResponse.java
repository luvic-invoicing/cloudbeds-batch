package com.agtech.cloudbedsbatchcr.pojo.atvadapter;

import java.util.Date;

public class InvoiceResponse {

    private long consecutive;
    private String idErp;
    private String clave;
    private  Status status;
    private String xmlResponse;
    private Date date;

    public String getIdErp() {
        return idErp;
    }

    public void setIdErp(String idErp) {
        this.idErp = idErp;
    }

    public String getClave() {
        return clave;
    }

    public void setClave(String clave) {
        this.clave = clave;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getXmlResponse() {
        return xmlResponse;
    }

    public void setXmlResponse(String xmlResponse) {
        this.xmlResponse = xmlResponse;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public long getConsecutive()
    {
        return consecutive;
    }

    public void setConsecutive( long consecutive )
    {
        this.consecutive = consecutive;
    }

    @Override
    public String toString()
    {
        return "InvoiceResponse{" +
                "consecutive=" + consecutive +
                ", idErp='" + idErp + '\'' +
                ", clave='" + clave + '\'' +
                ", status=" + status +
                ", xmlResponse='" + xmlResponse + '\'' +
                ", date=" + date +
                '}';
    }
}
