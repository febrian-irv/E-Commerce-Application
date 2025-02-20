package com.app.controllers;

import com.app.entites.User;
import com.app.payloads.MemberDTO;
import com.app.services.MemberService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@SecurityRequirement(name = "E-Commerce Application")
public class MemberController {

    @Autowired
    private MemberService memberService;

    @PostMapping("/admin/member")
    public ResponseEntity<MemberDTO> createMember(@RequestBody User user){


        MemberDTO memberDTO = memberService.createMember(user.getUserId());

        return new ResponseEntity<MemberDTO>(memberDTO, HttpStatus.CREATED);
    }

    @GetMapping("/public/member/{memberId}")
    public ResponseEntity<MemberDTO> getMember(@PathVariable Long memberId){

        MemberDTO memberDTO = memberService.getMember(memberId);

        return new ResponseEntity<MemberDTO>(memberDTO, HttpStatus.FOUND);
    }
}
