package com.agtech.cloudbedsbatchcr.pojo.cloudbeds;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class CBReservationData {

    @JsonProperty("status")
    private CBReservationStatus status;

    @JsonProperty("customFields")
    private List<Object> customFields;

    @JsonProperty("mainGuestDetails")
    private CBMainGuestDetails mainGuestDetails;

    @JsonProperty("reservationRooms")
    private List<CBReservationRoom> reservationRooms;

    @JsonProperty("reservationRoomsTotal")
    private Double reservationRoomsTotal;

    @JsonProperty("reservationAdjustments")
    private List<CBReservationAdjustment> reservationAdjustments;

    @JsonProperty("reservationAdjustmentsTotal")
    private double reservationAdjustmentsTotal;

    @JsonProperty("reservationPayments")
    private List<CBReservationPayment> reservationPayments;

    @JsonProperty("reservationPaymentsTotal")
    private Double reservationPaymentsTotal;

    @JsonProperty("reservationAdditionalProducts")
    private List<CBReservationAdditionalProduct> reservationAdditionalProducts;

    @JsonProperty("reservationAdditionalProductsTotal")
    private Double reservationAdditionalProductsTotal;

    @JsonProperty("reservationAddOnProducts")
    private List<Object> reservationAddOnProducts;

    @JsonProperty("reservationAddOnProductsTotal")
    private Double reservationAddOnProductsTotal;

    @JsonProperty("reservationTaxes")
    private List<CBReservationTax> reservationTaxes;

    @JsonProperty("reservationTaxesTotal")
    private Double reservationTaxesTotal;

    @JsonProperty("reservationFees")
    private List<Object> reservationFees;

    @JsonProperty("reservationFeesTotal")
    private Double reservationFeesTotal;

    @JsonProperty("balance")
    private Double balance;

    @JsonProperty("balanceDetailed")
    private CBBalanceDetailed balanceDetailed;
}
