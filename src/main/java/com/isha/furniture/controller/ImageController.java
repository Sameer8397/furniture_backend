package com.isha.furniture.controller;

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
        String fileName = StringUtils.cleanPath(file.getOriginalFilename());
        System.out.println("Received file: " + file.getOriginalFilename());
        System.out.println("Received product1: " + product.getName());
        File uploadPath = new File(uploadDir);
        if (!uploadPath.exists()) {
            uploadPath.mkdirs();
        }

        Path filePath = Paths.get(uploadDir, fileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // You can now use product1.getTitle(), product1.getDescription()
        System.out.println("Image metadata received: Title = " + product.getName() + ", Description = " + product.getDescription());

        Map<String, String> response = new HashMap<>();
        response.put("url", "/api/images/" + fileName);
        response.put("title", product.getDescription());
        response.put("description", product.getDescription());

        Product product1 = new Product();
        product1.setDescription(product.getDescription());
        product1.setImageUrl("http://localhost:8080/api/"+fileName);
        product1.setName(product.getName());
        product1.setPrice(product.getPrice());


        productService.saveProduct(product1);
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
}
