package kd.paperless.service;

import kd.paperless.dto.ResidentRegistrationForm;

public interface PdfService {
    /** 폼으로 미리보기 PDF를 만들고 식별자(fileId)를 반환 */
    String makePreview(ResidentRegistrationForm form) throws Exception;
}