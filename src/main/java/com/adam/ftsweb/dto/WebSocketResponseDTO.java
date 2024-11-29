package com.adam.ftsweb.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class WebSocketResponseDTO extends WebSocketDTO {

    private boolean success;

}
