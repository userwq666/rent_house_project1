package com.renthouse.dto;

import lombok.Data;

@Data
public class TerminationCounterpartyDecisionRequest {
    private Boolean approve;
    private String comment;
}
