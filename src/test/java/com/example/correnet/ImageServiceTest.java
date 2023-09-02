package com.example.correnet;

import com.example.correnet.service.ImageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.DetectLabelsRequest;
import software.amazon.awssdk.services.rekognition.model.DetectLabelsResponse;
import software.amazon.awssdk.services.rekognition.model.Label;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class ImageServiceTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private RekognitionClient rekognitionClient;

    @InjectMocks
    private ImageService imageService;

    private MultipartFile mockFile;

    @BeforeEach
    void setUp() throws IOException {
        // Initialize the mocks
        MockitoAnnotations.openMocks(this);
        
        mockFile = mock(MultipartFile.class);
        InputStream mockInputStream = mock(InputStream.class);

        ReflectionTestUtils.setField(imageService, "bucketName", "correnetbucket");

        when(mockFile.getOriginalFilename()).thenReturn("ball.jpeg");
        when(mockFile.getSize()).thenReturn(1024L);
        when(mockFile.getInputStream()).thenReturn(mockInputStream);
    }

    @Test
    void testUploadAndAnalyzeImage() throws IOException {
        // Create some mock responses
        DetectLabelsResponse mockResponse = DetectLabelsResponse.builder()
                .labels(Label.builder().name("label1").confidence(90.0f).build())
                .build();

        when(rekognitionClient.detectLabels(any(DetectLabelsRequest.class))).thenReturn(mockResponse);

        // Run the method under test
        imageService.uploadAndAnalyzeImage(mockFile);

        // Verify interactions
        verify(s3Client, times(2)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        verify(rekognitionClient).detectLabels(any(DetectLabelsRequest.class));
    }

    @Test
    void testListObjectUrls() {
        // Mock S3 list objects response
        ListObjectsV2Response mockListObjectsResponse = ListObjectsV2Response.builder()
                .contents(S3Object.builder().key("ball.jpeg").build(),
                        S3Object.builder().key("ball2.jpeg").build())
                .build();
        
        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(mockListObjectsResponse);

        // Run the method under test
        List<String> urls = imageService.listObjectUrls();

        // Assertions
        assertEquals(2, urls.size());
        assertTrue(urls.contains("https://correnetbucket.s3.amazonaws.com/ball.jpeg"));
        assertTrue(urls.contains("https://correnetbucket.s3.amazonaws.com/ball2.jpeg"));
    }

    @Test
    void testSearchImagesByLabel() {
        // Mock S3 list objects and head object responses
        ListObjectsV2Response mockListObjectsResponse = ListObjectsV2Response.builder()
                .contents(S3Object.builder().key("ball.jpeg").build())
                .build();
        
        Map<String, String> metadata = new HashMap<>();
        metadata.put("label-ball", "90.0");

        HeadObjectResponse mockHeadObjectResponse = HeadObjectResponse.builder()
                .metadata(metadata)
                .build();

        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(mockListObjectsResponse);
        when(s3Client.headObject(any(HeadObjectRequest.class))).thenReturn(mockHeadObjectResponse);

        // Run the method under test
        List<String> urls = imageService.searchImagesUrlByLabel("ball");

        System.out.println(urls);

        // Assertions
        assertEquals(1, urls.size());
        assertTrue(urls.contains("https://correnetbucket.s3.amazonaws.com/ball.jpeg"));
    }
}
