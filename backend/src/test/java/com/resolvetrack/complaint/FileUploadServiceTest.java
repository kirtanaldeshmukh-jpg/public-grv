package com.resolvetrack.complaint;

import com.resolvetrack.common.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FileUploadServiceTest {

    private FileUploadService fileUploadService;

    @TempDir
    Path tempUploadDir;

    @BeforeEach
    void setUp() {
        fileUploadService = new FileUploadService(tempUploadDir.toString());
    }

    @Test
    @DisplayName("Valid JPEG image under 5 MB is successfully validated and stored")
    void testStoreFiles_ValidImage_Success() {
        MockMultipartFile file = new MockMultipartFile(
                "files",
                "evidence.jpg",
                "image/jpeg",
                "fake image content".getBytes()
        );

        Complaint complaint = Complaint.builder().id(1L).build();
        List<ComplaintAttachment> attachments = fileUploadService.storeFiles(List.of(file), complaint);

        assertEquals(1, attachments.size());
        assertEquals("evidence.jpg", attachments.get(0).getOriginalFilename());
        assertEquals("image/jpeg", attachments.get(0).getContentType());
        assertTrue(attachments.get(0).getStoredFilename().endsWith(".jpg"));
    }

    @Test
    @DisplayName("Valid PDF document under 5 MB is successfully validated and stored")
    void testStoreFiles_ValidPdf_Success() {
        MockMultipartFile file = new MockMultipartFile(
                "files",
                "complaint_notice.pdf",
                "application/pdf",
                "%PDF-1.4 mock content".getBytes()
        );

        Complaint complaint = Complaint.builder().id(2L).build();
        List<ComplaintAttachment> attachments = fileUploadService.storeFiles(List.of(file), complaint);

        assertEquals(1, attachments.size());
        assertEquals("complaint_notice.pdf", attachments.get(0).getOriginalFilename());
        assertEquals("application/pdf", attachments.get(0).getContentType());
        assertTrue(attachments.get(0).getStoredFilename().endsWith(".pdf"));
    }

    @Test
    @DisplayName("File exceeding 5 MB limit is rejected with BusinessRuleException")
    void testStoreFiles_Exceeds5Mb_ThrowsException() {
        byte[] oversized = new byte[(int) (FileUploadService.MAX_FILE_SIZE + 1024)];
        MockMultipartFile file = new MockMultipartFile(
                "files",
                "large_video_or_scan.pdf",
                "application/pdf",
                oversized
        );

        Complaint complaint = Complaint.builder().id(3L).build();
        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> fileUploadService.storeFiles(List.of(file), complaint));

        assertTrue(ex.getMessage().contains("exceeds the maximum allowed size of 5 MB"));
    }

    @Test
    @DisplayName("Disallowed file type (e.g. .exe or script) is rejected with BusinessRuleException")
    void testStoreFiles_DisallowedType_ThrowsException() {
        MockMultipartFile file = new MockMultipartFile(
                "files",
                "malware.exe",
                "application/octet-stream",
                "MZ executable content".getBytes()
        );

        Complaint complaint = Complaint.builder().id(4L).build();
        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> fileUploadService.storeFiles(List.of(file), complaint));

        assertTrue(ex.getMessage().contains("invalid content type") || ex.getMessage().contains("unsupported file extension"));
    }
}

