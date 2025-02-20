package com.app.services;

import com.app.entites.Member;
import com.app.entites.User;
import com.app.exceptions.ResourceNotFoundException;
import com.app.payloads.MemberDTO;
import com.app.repositories.MemberRepo;
import com.app.repositories.UserRepo;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

        return modelMapper.map(newMember, MemberDTO.class);

    }


}
