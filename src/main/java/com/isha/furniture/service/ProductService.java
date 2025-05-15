package com.isha.furniture.service;

import com.isha.furniture.model.Product;
import com.isha.furniture.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

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
                    // Extract file extension
                    String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
                    String fileExtension = originalFilename.substring(originalFilename.lastIndexOf('.'));
                    String uniqueFileName = UUID.randomUUID().toString() + fileExtension;

                    // Define upload directory
                    String uploadDir = "C:\\Users\\gs1-sameerahamedm\\Pictures\\Camera Roll\\uploaded-images\\";
                    Path uploadPath = Paths.get(uploadDir);

                    if (!Files.exists(uploadPath)) {
                        Files.createDirectories(uploadPath);
                    }

                    // Save new image file
                    Path filePath = uploadPath.resolve(uniqueFileName);
                    Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

                    // (Optional) Delete old file
                    String oldImageUrl = existingProduct.getImageUrl();
                    if (oldImageUrl != null && oldImageUrl.startsWith("http://localhost:8080/api/")) {
                        String oldFileName = oldImageUrl.substring(oldImageUrl.lastIndexOf('/') + 1);
                        Path oldFilePath = uploadPath.resolve(oldFileName);
                        Files.deleteIfExists(oldFilePath);
                    }

                    // Set new image URL
                    existingProduct.setImageUrl("http://localhost:8080/api/" + uniqueFileName);

                } catch (IOException e) {
                    throw new RuntimeException("Failed to process image file", e);
                }
            }

            return productRepository.save(existingProduct);
        }).orElseThrow(() -> new RuntimeException("Product not found with ID: " + id));
    }

}
