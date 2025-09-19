package kd.paperless.config.converter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.HashMap;
import java.util.Map;

/**
 * Map<String,Object> ↔ JSON(String) 변환 컨버터
 * - DB엔 CLOB 컬럼(예: extra_json)에 JSON 문자열로 저장
 * - null/빈 맵은 "{}"로 저장하여 NOT NULL + IS JSON 제약을 만족
 */
@Converter(autoApply = false)
public class JsonMapConverter implements AttributeConverter<Map<String, Object>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private static final TypeReference<Map<String, Object>> MAP_TYPE =
            new TypeReference<Map<String, Object>>() {};

    @Override
    public String convertToDatabaseColumn(Map<String, Object> attribute) {
        try {
            if (attribute == null || attribute.isEmpty()) {
                return "{}";
            }
            return MAPPER.writeValueAsString(attribute);
        } catch (Exception e) {
            // 엔티티 저장 시 직렬화 실패 원인을 바로 확인할 수 있도록 예외 래핑
            throw new IllegalArgumentException("JSON serialize 실패: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, Object> convertToEntityAttribute(String dbData) {
        try {
            if (dbData == null || dbData.isBlank()) {
                return new HashMap<>();
            }
            return MAPPER.readValue(dbData, MAP_TYPE);
        } catch (Exception e) {
            // 손상된 JSON이 DB에 들어있을 때 역직렬화 실패
            throw new IllegalArgumentException("JSON deserialize 실패: " + e.getMessage(), e);
        }
    }
}
