package com.isha.furniture.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.isha.furniture.model.Product;
import com.isha.furniture.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
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

        String uploadDir = "C:\\Users\\gs1-sameerahamedm\\Pictures\\Camera Roll\\uploaded-images\\";

        // Get original file extension
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        String fileExtension = "";

        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex > 0) {
            fileExtension = originalFilename.substring(dotIndex);
        }

        // Generate a unique filename
        String uniqueFilename = UUID.randomUUID().toString() + fileExtension;
        Path filePath = Paths.get(uploadDir, uniqueFilename);

        // Ensure the directory exists
        File uploadPath = new File(uploadDir);
        if (!uploadPath.exists()) {
            uploadPath.mkdirs();
        }

        // Save the file
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // Log info
        System.out.println("Received file: " + originalFilename);
        System.out.println("Stored as: " + uniqueFilename);
        System.out.println("Received product: " + product.getName());

        // Save to DB with unique file name
        Product savedProduct = new Product();
        savedProduct.setName(product.getName());
        savedProduct.setDescription(product.getDescription());
        savedProduct.setPrice(product.getPrice());
        savedProduct.setImageUrl("http://localhost:8080/api/" + uniqueFilename); // or your actual image URL path

        productService.saveProduct(savedProduct);

        // Build response
        Map<String, String> response = new HashMap<>();
        response.put("url", "/api/" + uniqueFilename);
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

}
