package kd.paperless.account;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import org.springframework.security.crypto.password.PasswordEncoder;

public final class PasswordPolicyUtil {

    private PasswordPolicyUtil() {}

    public static final int MIN_LENGTH = 8;
    public static final int NEED_CATEGORIES = 2;

    private static final Pattern UPPER   = Pattern.compile(".*[A-Z].*");
    private static final Pattern LOWER   = Pattern.compile(".*[a-z].*");
    private static final Pattern DIGIT   = Pattern.compile(".*\\d.*");
    private static final Pattern SPECIAL = Pattern.compile(".*[^A-Za-z0-9].*");

    /** 정책 충족 여부만 필요할 때 */
    public static boolean isValid(String pw) {
        return validate(pw).valid();
    }

    /** 상세 사유까지 받을 때 */
    public static ValidationResult validate(String pw) {
        List<String> reasons = new ArrayList<>();
        if (pw == null || pw.isBlank()) {
            reasons.add("비밀번호를 입력해 주세요.");
            return new ValidationResult(false, reasons);
        }
        if (pw.length() < MIN_LENGTH) {
            reasons.add("비밀번호는 최소 " + MIN_LENGTH + "자 이상이어야 합니다.");
        }
        int cat = 0;
        if (UPPER.matcher(pw).matches())   cat++;
        if (LOWER.matcher(pw).matches())   cat++;
        if (DIGIT.matcher(pw).matches())   cat++;
        if (SPECIAL.matcher(pw).matches()) cat++;
        if (cat < NEED_CATEGORIES) {
            reasons.add("대문자/소문자/숫자/특수문자 중 최소 " + NEED_CATEGORIES + "종 이상을 포함해야 합니다.");
        }
        return new ValidationResult(reasons.isEmpty(), reasons);
    }

    /** 기존(해시)과 동일 여부 */
    public static boolean isSameAsOld(String rawNewPassword, String storedHash, PasswordEncoder encoder) {
        return rawNewPassword != null && storedHash != null && encoder != null
                && encoder.matches(rawNewPassword, storedHash);
    }

    public record ValidationResult(boolean valid, List<String> messages) {
        public String firstMessageOrDefault(String fallback) {
            return (messages != null && !messages.isEmpty()) ? messages.get(0) : fallback;
        }
    }
}