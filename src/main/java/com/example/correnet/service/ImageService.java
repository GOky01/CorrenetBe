package com.example.correnet.service;

import com.example.correnet.repo.ImageRepository;
import com.example.correnet.entity.ImageEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.*;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ImageService {

    @Autowired
    private S3Client s3Client;

    @Autowired
    private RekognitionClient rekognitionClient;

    @Autowired
    private ImageRepository imageRepository;

    @Value("${aws.bucketName}")
    private String bucketName;
    public void uploadAndAnalyzeImage(MultipartFile file) throws IOException {
        // Upload to S3
        s3Client.putObject(PutObjectRequest.builder().bucket(bucketName).key(file.getOriginalFilename())
                .build(), RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

        String key = file.getOriginalFilename();
        String url = "https://" + bucketName + ".s3.amazonaws.com/" + key;

        // Analyze using Rekognition
        DetectLabelsRequest detectLabelsRequest = DetectLabelsRequest.builder()
                .image(Image.builder().s3Object(S3Object.builder().bucket(bucketName).name(file.getOriginalFilename()).build()).build())
                .maxLabels(10)
                .build();

        DetectLabelsResponse response = rekognitionClient.detectLabels(detectLabelsRequest);

        List<String> detectedLabels = new ArrayList<>();

        for (Label label : response.labels()) {
            System.out.println(label.name() + ": " + label.confidence());
            detectedLabels.add(label.name());

        }
        ImageEntity imageEntity = new ImageEntity();
        imageEntity.setUrl(url);
        imageEntity.setLabels(detectedLabels);
        imageRepository.save(imageEntity);
    }
    public List<String> listObjectUrls() {
        List<ImageEntity> allImages = imageRepository.findAll();
        return allImages.stream().map(ImageEntity::getUrl).collect(Collectors.toList());
    }

    public List<String> searchImagesByLabel(String query) {
        List<ImageEntity> filteredImages = imageRepository.findByLabelsContaining(query);
        return filteredImages.stream().map(ImageEntity::getUrl).collect(Collectors.toList());
    }
}
