package com.app.repositories;

import com.app.entites.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemberRepo extends JpaRepository<Member, Long> {
    @Query("SELECT m FROM Member m WHERE m.user.id = :userId")
    Member findByUserId(@Param("userId") Long userId);
}
