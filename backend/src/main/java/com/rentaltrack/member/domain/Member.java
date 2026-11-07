package com.rentaltrack.member.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "`member`")
@SecondaryTable(
  name = "member_credential",
  pkJoinColumns = @PrimaryKeyJoinColumn(name = "member_id")
)
public class Member {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "member_id", columnDefinition = "BIGINT UNSIGNED")
  private Long id;

  @Column(name = "login_id", nullable = false, length = 50)
  private String loginId;

  @Column(name = "member_name", nullable = false, length = 100)
  private String name;

  @Column(nullable = false, length = 254)
  private String email;

  @Enumerated(EnumType.STRING)
  @Column(name = "member_role_cd", nullable = false, length = 20)
  private MemberRole role = MemberRole.CUSTOMER;

  @Column(
    name = "created_at",
    nullable = false,
    columnDefinition = "DATETIME(3)"
  )
  private LocalDateTime createdAt = LocalDateTime.now();

  @Column(
    name = "password_hash",
    table = "member_credential",
    nullable = false,
    length = 100
  )
  private String passwordHash;

  @Enumerated(EnumType.STRING)
  @Column(table = "member_credential", nullable = false, length = 20)
  private MemberStatus status = MemberStatus.ACTIVE;

  protected Member() {}

  public Member(
    String loginId,
    String email,
    String name,
    String passwordHash
  ) {
    this.loginId = loginId;
    this.email = email;
    this.name = name;
    this.passwordHash = passwordHash;
  }

  public Long getId() {
    return id;
  }

  public String getLoginId() {
    return loginId;
  }

  public String getName() {
    return name;
  }

  public String getEmail() {
    return email;
  }

  public MemberRole getRole() {
    return role;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public MemberStatus getStatus() {
    return status;
  }

  public void changeRole(MemberRole role) {
    this.role = role;
  }
}
