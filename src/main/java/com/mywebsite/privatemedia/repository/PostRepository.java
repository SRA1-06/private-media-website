package com.mywebsite.privatemedia.repository;


import com.mywebsite.privatemedia.model.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

// This interface gives us all database commands (save, find, delete) for free
public interface PostRepository extends JpaRepository<Post, Long> {

  // This method gives us all posts, sorted by newest first
  List<Post> findAllByOrderByTimestampDesc();
}
