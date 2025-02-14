package com.app.payloads;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CouponDTO {

    private Long id;
    private String code;
    private double discount;
    private boolean active;
}
