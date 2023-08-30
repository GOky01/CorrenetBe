package com.example.correnet.controller;

import com.example.correnet.service.ImageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.S3Client;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/images")
public class ImageController {

    @Autowired
    private ImageService imageService;

    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> upload(@RequestParam("file") MultipartFile file) throws IOException {
        imageService.uploadAndAnalyzeImage(file);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Image uploaded and analyzed successfully.");
        return ResponseEntity.ok(response);
    }
    @GetMapping("/urls")
    public List<String> getImageUrls() {
        return imageService.listObjectUrls();
    }
    @GetMapping("/search")
    public List<String> searchImages(@RequestParam String query) {
        return imageService.searchImagesByLabel(query);
    }
}
