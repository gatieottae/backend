package com.gatieottae.backend.domain.expense;

/** DB enum(gatieottae.transfer_status)과 문자열 매핑 */
public enum TransferStatus {
    REQUESTED, SENT, CONFIRMED, CANCELED, ROLLED_BACK
}