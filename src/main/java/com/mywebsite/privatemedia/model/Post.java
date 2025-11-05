package com.mywebsite.privatemedia.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class Post {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  // --- CHANGE THIS ---
  // Rename 'mediaUrl' to 'mediaKey'. This will store the
  // private filename in S3 (e.g., "a1b2c3d4-my-video.mp4")
  private String mediaKey;

  private String mediaType;
  private LocalDateTime timestamp;

  // --- UPDATE GETTERS/SETTERS ---
  public Long getId() {
    return id;
  }
  public void setId(Long id) {
    this.id = id;
  }

  // Updated getter
  public String getMediaKey() {
    return mediaKey;
  }
  // Updated setter
  public void setMediaKey(String mediaKey) {
    this.mediaKey = mediaKey;
  }

  public String getMediaType() {
    return mediaType;
  }
  public void setMediaType(String mediaType) {
    this.mediaType = mediaType;
  }
  public LocalDateTime getTimestamp() {
    return timestamp;
  }
  public void setTimestamp(LocalDateTime timestamp) {
    this.timestamp = timestamp;
  }
}