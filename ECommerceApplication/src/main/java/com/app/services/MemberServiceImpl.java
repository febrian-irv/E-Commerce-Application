package com.app.services;

import com.app.entites.Member;
import com.app.entites.User;
import com.app.exceptions.APIException;
import com.app.exceptions.ResourceNotFoundException;
import com.app.payloads.MemberDTO;
import com.app.payloads.MemberResponse;
import com.app.repositories.MemberRepo;
import com.app.repositories.UserRepo;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Transactional
@Service
public class MemberServiceImpl implements MemberService{

    @Autowired
    public MemberRepo memberRepo;

    @Autowired
    public UserRepo userRepo;

    @Autowired
    private ModelMapper modelMapper;

    @Override
    public MemberDTO createMember(Long userId){

        User user= userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "userId", userId));

        Member newMember = new Member();

        newMember.setUser(user);
        newMember = memberRepo.save(newMember);

        user.setMember(newMember);
        userRepo.save(user);

        return modelMapper.map(newMember, MemberDTO.class);
    }

    @Override
    public MemberDTO getMember(Long memberId){
        Member member = memberRepo.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member", "memberId", memberId));


        return modelMapper.map(member, MemberDTO.class);
    }

    @Override
    public MemberResponse getAllMembers(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder){

        Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageDetails = PageRequest.of(pageNumber, pageSize, sortByAndOrder);

        Page<Member> pageMembers = memberRepo.findAll(pageDetails);

        List<Member> members = pageMembers.getContent();

        List<MemberDTO> memberDTOs = members.stream().map(member -> modelMapper.map(member, MemberDTO.class))
                .collect(Collectors.toList());

        if (memberDTOs.isEmpty()) {
            throw new APIException("No members placed yet");
        }

        MemberResponse memberResponse = new MemberResponse();

        memberResponse.setContent(memberDTOs);
        memberResponse.setPageNumber(pageMembers.getNumber());
        memberResponse.setPageSize(pageMembers.getSize());
        memberResponse.setTotalElements(pageMembers.getTotalElements());
        memberResponse.setTotalPages(pageMembers.getTotalPages());
        memberResponse.setLastPage(pageMembers.isLast());

        return memberResponse;
    }
}
