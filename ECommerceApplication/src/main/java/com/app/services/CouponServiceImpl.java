package com.app.services;


import com.app.entites.Coupon;
import com.app.exceptions.APIException;
import com.app.exceptions.ResourceNotFoundException;
import com.app.payloads.CouponDTO;
import com.app.payloads.CouponResponse;
import com.app.repositories.CouponRepo;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Transactional
@Service
public class CouponServiceImpl implements CouponService{

    @Autowired
    private CouponRepo couponRepo;

    @Autowired
    private ModelMapper modelMapper;

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int CODE_LENGTH = 5; // Adjust the length as needed
    private final Random random = new SecureRandom();

    @Override
    public CouponDTO createCoupon(Coupon coupon) {

        String uniqueCode = generateUniqueCouponCode();
        coupon.setCode(uniqueCode);

        Coupon savedCoupon = couponRepo.save(coupon);

        return modelMapper.map(savedCoupon, CouponDTO.class);
    }

    @Override
    public CouponResponse getCoupons(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder){
        Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();

        Pageable pageDetails = PageRequest.of(pageNumber, pageSize, sortByAndOrder);

        Page<Coupon> pageCoupons = couponRepo.findAll(pageDetails);

        List<Coupon> coupons = pageCoupons.getContent();

        System.out.println(coupons);

        if (coupons.size() == 0) {
            throw  new APIException("No coupon is created till now");
        }

        List<CouponDTO> couponDTOs = coupons.stream().map(coupon -> modelMapper.map(coupon, CouponDTO.class)).collect(Collectors.toList());

        CouponResponse couponResponse = new CouponResponse();

        couponResponse.setContent(couponDTOs);
        couponResponse.setPageNumber(pageCoupons.getNumber());
        couponResponse.setPageSize(pageCoupons.getSize());
        couponResponse.setTotalElements(pageCoupons.getTotalElements());
        couponResponse.setTotalPages(pageCoupons.getTotalPages());
        couponResponse.setLastPage(pageCoupons.isLast());

        return couponResponse;
    }

    @Override
    public String deleteCoupon(Long couponId){
        Coupon coupon = couponRepo.findById(couponId).orElseThrow(() -> new ResourceNotFoundException("Coupon", "couponId", couponId));

        coupon.setActive(true);

        couponRepo.save(coupon);

        return "Coupon with ID " + couponId + " has been soft deleted";

    }

    private String generateUniqueCouponCode() {
        String code;
        do {
            code = generateRandomCode(CODE_LENGTH);
        } while (couponRepo.existsByCode(code));

        return code;
    }

    private String generateRandomCode(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        return sb.toString();
    }





}
