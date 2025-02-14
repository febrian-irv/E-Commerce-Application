package com.app.services;

import com.app.entites.Coupon;
import com.app.payloads.CouponDTO;
import com.app.payloads.CouponResponse;

public interface CouponService {

    CouponDTO createCoupon(Coupon coupon);

    CouponResponse getCoupons(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder);

    String deleteCoupon(Long couponId);
}
