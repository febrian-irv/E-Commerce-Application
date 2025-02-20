package com.app.payloads;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemberDTO {

    private Long memberId;
    private double credit = 0.0;
    private String email;
}
