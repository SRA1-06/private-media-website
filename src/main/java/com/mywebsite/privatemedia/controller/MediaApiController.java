package com.mywebsite.privatemedia.controller;

import com.mywebsite.privatemedia.model.Post;
import com.mywebsite.privatemedia.repository.PostRepository;
import com.mywebsite.privatemedia.service.StorageService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class MediaApiController {

  @Value("${MY_SITE_PASSWORD}")
  private String sitePassword;

  private final PostRepository postRepository;
  private final StorageService storageService;

  public MediaApiController(PostRepository postRepository, StorageService storageService) {
    this.postRepository = postRepository;
    this.storageService = storageService;
  }

  // This class is used to receive the password
  public static class PasswordRequest {
    public String password;
  }

  // --- NEW DTO CLASS ---
  // This is a new "Data Transfer Object" we will send to the frontend.
  // It contains the presigned URL, not the private key.
  public static class PostResponse {
    private String mediaUrl; // This will hold the PRESIGNED URL
    private String mediaType;
    private LocalDateTime timestamp;

    // Constructor
    public PostResponse(String mediaUrl, String mediaType, LocalDateTime timestamp) {
      this.mediaUrl = mediaUrl;
      this.mediaType = mediaType;
      this.timestamp = timestamp;
    }

    // Getters
    public String getMediaUrl() { return mediaUrl; }
    public String getMediaType() { return mediaType; }
    public LocalDateTime getTimestamp() { return timestamp; }
  }


  @PostMapping("/authenticate")
  public ResponseEntity<Void> authenticate(@RequestBody PasswordRequest request, HttpSession session) {
    // (This method is unchanged)
    if (sitePassword.equals(request.password)) {
      session.setAttribute("authenticated", true);
      return ResponseEntity.ok().build();
    } else {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
  }

  private boolean isAuthenticated(HttpSession session) {
    // (This method is unchanged)
    Object authFlag = session.getAttribute("authenticated");
    return authFlag != null && (Boolean) authFlag;
  }

  /**
   * Endpoint 2: Get Media (HEAVILY MODIFIED)
   */
  @GetMapping("/media")
  public ResponseEntity<List<PostResponse>> getMedia(HttpSession session) {
    if (!isAuthenticated(session)) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    // 1. Get all Post entities (which contain the private mediaKey)
    List<Post> posts = postRepository.findAllByOrderByTimestampDesc();

    // 2. Convert each Post into a PostResponse DTO
    List<PostResponse> responseList = posts.stream()
        .map(post -> {
          // 3. Generate a presigned URL for each post's media key
          String presignedUrl = storageService.generatePresignedUrl(post.getMediaKey());

          // 4. Create the new response object
          return new PostResponse(
              presignedUrl,
              post.getMediaType(),
              post.getTimestamp()
          );
        })
        .collect(Collectors.toList());

    // 5. Return the list of DTOs
    return ResponseEntity.ok(responseList);
  }

  /**
   * Endpoint 3: Upload Media (MODIFIED)
   */
  @PostMapping("/upload")
  public ResponseEntity<Void> uploadMedia(@RequestParam("file") MultipartFile file, HttpSession session) {
    if (!isAuthenticated(session) || file.isEmpty()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    // 1. Store the file. This now returns the private key (filename).
    String mediaKey = storageService.store(file);

    // 2. Save the key (not a URL) to the database
    Post post = new Post();
    post.setMediaKey(mediaKey); // <-- Use the updated setter
    post.setMediaType(file.getContentType());
    post.setTimestamp(LocalDateTime.now());

    postRepository.save(post);

    return ResponseEntity.ok().build();
  }
}