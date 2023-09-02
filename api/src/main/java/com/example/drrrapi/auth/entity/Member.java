package com.example.drrrapi.auth.entity;


import com.example.drrrapi.web.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Entity
@Getter
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "drrr_member")
@PrimaryKeyJoinColumn(name = "member_id")
public class Member extends BaseEntity {

    @Column(nullable = false)
    private String nickname;

    @Embedded
    private Address address;

    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    private Gender gender;


    private String birthYear;

    private String provider;
    @Column(unique = true)
    private String providerId;

    private String imageUrl;

    @Column(nullable = false, unique = true)
    private String email;


    @Enumerated(EnumType.STRING)
    private MemberRole role;


    public static Member createMember(String email, String nickname, Gender gender, String provider, String providerId,
                                      String imageUrl, MemberRole role) {
        return Member.builder()
                .email(email)
                .nickname(nickname)
                .gender(gender)
                .provider(provider)
                .providerId(providerId)
                .imageUrl(imageUrl)
                .role(role)
                .build();
    }
}
