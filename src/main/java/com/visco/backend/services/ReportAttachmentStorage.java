package com.visco.backend.services;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import java.io.IOException;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Uploads generated report files (PDF, Excel) to Cloudinary and returns a
 * signed HTTPS URL that can be safely included in an outbound email.
 *
 * <p>Originally the scheduled and weekly reports were sent as binary
 * attachments. On the Render free tier a 50k-record report can easily
 * exceed 50 MB; encoding it to base64 in memory produces a second copy
 * of the same size, so a single email job can OOM the 512 MB container.
 * Sending a download link instead keeps the heap footprint at the few
 * kilobytes of the request/response and the file lives in Cloudinary.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ReportAttachmentStorage {

    private static final String FOLDER = "reports";

    private final Cloudinary cloudinary;

    /**
     * Uploads a non-image file (PDF, Excel) to Cloudinary and returns the
     * secure download URL.
     *
     * @param filename    the original filename (without extension), used to
     *                    build a stable {@code public_id}
     * @param content     the raw file bytes
     * @param contentType MIME type, used to choose the file extension
     * @return a {@code https://} URL the recipient can click to download,
     *         or {@code null} if the upload failed
     */
    public String uploadReport(String filename, byte[] content, String contentType) {
        try {
            String resourceType = contentType != null && contentType.contains("pdf")
                ? "image"   // Cloudinary treats PDFs as image resource type for delivery
                : "raw";    // xlsx, csv, etc.
            String format = contentType != null && contentType.contains("pdf")
                ? "pdf"
                : contentType != null && contentType.contains("spreadsheet")
                    ? "xlsx"
                    : "bin";

            Map<?, ?> result = cloudinary.uploader().upload(
                content,
                ObjectUtils.asMap(
                    "folder", FOLDER,
                    "public_id", sanitize(filename),
                    "resource_type", resourceType,
                    "format", format,
                    "overwrite", true
                )
            );
            String url = (String) result.get("secure_url");
            log.info("Report uploaded to Cloudinary: {} ({} bytes)", url, content.length);
            return url;
        } catch (IOException e) {
            log.error("Failed to upload report to Cloudinary: {}", e.getMessage(), e);
            return null;
        }
    }

    private String sanitize(String name) {
        if (name == null) return "report";
        return name.replaceAll("[^a-zA-Z0-9\\-_]", "_");
    }
}
