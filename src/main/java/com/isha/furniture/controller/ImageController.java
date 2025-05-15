package com.isha.furniture.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.isha.furniture.model.Product;
import com.isha.furniture.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.*;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;


@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api")
public class ImageController {

    @Autowired
    private ProductService productService;


    private final Path imageFolder = Paths.get("C:\\Users\\gs1-sameerahamedm\\Pictures\\Camera Roll\\uploaded-images\\");

    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadImage(
            @RequestPart("file") MultipartFile file,
            @RequestPart("product") Product product
    ) throws IOException {

        // === Supabase configuration ===
        String SUPABASE_URL = "https://xlkhatuvjkhnohrsziqi.supabase.co";
        String SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Inhsa2hhdHV2amtobm9ocnN6aXFpIiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc0Njc2NjU5OCwiZXhwIjoyMDYyMzQyNTk4fQ.df3WYSuVTzqRuQ04LoYzJX9y-EKz0BXrrbRByqRuspA";
        String BUCKET_NAME = "ishafurniture";

        // Get file extension and generate unique filename
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        String fileExtension = "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex > 0) {
            fileExtension = originalFilename.substring(dotIndex);
        }

        String uniqueFilename = UUID.randomUUID().toString() + fileExtension;
        String objectPath = "uploads/" + uniqueFilename;

        // Upload to Supabase Storage
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.set("apikey", SUPABASE_KEY);
        headers.set("Authorization", "Bearer " + SUPABASE_KEY);

        HttpEntity<byte[]> entity = new HttpEntity<>(file.getBytes(), headers);
        String uploadUrl = SUPABASE_URL + "/storage/v1/object/" + BUCKET_NAME + "/" + objectPath;

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> uploadResponse = restTemplate.exchange(uploadUrl, HttpMethod.POST, entity, String.class);

        if (!uploadResponse.getStatusCode().is2xxSuccessful()) {
            return ResponseEntity.status(uploadResponse.getStatusCode()).body(Map.of("error", "Failed to upload to Supabase"));
        }

        // Construct public URL (assuming public bucket)
        String publicUrl = SUPABASE_URL + "/storage/v1/object/public/" + BUCKET_NAME + "/" + objectPath;

        // Save product info
        Product savedProduct = new Product();
        savedProduct.setName(product.getName());
        savedProduct.setDescription(product.getDescription());
        savedProduct.setPrice(product.getPrice());
        savedProduct.setImageUrl(publicUrl);  // Save Supabase image URL

        productService.saveProduct(savedProduct);

        // Build response
        Map<String, String> response = new HashMap<>();
        response.put("url", publicUrl);
        response.put("originalFilename", originalFilename);
        response.put("storedFilename", uniqueFilename);
        response.put("title", product.getName());
        response.put("description", product.getDescription());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{filename}")
    public ResponseEntity<Resource> getImage(@PathVariable String filename) throws MalformedURLException {
        Path filePath = imageFolder.resolve(filename);
        Resource resource = new UrlResource(filePath.toUri());

        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok().body(resource);
    }

    @GetMapping("/getAllProducts")
    public List<Product> getAllProducts() {
        return productService.getAllProducts();
    }


    @PutMapping(value = "/updateProduct/{id}", consumes = "multipart/form-data")
    public ResponseEntity<Product> updateProduct(
            @PathVariable UUID id,
            @RequestPart("product") String productJson,
            @RequestPart(value = "file", required = false) MultipartFile file) {

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            Product updatedProduct = objectMapper.readValue(productJson, Product.class);

            Product savedProduct = productService.updateProduct(id, updatedProduct, file);
            return ResponseEntity.ok(savedProduct);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }



    @DeleteMapping("/deleteProduct/{id}")
    public ResponseEntity<String> deleteProduct(@PathVariable UUID id) {
        try {
            productService.deleteProduct(id);
            return ResponseEntity.ok("Product and associated image deleted successfully.");
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Product not found");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error deleting product");
        }
    }

}
