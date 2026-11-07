package com.rentaltrack.member.repository;

import com.rentaltrack.member.domain.Member;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {
  boolean existsByEmailIgnoreCase(String email);
  boolean existsByLoginIdIgnoreCase(String loginId);
  Optional<Member> findByLoginIdIgnoreCase(String loginId);
}
