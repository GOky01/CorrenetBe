package com.example.correnet.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.S3Object;
import software.amazon.awssdk.services.rekognition.model.*;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ImageService {

    @Autowired
    private S3Client s3Client;

    @Autowired
    private RekognitionClient rekognitionClient;

    @Value("${aws.bucketName}")
    private String bucketName;
    public void uploadAndAnalyzeImage(MultipartFile file) throws IOException {
        String key = file.getOriginalFilename();

        // Upload to S3 initially
        s3Client.putObject(PutObjectRequest.builder().bucket(bucketName).key(key)
                .build(), RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

        // Rekognition Analysis
        DetectLabelsRequest detectLabelsRequest = DetectLabelsRequest.builder()
                .image(Image.builder().s3Object(S3Object.builder().bucket(bucketName).name(key).build()).build())
                .maxLabels(10)
                .build();
        DetectLabelsResponse response = rekognitionClient.detectLabels(detectLabelsRequest);

        // Collect labels and confidence scores
        Map<String, String> metadata = new HashMap<>();
        for (Label label : response.labels()) {
            metadata.put("label-" + label.name(), String.valueOf(label.confidence()));
        }

        // Re-upload the image to S3 with added metadata
        s3Client.putObject(PutObjectRequest.builder().bucket(bucketName).key(key)
                .metadata(metadata)
                .build(), RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
    }
    public List<String> listObjectUrls() {
        ListObjectsV2Request listObjectsReqManual = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .maxKeys(10)
                .build();

        ListObjectsV2Response listObjResponse = s3Client.listObjectsV2(listObjectsReqManual);
        List<String> urls = new ArrayList<>();

        for (software.amazon.awssdk.services.s3.model.S3Object content : listObjResponse.contents()) {
            String key = content.key();
            String url = "https://" + bucketName + ".s3.amazonaws.com/" + key;
            urls.add(url);
        }

        return urls;
    }

    public List<String> searchImagesUrlByLabel(String searchLabel) {
        ListObjectsV2Request listObjectsReq = ListObjectsV2Request.builder().bucket(bucketName).build();
        ListObjectsV2Response listObjResponse = s3Client.listObjectsV2(listObjectsReq);
        List<String> urls = new ArrayList<>();

        for (software.amazon.awssdk.services.s3.model.S3Object content : listObjResponse.contents()) {
            // Retrieve metadata for each object
            HeadObjectResponse headObjectResponse = s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(content.key())
                    .build());

            Map<String, String> metadata = headObjectResponse.metadata();

            // Check if metadata contains the search label
            if (metadata.keySet().stream().anyMatch(key -> key.toLowerCase().contains(searchLabel.toLowerCase()))) {
                String url = "https://" + bucketName + ".s3.amazonaws.com/" + content.key();
                urls.add(url);
            }
        }

        return urls;
    }

}
