<h1 align="center">📨 Paperless_Project</h1>
<p align="center">
  시민이 온라인으로 민원을 신청하고, 처리 과정을 투명하게 확인할 수 있는 <b>무서류(페이퍼리스) 전자 민원 포털</b>입니다.<br/>
  <em>“종이 없는 행정, 더 편리한 민원”</em>
</p>
<img width="1800" height="500" alt="image" src="https://github.com/user-attachments/assets/62b40137-d6b4-4c39-9f51-6ccf791b4bc6" />

## 🌟 주요 기능

### 🧑‍💻 회원 기능
- 일반 회원가입 / 로그인  
- 카카오 · 네이버 **소셜 로그인(OAuth)**  
- 마이페이지에서 내 정보 수정 및 연동 관리  

> 구현: **이정향**

---

### 📢 공지사항
- 관리자 공지 목록 및 상세 조회  
- 제목, 작성자, 등록일 기준 검색  
- 최신순 정렬 및 페이지네이션  

> 구현: **황금성**

---

### 📮 신문고(민원)
- 주민이 직접 **불편사항 / 건의사항 등록**
- 첨부파일 업로드 (PDF, 이미지 등)
- 마이페이지에서 민원 상태(`접수`, `처리중`, `완료`, `반려`) 확인
- 관리자의 답변 내용 실시간 반영  

> 구현: **황금성**

---

### 📄 종이 없는 행정 (Paperless)
- 오프라인 문서를 대체하는 **전자 신청서**
- 첨부파일 업로드 (MinIO 연동)
- 신청 내역 및 처리 상태 조회  
- PDF 문서 브라우저 내 미리보기 지원  

> 구현: **최원창**

---

## 🧱 기술 스택

| 구분 | 기술 |
|------|------|
| **Backend** | Spring Boot 3.x / Spring MVC / Spring Security 6 / Spring Data JPA |
| **Database** | Oracle 19c |
| **Template** | Thymeleaf (서버사이드 렌더링) |
| **Storage** | MinIO (파일 업로드, 다운로드, 미리보기) |
| **OAuth** | Kakao / Naver API |
| **Validation & Utils** | Jakarta Validation / Lombok |
| **Build Tool** | Maven |
| **Language** | Java 17+ |

---

## 🗂 도메인 구조

| 도메인 | 설명 |
|--------|------|
| `User` | 사용자 계정, 로그인, 소셜 연동, 마이페이지 |
| `Notice` | 공지사항 게시글 |
| `Sinmungo` | 민원(신문고) 데이터 — 제목, 내용, 상태, 답변 등 |
| `PaperlessDoc` | 전자문서(무서류 행정 신청) |
| `Attachment` | 첨부파일 메타데이터 (MinIO Object Key, 파일명 등) |

---

## ⚙️ 실행 방법

### 1️⃣ 환경 설정

`src/main/resources/application.properties` 예시:

```properties
# ⚙️ Application Settings
server.port=8080
spring.application.name=Paperless_Project

# 🗄️ Oracle Database
spring.datasource.url=jdbc:oracle:thin:@<HOST>:1521:<SID>
spring.datasource.driver-class-name=oracle.jdbc.OracleDriver
spring.datasource.username=<USERNAME>
spring.datasource.password=<PASSWORD>

# 🧩 JPA Settings
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

# 📎 File Upload Limits
spring.servlet.multipart.max-file-size=100MB
spring.servlet.multipart.max-request-size=100MB

# ☁️ MinIO Storage
storage.minio.endpoint=http://<MINIO_HOST>:9000
storage.minio.access-key=<ACCESS_KEY>
storage.minio.secret-key=<SECRET_KEY>
storage.minio.bucket=attachments
storage.minio.secure=false
```


### 2️⃣ 빌드 & 실행
```mvn clean package
java -jar target/Paperless_Project-0.0.1-SNAPSHOT.jar
```
또는 IDE에서 Spring Boot Run으로 바로 실행해도 됩니다.

📄 프로젝트 구조
```text
Paperless_Project/
├─ controller/       # 컨트롤러 (회원, 공지, 신문고 등)
├─ entity/           # JPA 엔티티
├─ dto/              # DTO 클래스
├─ repository/       # JpaRepository 인터페이스
├─ service/          # 비즈니스 로직
├─ resources/
│  ├─ templates/     # Thymeleaf 템플릿
│  ├─ static/        # CSS, JS, 이미지
│  └─ application.properties
└─ pom.xml
```
### 🔐 보안

- Spring Security 기반 로그인 및 접근 제어
- CSRF 토큰 자동 주입 (Thymeleaf form)
- 비밀번호는 BCrypt로 암호화 저장
- 소셜 로그인 시 SecurityContext에 세션 인증정보 주입

### 🚀 향후 개선 예정

- 이메일 알림 / 푸시 알림 기능
- 공지사항 검색 UX 개선
- 다국어(i18n) 지원

### 👥 기여자 (Contributors)

| 이름 | 담당 기능 |
|------|------------|
| **이정향** | 회원가입, 로그인, 카카오·네이버 소셜 로그인, 마이페이지 |
| **황금성** | 공지사항, 신문고(민원) |
| **최원창** | 종이 없는 행정 (전자문서) |

### 📜 라이선스

이 프로젝트는 Apache License 2.0하에 배포됩니다.

```pgsql
Copyright 2025 Paperless
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
```

<h3 align="center">✨ Paperless_Project ✨</h3> <p align="center"> 시민이 민원을 등록하고 처리 과정을 투명하게 확인할 수 있는 <br/> <b>무서류 전자 민원 시스템</b> </p>
