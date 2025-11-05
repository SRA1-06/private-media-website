package com.mywebsite.privatemedia.service;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URL;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class StorageService {

  @Autowired
  private AmazonS3 s3Client;

  @Value("${aws.s3.bucket-name}")
  private String bucketName;

  /**
   * Stores the file in S3 and returns the private key (filename).
   */
  public String store(MultipartFile file) {
    String filename = UUID.randomUUID() + "-" + file.getOriginalFilename();

    try {
      ObjectMetadata metadata = new ObjectMetadata();
      metadata.setContentLength(file.getSize());
      metadata.setContentType(file.getContentType());
      PutObjectRequest putObjectRequest = new PutObjectRequest(
          bucketName,
          filename,
          file.getInputStream(),
          metadata
      );

      // --- CHANGE 1: REMOVE PUBLIC ACL ---
      // DO NOT add .withCannedAcl(CannedAccessControlList.PublicRead)
      // The file will be private by default.

      s3Client.putObject(putObjectRequest);

      // --- CHANGE 2: RETURN THE KEY, NOT A URL ---
      return filename; // e.g., "a1b2c3d4-my-video.mp4"

    } catch (IOException e) {
      throw new RuntimeException("Failed to store file in S3.", e);
    }
  }

  /**
   * --- NEW METHOD ---
   * Generates a temporary, 15-minute presigned URL for a private S3 object.
   */
  public String generatePresignedUrl(String mediaKey) {
    if (mediaKey == null || mediaKey.isEmpty()) {
      return null;
    }

    // Set the expiration time (e.g., 15 minutes from now)
    Date expiration = new Date();
    long expTimeMillis = Instant.now().toEpochMilli();
    expTimeMillis += 1000 * 60 * 15; // 15 minutes
    expiration.setTime(expTimeMillis);

    // Generate the presigned URL
    GeneratePresignedUrlRequest generatePresignedUrlRequest =
        new GeneratePresignedUrlRequest(bucketName, mediaKey)
            .withMethod(HttpMethod.GET)
            .withExpiration(expiration);

    URL url = s3Client.generatePresignedUrl(generatePresignedUrlRequest);
    return url.toString();
  }
}