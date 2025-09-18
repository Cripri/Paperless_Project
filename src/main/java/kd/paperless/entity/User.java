package kd.paperless.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter @Setter
public class User {

    @Id
    @SequenceGenerator(name = "seq_users", sequenceName = "seq_users", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_users")
    @Column(name = "user_id")
    private Long id;

    @Column(name = "login_id", length = 50, nullable = false, unique = true)
    private String loginId;

    @Column(name = "password_hash", length = 255, nullable = false)
    private String passwordHash;

    @Column(name = "user_name", length = 100, nullable = false)
    private String userName;

    @Column(name = "user_birth")
    private LocalDate userBirth;

    @Column(name = "phone_num", length = 20)
    private String phoneNum;

    @Column(name = "tel_num", length = 20)
    private String telNum;

    @Column(name = "email", length = 200, unique = true)
    private String email;

    @Column(name = "postcode", length = 10)
    private String postcode;

    @Column(name = "addr1", length = 200)
    private String addr1;

    @Column(name = "addr2", length = 200)
    private String addr2;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "status", length = 20)
    private String status; // ACTIVE / INACTIVE / SUSPENDED / DELETED
}