package com.resolvetrack.complaint;

import com.resolvetrack.common.BusinessRuleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

@Service
public class FileUploadService {

    private static final Logger log = LoggerFactory.getLogger(FileUploadService.class);

    public static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/webp",
            "image/gif",
            "application/pdf"
    );

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "webp", "gif", "pdf"
    );

    private final Path uploadLocation;

    public FileUploadService(@Value("${resolvetrack.upload.dir:./uploads}") String uploadDir) {
        this.uploadLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadLocation);
            log.info("Initialized local uploads directory at: {}", this.uploadLocation);
        } catch (IOException e) {
            log.error("Could not initialize upload directory: {}", uploadDir, e);
            throw new RuntimeException("Could not initialize upload directory", e);
        }
    }

    public List<ComplaintAttachment> storeFiles(List<MultipartFile> files, Complaint complaint) {
        if (files == null || files.isEmpty()) {
            return Collections.emptyList();
        }

        List<ComplaintAttachment> savedAttachments = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            validateFile(file);
            ComplaintAttachment attachment = saveSingleFile(file, complaint);
            savedAttachments.add(attachment);
        }
        return savedAttachments;
    }

    public void validateFile(MultipartFile file) {
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessRuleException(
                    String.format("File '%s' exceeds the maximum allowed size of 5 MB (size: %.2f MB)",
                            file.getOriginalFilename(), file.getSize() / (1024.0 * 1024.0))
            );
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new BusinessRuleException(
                    String.format("File '%s' has an invalid content type '%s'. Only images (JPEG, PNG, WEBP, GIF) and PDFs are allowed.",
                            file.getOriginalFilename(), contentType)
            );
        }

        String originalFilename = StringUtils.cleanPath(Objects.requireNonNullElse(file.getOriginalFilename(), "file"));
        String extension = getFileExtension(originalFilename);
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new BusinessRuleException(
                    String.format("File '%s' has an unsupported file extension '.%s'. Only images and PDFs are accepted.",
                            originalFilename, extension)
            );
        }
    }

    private ComplaintAttachment saveSingleFile(MultipartFile file, Complaint complaint) {
        String originalFilename = StringUtils.cleanPath(Objects.requireNonNullElse(file.getOriginalFilename(), "attachment"));
        String extension = getFileExtension(originalFilename);
        String storedFilename = UUID.randomUUID().toString() + (extension.isEmpty() ? "" : "." + extension);
        Path destination = this.uploadLocation.resolve(storedFilename);

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("Failed to store file {}", originalFilename, e);
            throw new BusinessRuleException("Failed to store uploaded file: " + originalFilename);
        }

        return ComplaintAttachment.builder()
                .complaint(complaint)
                .originalFilename(originalFilename)
                .storedFilename(storedFilename)
                .filePath(destination.toString())
                .contentType(file.getContentType())
                .fileSize(file.getSize())
                .build();
    }

    private String getFileExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < filename.length() - 1) {
            return filename.substring(dotIndex + 1);
        }
        return "";
    }

    public Path getUploadLocation() {
        return uploadLocation;
    }
}

