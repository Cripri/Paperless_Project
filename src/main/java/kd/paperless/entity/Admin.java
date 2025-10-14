package kd.paperless.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "admin")
@Getter
@Setter
public class Admin {

  @Id
  @Column(name = "admin_id")
  private Long adminId;

  @Column(name = "admin_name", nullable = false)
  private String adminName;

  @Column(name = "phone_num")
  private String phoneNum;
}