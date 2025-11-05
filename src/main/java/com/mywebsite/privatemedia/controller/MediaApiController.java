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
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class MediaApiController {

  // --- Injected Passwords (from application.properties) ---
  @Value("${ADMIN_PASSWORD}")
  private String adminPassword;

  @Value("${USER_PASSWORD}")
  private String userPassword;

  // --- Injected Services ---
  private final PostRepository postRepository;
  private final StorageService storageService;

  public MediaApiController(PostRepository postRepository, StorageService storageService) {
    this.postRepository = postRepository;
    this.storageService = storageService;
  }

  // --- Helper DTO Classes (inner classes) ---

  // Receives password from frontend
  public static class PasswordRequest {
    public String password;
  }

  // Holds post data to send to frontend
  public static class PostResponse {
    private Long id;
    private String mediaUrl;
    private String mediaType;
    private LocalDateTime timestamp;

    public PostResponse(Long id, String mediaUrl, String mediaType, LocalDateTime timestamp) {
      this.id = id;
      this.mediaUrl = mediaUrl;
      this.mediaType = mediaType;
      this.timestamp = timestamp;
    }

    // Getters are needed for JSON serialization
    public Long getId() { return id; }
    public String getMediaUrl() { return mediaUrl; }
    public String getMediaType() { return mediaType; }
    public LocalDateTime getTimestamp() { return timestamp; }
  }

  // Holds the complete page data (posts + role)
  public static class MediaPageResponse {
    private List<PostResponse> posts;
    private String userRole;

    public MediaPageResponse(List<PostResponse> posts, String userRole) {
      this.posts = posts;
      this.userRole = userRole;
    }

    // Getters
    public List<PostResponse> getPosts() { return posts; }
    public String getUserRole() { return userRole; }
  }

  // --- Security Helper Method ---

  /**
   * Helper method to get the role from the session.
   * Returns "ADMIN", "USER", or null.
   */
  private String getSessionRole(HttpSession session) {
    Object role = session.getAttribute("role");
    if (role == null) {
      return null;
    }
    return (String) role;
  }

  // --- API ENDPOINTS ---

  /**
   * Endpoint 1: Authenticate
   * Checks password and returns the user's role.
   */
  @PostMapping("/authenticate")
  public ResponseEntity<Map<String, String>> authenticate(@RequestBody PasswordRequest request, HttpSession session) {
    String sessionRole = null;

    if (adminPassword.equals(request.password)) {
      sessionRole = "ADMIN";
    } else if (userPassword.equals(request.password)) {
      sessionRole = "USER";
    }

    if (sessionRole != null) {
      session.setAttribute("role", sessionRole);
      return ResponseEntity.ok(Map.of("role", sessionRole));
    } else {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
  }

  /**
   * Endpoint 2: Get Media
   * Returns all media AND the user's role. (Used for auto-login)
   */
  @GetMapping("/media")
  public ResponseEntity<MediaPageResponse> getMedia(HttpSession session) {
    String role = getSessionRole(session);
    if (role == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    List<Post> posts = postRepository.findAllByOrderByTimestampDesc();
    List<PostResponse> responseList = posts.stream()
        .map(post -> {
          String presignedUrl = storageService.generatePresignedUrl(post.getMediaKey());
          return new PostResponse(
              post.getId(),
              presignedUrl,
              post.getMediaType(),
              post.getTimestamp()
          );
        })
        .collect(Collectors.toList());

    MediaPageResponse pageResponse = new MediaPageResponse(responseList, role);
    return ResponseEntity.ok(pageResponse);
  }

  /**
   * Endpoint 3: Upload Media
   * Both ADMIN and USER can upload.
   */
  @PostMapping("/upload")
  public ResponseEntity<Void> uploadMedia(@RequestParam("file") MultipartFile file, HttpSession session) {
    if (getSessionRole(session) == null || file.isEmpty()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    String mediaKey = storageService.store(file);

    Post post = new Post();
    post.setMediaKey(mediaKey);
    post.setMediaType(file.getContentType());
    post.setTimestamp(LocalDateTime.now());

    postRepository.save(post);

    return ResponseEntity.ok().build();
  }

  /**
   * Endpoint 4: Delete Media
   * ONLY ADMIN can delete.
   */
  @DeleteMapping("/media/{id}")
  public ResponseEntity<Void> deleteMedia(@PathVariable Long id, HttpSession session) {
    if (!"ADMIN".equals(getSessionRole(session))) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build(); // 403 Forbidden
    }

    Optional<Post> postOptional = postRepository.findById(id);
    if (postOptional.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    Post post = postOptional.get();
    storageService.delete(post.getMediaKey());
    postRepository.delete(post);

    return ResponseEntity.ok().build();
  }

  /**
   * --- NEW ENDPOINT 5: LOGOUT ---
   * Destroys the server session.
   */
  @PostMapping("/logout")
  public ResponseEntity<Void> logout(HttpSession session) {
    session.invalidate(); // Destroys the session
    return ResponseEntity.ok().build();
  }
}