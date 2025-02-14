package com.app.repositories;

import com.app.entites.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponRepo extends JpaRepository<Coupon, Long>{
    
    Coupon findByCode(String code);

    boolean existsByCode(String code);
}
