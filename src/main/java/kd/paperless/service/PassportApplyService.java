package kd.paperless.service;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import kd.paperless.dto.PassportForm;
import kd.paperless.entity.Attachment;
import kd.paperless.entity.PaperlessDoc;
import kd.paperless.repository.AttachmentRepository;
import kd.paperless.repository.PaperlessDocRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PassportApplyService {

    private final PaperlessDocRepository docRepository;
    private final AttachmentRepository attachmentRepository;
    private final MinioClient minioClient;

    @Value("${storage.minio.bucket}")
    private String bucket;

    @Transactional
    public Long savePassportApplication(Long userId, PassportForm form) {
        // 1) PaperlessDoc 생성
        PaperlessDoc doc = new PaperlessDoc(userId, "passport_reissue");
        doc.setConsentYn('N');
        doc.setStatus(PaperlessDoc.PaperlessStatus.RECEIVED);

        Map<String, Object> json = new LinkedHashMap<>();

        // 기본 선택
        json.put("passportType", up(form.getPassportType()));
        if ("TRAVEL_CERT".equalsIgnoreCase(s(form.getPassportType()))) {
            json.put("travelMode", up(form.getTravelMode()));
        }
        json.put("pageCount", form.getPageCount());
        json.put("validity", up(form.getValidity()));

        // 인적 사항
        json.put("koreanName", s(form.getKoreanName()));
        String rrnFront = onlyDigits(form.getRrnFront(), 6);
        String rrnBack  = onlyDigits(form.getRrnBack(), 7);
        json.put("rrnFront", rrnFront);
        json.put("rrnBackMask", (rrnBack != null && rrnBack.length()==7) ? rrnBack.charAt(0)+"******" : null);
        json.put("phone", s(form.getPhone()));
        json.put("emergency", Map.of(
                "name", s(form.getEmergencyName()),
                "relation", s(form.getEmergencyRelation()),
                "phone", s(form.getEmergencyPhone())
        ));
        json.put("english", Map.of(
                "lastName", upLetters(form.getEngLastName()),
                "firstName", upLetters(form.getEngFirstName()),
                "spouseLastName", upLetters(form.getSpouseEngLastName())
        ));

        // 옵션
        json.put("braillePassportYn", yn(form.getBraillePassport()));
        json.put("deliveryWantedYn", yn(form.getDeliveryWanted()));
        if ("Y".equalsIgnoreCase(yn(form.getDeliveryWanted()))) {
            json.put("delivery", Map.of(
                    "postcode", s(form.getDeliveryPostcode()),
                    "address1", s(form.getDeliveryAddress1()),
                    "address2", s(form.getDeliveryAddress2())
            ));
        }
        json.put("submittedAtTs", java.time.LocalDateTime.now().toString());

        doc.setExtraJson(json);
        doc = docRepository.save(doc); // plId 생성

        // 2) 사진 파일 업로드 + 첨부 저장
        if (form.getPhotoFile() != null && !form.getPhotoFile().isEmpty()) {
            Attachment att = uploadToMinioAndSaveAttachment(
                "PASSPORT_PHOTO",
                doc.getPlId(),
                form.getPhotoFile()
            );
            Map<String, Object> photoMeta = new LinkedHashMap<>();
            photoMeta.put("attachmentId", att.getFileId());
            photoMeta.put("objectKey", att.getFileUri());
            photoMeta.put("fileName", att.getFileName());
            photoMeta.put("mimeType", att.getMimeType());
            photoMeta.put("fileSize", att.getFileSize());
            photoMeta.put("downloadUrl", "/files/" + att.getFileId() + "/download");
            doc.getExtraJson().put("photo", photoMeta);
        }

        return doc.getPlId();
    }

    private Attachment uploadToMinioAndSaveAttachment(String targetType, Long targetId, MultipartFile file) {
        try (InputStream in = file.getInputStream()) {
            String original = Optional.ofNullable(file.getOriginalFilename()).orElse("file");
            String safeName = (original.isBlank() ? "file" : original);
            String objectKey = buildObjectKey(safeName);

            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .contentType(file.getContentType())
                    .stream(in, file.getSize(), -1)
                    .build()
            );

            Attachment saved = attachmentRepository.save(
                Attachment.builder()
                    .targetType(targetType.toUpperCase(Locale.ROOT))
                    .targetId(targetId)
                    .fileUri(objectKey)
                    .fileName(safeName)
                    .mimeType(file.getContentType())
                    .fileSize(file.getSize())
                    .build()
            );
            return saved;
        } catch (Exception e) {
            throw new IllegalStateException("여권 사진 업로드 실패: " + e.getMessage(), e);
        }
    }

    private static String buildObjectKey(String filename) {
        LocalDate d = LocalDate.now();
        return String.format("%04d/%02d/%02d/%s__%s",
                d.getYear(), d.getMonthValue(), d.getDayOfMonth(),
                java.util.UUID.randomUUID(), filename);
    }

    private static String s(String v) { return v == null ? null : v.trim(); }
    private static String up(String v) { return v == null ? null : v.trim().toUpperCase(Locale.ROOT); }
    private static String upLetters(String v) { return v == null ? null : v.trim().toUpperCase(Locale.ROOT); }
    private static String yn(String v) {
        if (v == null) return "N";
        String t = v.trim().toUpperCase(Locale.ROOT);
        return ("Y".equals(t) || "YES".equals(t) || "TRUE".equals(t)) ? "Y" : "N";
    }
    private static String onlyDigits(String v, int maxLen) {
        if (v == null) return null;
        String d = v.replaceAll("\\D+", "");
        return d.length() > maxLen ? d.substring(0, maxLen) : d;
    }
}
