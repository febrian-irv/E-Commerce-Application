package com.app.services;

import com.app.payloads.MemberDTO;
import com.app.payloads.MemberResponse;

public interface MemberService {

    MemberDTO createMember(Long userId);

    MemberDTO getMember(Long memberId);
//
//    MemberResponse getAllMembers(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder);


}
