package com.isha.furniture.service;

import com.isha.furniture.model.Product;
import com.isha.furniture.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    String SUPABASE_URL = "https://xlkhatuvjkhnohrsziqi.supabase.co";
    String SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Inhsa2hhdHV2amtobm9ocnN6aXFpIiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc0Njc2NjU5OCwiZXhwIjoyMDYyMzQyNTk4fQ.df3WYSuVTzqRuQ04LoYzJX9y-EKz0BXrrbRByqRuspA";
    String BUCKET_NAME = "ishafurniture";


    public Product saveProduct(Product product) {
        return productRepository.save(product);
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }



    public Product updateProduct(UUID id, Product updatedProduct, MultipartFile file) {

        return productRepository.findById(id).map(existingProduct -> {
            existingProduct.setName(updatedProduct.getName());
            existingProduct.setDescription(updatedProduct.getDescription());
            existingProduct.setPrice(updatedProduct.getPrice());

            if (file != null && !file.isEmpty()) {
                try {
                    // Generate unique filename
                    String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
                    String fileExtension = originalFilename.substring(originalFilename.lastIndexOf('.'));
                    String uniqueFileName = UUID.randomUUID().toString() + fileExtension;
                    String objectPath = "uploads/" + uniqueFileName;

                    // === Upload new image to Supabase ===
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
                    headers.set("apikey", SUPABASE_KEY);
                    headers.set("Authorization", "Bearer " + SUPABASE_KEY);

                    HttpEntity<byte[]> entity = new HttpEntity<>(file.getBytes(), headers);
                    RestTemplate restTemplate = new RestTemplate();
                    String uploadUrl = SUPABASE_URL + "/storage/v1/object/" + BUCKET_NAME + "/" + objectPath;

                    ResponseEntity<String> response = restTemplate.exchange(uploadUrl, HttpMethod.POST, entity, String.class);
                    if (!response.getStatusCode().is2xxSuccessful()) {
                        throw new RuntimeException("Image upload to Supabase failed");
                    }

                    // === Delete old image from Supabase if applicable ===
                    String oldImageUrl = existingProduct.getImageUrl();
                    if (oldImageUrl != null && oldImageUrl.contains("/storage/v1/object/public/" + BUCKET_NAME + "/")) {
                        String oldObjectPath = oldImageUrl.substring(oldImageUrl.indexOf(BUCKET_NAME) + BUCKET_NAME.length() + 1);
                        String deleteUrl = SUPABASE_URL + "/storage/v1/object/" + BUCKET_NAME + "/" + oldObjectPath;

                        HttpEntity<String> deleteEntity = new HttpEntity<>(headers);
                        restTemplate.exchange(deleteUrl, HttpMethod.DELETE, deleteEntity, String.class);
                    }

                    // Set new image URL
                    String publicUrl = SUPABASE_URL + "/storage/v1/object/public/" + BUCKET_NAME + "/" + objectPath;
                    existingProduct.setImageUrl(publicUrl);

                } catch (IOException e) {
                    throw new RuntimeException("Failed to process image file", e);
                }
            }

            return productRepository.save(existingProduct);
        }).orElseThrow(() -> new RuntimeException("Product not found with ID: " + id));
    }


    public void deleteProduct(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Product not found"));

        String imageUrl = product.getImageUrl();

        // === Delete image from Supabase ===
        if (imageUrl != null && imageUrl.contains("/storage/v1/object/public/")) {

            // Extract object path from URL
            String objectPath = imageUrl.substring(imageUrl.indexOf(BUCKET_NAME) + BUCKET_NAME.length() + 1);

            HttpHeaders headers = new HttpHeaders();
            headers.set("apikey", SUPABASE_KEY);
            headers.set("Authorization", "Bearer " + SUPABASE_KEY);

            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
            RestTemplate restTemplate = new RestTemplate();
            String deleteUrl = SUPABASE_URL + "/storage/v1/object/" + BUCKET_NAME + "/" + objectPath;

            try {
                restTemplate.exchange(deleteUrl, HttpMethod.DELETE, requestEntity, String.class);
            } catch (Exception e) {
                System.err.println("Failed to delete image from Supabase: " + e.getMessage());
            }
        }
        productRepository.deleteById(id);
    }


}
