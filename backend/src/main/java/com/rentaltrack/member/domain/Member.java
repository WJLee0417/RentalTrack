package com.rentaltrack.member.domain;

import com.rentaltrack.global.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "member_account")
public class Member extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", length = 100)
    private String passwordHash;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MemberStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private MemberRole role;

    protected Member() {
    }

    public Member(String email, String name) {
        this(email, name, null, MemberRole.USER);
    }

    public Member(String email, String name, MemberRole role) {
        this(email, name, null, role);
    }

    public Member(String email, String name, String passwordHash) {
        this(email, name, passwordHash, MemberRole.USER);
    }

    public Member(
            String email,
            String name,
            String passwordHash,
            MemberRole role
    ) {
        this.email = email;
        this.name = name;
        this.passwordHash = passwordHash;
        this.status = MemberStatus.ACTIVE;
        this.role = role;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getName() {
        return name;
    }

    public MemberStatus getStatus() {
        return status;
    }

    public MemberRole getRole() {
        return role;
    }

    public void suspend() {
        this.status = MemberStatus.SUSPENDED;
    }

    public void withdraw() {
        this.status = MemberStatus.WITHDRAWN;
    }
}
