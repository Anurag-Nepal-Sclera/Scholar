//package com.scholar.service.storage;
//
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Service;
//import org.springframework.web.multipart.MultipartFile;
//
//import jakarta.annotation.PostConstruct;
//import java.io.IOException;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.nio.file.StandardCopyOption;
//import java.util.Arrays;
//import java.util.Set;
//import java.util.UUID;
//import java.util.stream.Collectors;
//
///**
// * Service for handling file storage operations.
// * Manages secure file uploads, retrievals, and deletions.
// */
//@Service
//@Slf4j
//public class FileStorageService {
//
//    @Value("${scholar.storage.base-path:./uploads}")
//    private String basePath;
//
//    private Path storageLocation;
//
//    @PostConstruct
//    public void init() {
//        try {
//            this.storageLocation = Paths.get(basePath).toAbsolutePath().normalize();
//            Files.createDirectories(this.storageLocation);
//            log.info("File storage initialized at: {}", this.storageLocation);
//        } catch (IOException e) {
//            log.error("Could not create storage directory", e);
//            throw new RuntimeException("Could not create storage directory", e);
//        }
//    }
//
//    /**
//     * Stores a file for a specific tenant.
//     *
//     * @param file the file to store
//     * @param tenantId the tenant identifier
//     * @return the relative path where the file was stored
//     */
//    public String storeFile(MultipartFile file, UUID tenantId) {
//        try {
//            // Create tenant-specific directory
//            Path tenantDir = this.storageLocation.resolve(tenantId.toString());
//            Files.createDirectories(tenantDir);
//
//            // Generate unique filename
//            String originalFilename = file.getOriginalFilename();
//            String extension = getFileExtension(originalFilename);
//            String storedFilename = UUID.randomUUID().toString() + extension;
//
//            // Store file
//            Path targetLocation = tenantDir.resolve(storedFilename);
//            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
//
//            String relativePath = tenantId.toString() + "/" + storedFilename;
//            log.info("File stored successfully: {}", relativePath);
//
//            return relativePath;
//        } catch (IOException e) {
//            log.error("Failed to store file: {}", file.getOriginalFilename(), e);
//            throw new RuntimeException("Failed to store file", e);
//        }
//    }
//
//    /**
//     * Retrieves a file as byte array.
//     *
//     * @param filePath the relative path to the file
//     * @return the file contents as byte array
//     */
//    public byte[] retrieveFile(String filePath) {
//        try {
//            Path file = this.storageLocation.resolve(filePath).normalize();
//
//            // Security check: ensure file is within storage location
//            if (!file.startsWith(this.storageLocation)) {
//                throw new SecurityException("Cannot access file outside storage directory");
//            }
//
//            if (!Files.exists(file)) {
//                throw new RuntimeException("File not found: " + filePath);
//            }
//
//            return Files.readAllBytes(file);
//        } catch (IOException e) {
//            log.error("Failed to retrieve file: {}", filePath, e);
//            throw new RuntimeException("Failed to retrieve file", e);
//        }
//    }
//
//    /**
//     * Deletes a file from storage.
//     *
//     * @param filePath the relative path to the file
//     */
//    public void deleteFile(String filePath) {
//        try {
//            Path file = this.storageLocation.resolve(filePath).normalize();
//
//            // Security check: ensure file is within storage location
//            if (!file.startsWith(this.storageLocation)) {
//                throw new SecurityException("Cannot delete file outside storage directory");
//            }
//
//            if (Files.exists(file)) {
//                Files.delete(file);
//                log.info("File deleted successfully: {}", filePath);
//            } else {
//                log.warn("File not found for deletion: {}", filePath);
//            }
//        } catch (IOException e) {
//            log.error("Failed to delete file: {}", filePath, e);
//            throw new RuntimeException("Failed to delete file", e);
//        }
//    }
//
//    /**
//     * Validates if the file type is allowed.
//     *
//     * @param file the file to validate
//     * @param allowedTypes comma-separated list of allowed MIME types or extensions
//     * @return true if file type is valid
//     */
//    public boolean isValidFileType(MultipartFile file, String allowedTypes) {
//        if (file == null || allowedTypes == null || allowedTypes.isEmpty()) {
//            return false;
//        }
//
//        Set<String> allowed = Arrays.stream(allowedTypes.split(","))
//                .map(String::trim)
//                .map(String::toLowerCase)
//                .collect(Collectors.toSet());
//
//        // Check MIME type
//        String contentType = file.getContentType();
//        if (contentType != null && allowed.contains(contentType.toLowerCase())) {
//            return true;
//        }
//
//        // Check file extension
//        String originalFilename = file.getOriginalFilename();
//        if (originalFilename != null) {
//            String extension = getFileExtension(originalFilename).toLowerCase();
//            // Remove the dot for comparison
//            String extWithoutDot = extension.startsWith(".") ? extension.substring(1) : extension;
//            if (allowed.contains(extWithoutDot) || allowed.contains("." + extWithoutDot)) {
//                return true;
//            }
//        }
//
//        return false;
//    }
//
//    /**
//     * Gets the file extension from a filename.
//     *
//     * @param filename the filename
//     * @return the extension including the dot, or empty string
//     */
//    private String getFileExtension(String filename) {
//        if (filename == null || filename.isEmpty()) {
//            return "";
//        }
//        int lastDotIndex = filename.lastIndexOf('.');
//        if (lastDotIndex == -1) {
//            return "";
//        }
//        return filename.substring(lastDotIndex);
//    }
//
//    /**
//     * Gets the full path to a stored file.
//     *
//     * @param filePath the relative path
//     * @return the absolute Path object
//     */
//    public Path getFilePath(String filePath) {
//        return this.storageLocation.resolve(filePath).normalize();
//    }
//}
package com.scholar.service.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for handling file storage operations.
 * Manages secure file uploads, retrievals, and deletions.
 */
@Service
@Slf4j
public class FileStorageService {

    private final String basePath;
    private Path storageLocation;

    public FileStorageService(@Value("${scholar.storage.base-path:./uploads}") String basePath) {
        this.basePath = basePath;
    }

    @PostConstruct
    void init() {
        try {
            this.storageLocation = Paths.get(basePath).toAbsolutePath().normalize();
            Files.createDirectories(this.storageLocation);
            log.info("File storage initialized at: {}", this.storageLocation);
        } catch (IOException e) {
            log.error("Could not create storage directory", e);
            throw new RuntimeException("Could not create storage directory", e);
        }
    }

    /**
     * Stores a file for a specific tenant.
     *
     * @param file the file to store
     * @param tenantId the tenant identifier
     * @return the relative path where the file was stored
     */
    public String storeFile(MultipartFile file, UUID tenantId) {
        try {
            // Create tenant-specific directory
            String tenantIdStr = tenantId.toString();
            Path tenantDir = this.storageLocation.resolve(tenantIdStr);
            Files.createDirectories(tenantDir);

            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String extension = getFileExtension(originalFilename);
            String storedFilename = UUID.randomUUID() + extension;

            // Store file
            Path targetLocation = tenantDir.resolve(storedFilename);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            String relativePath = tenantIdStr + "/" + storedFilename;
            log.info("File stored successfully: {}", relativePath);

            return relativePath;
        } catch (IOException e) {
            log.error("Failed to store file: {}", file.getOriginalFilename(), e);
            throw new RuntimeException("Failed to store file", e);
        }
    }

    /**
     * Retrieves a file as byte array.
     *
     * @param filePath the relative path to the file
     * @return the file contents as byte array
     */
    public byte[] retrieveFile(String filePath) {
        try {
            Path file = this.storageLocation.resolve(filePath).normalize();

            // Security check: ensure file is within storage location
            if (!file.startsWith(this.storageLocation)) {
                throw new SecurityException("Cannot access file outside storage directory");
            }

            if (!Files.exists(file)) {
                throw new RuntimeException("File not found: " + filePath);
            }

            return Files.readAllBytes(file);
        } catch (IOException e) {
            log.error("Failed to retrieve file: {}", filePath, e);
            throw new RuntimeException("Failed to retrieve file", e);
        }
    }

    /**
     * Deletes a file from storage.
     *
     * @param filePath the relative path to the file
     */
    public void deleteFile(String filePath) {
        try {
            Path file = this.storageLocation.resolve(filePath).normalize();

            // Security check: ensure file is within storage location
            if (!file.startsWith(this.storageLocation)) {
                throw new SecurityException("Cannot delete file outside storage directory");
            }

            if (Files.deleteIfExists(file)) {
                log.info("File deleted successfully: {}", filePath);
            } else {
                log.warn("File not found for deletion: {}", filePath);
            }
        } catch (IOException e) {
            log.error("Failed to delete file: {}", filePath, e);
            throw new RuntimeException("Failed to delete file", e);
        }
    }

    /**
     * Validates if the file type is allowed.
     *
     * @param file the file to validate
     * @param allowedTypes comma-separated list of allowed MIME types or extensions
     * @return true if file type is valid
     */
    public boolean isValidFileType(MultipartFile file, String allowedTypes) {
        if (file == null || allowedTypes == null || allowedTypes.isEmpty()) {
            return false;
        }

        Set<String> allowed = Arrays.stream(allowedTypes.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        // Check MIME type
        String contentType = file.getContentType();
        if (contentType != null && allowed.contains(contentType.toLowerCase())) {
            return true;
        }

        // Check file extension
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            return false;
        }

        String extension = getFileExtension(originalFilename).toLowerCase();
        String extWithoutDot = extension.startsWith(".") ? extension.substring(1) : extension;

        return allowed.contains(extWithoutDot) || allowed.contains("." + extWithoutDot);
    }

    /**
     * Gets the file extension from a filename.
     *
     * @param filename the filename
     * @return the extension including the dot, or empty string
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }
        int lastDotIndex = filename.lastIndexOf('.');
        return lastDotIndex == -1 ? "" : filename.substring(lastDotIndex);
    }
}