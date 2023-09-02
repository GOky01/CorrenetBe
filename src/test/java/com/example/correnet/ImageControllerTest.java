package com.example.correnet;

import com.example.correnet.controller.ImageController;
import com.example.correnet.service.ImageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class ImageControllerTest {

    @InjectMocks
    private ImageController imageController;

    @Mock
    private ImageService imageService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void upload() throws IOException {
        MultipartFile file = new MockMultipartFile("file", "original", null, "filecontent".getBytes());
        doNothing().when(imageService).uploadAndAnalyzeImage(file);
        
        ResponseEntity<Map<String, String>> response = imageController.upload(file);
        
        assertEquals("Image uploaded and analyzed successfully.", response.getBody().get("message"));
    }

    @Test
    void getImageUrls() {
        List<String> urls = Arrays.asList("url1", "url2", "url3");
        when(imageService.listObjectUrls()).thenReturn(urls);
        
        List<String> response = imageController.getImageUrls();
        
        assertEquals(urls, response);
    }

    @Test
    void searchImages() {
        List<String> urls = Arrays.asList("url1", "url2", "url3");
        when(imageService.searchImagesUrlByLabel("query")).thenReturn(urls);

        List<String> response = imageController.searchImages("query");

        assertEquals(urls, response);
    }
}
