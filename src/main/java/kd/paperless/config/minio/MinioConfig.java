package kd.paperless.config.minio;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioConfig {

  @Value("${storage.minio.endpoint}")
  private String endpoint;

  @Value("${storage.minio.access-key}")
  private String accessKey;

  @Value("${storage.minio.secret-key}")
  private String secretKey;

  @Value("${storage.minio.bucket}")
  private String bucket;

  @Bean
  public MinioClient minioClient() {
    return MinioClient.builder()
        .endpoint(endpoint)
        .credentials(accessKey, secretKey)
        .build();
  }

  /** 애플리케이션 시작 시 버킷 존재 확인 */
  @Bean
  public SmartInitializingSingleton minioBucketInitializer(MinioClient minioClient) {
    return () -> {
      try {
        boolean exists = minioClient.bucketExists(
            BucketExistsArgs.builder().bucket(bucket).build());
        if (!exists) {
          minioClient.makeBucket(
              MakeBucketArgs.builder().bucket(bucket).build());
        }
      } catch (Exception e) {
        throw new RuntimeException("MinIO 버킷 초기화 실패: " + bucket, e);
      }
    };
  }
}