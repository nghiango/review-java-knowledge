package lab.springmvc.questions;

import java.util.Locale;
import java.util.Set;

@SuppressWarnings("unused")
public final class Q25MultipartUploadSecurityExample {
    private Q25MultipartUploadSecurityExample() {}

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".pdf", ".png", ".jpg");
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB

    public record UploadValidationResult(boolean valid, String reason) {}

    public static UploadValidationResult validateUploadedFile(String originalFilename, long sizeBytes, String contentType) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return new UploadValidationResult(false, "Filename cannot be empty");
        }

        // 1. Path traversal protection: block filenames containing relative paths
        if (originalFilename.contains("..") || originalFilename.contains("/") || originalFilename.contains("\\")) {
            return new UploadValidationResult(false, "Path traversal sequence detected in filename");
        }

        // 2. Strict file size bound
        if (sizeBytes > MAX_FILE_SIZE) {
            return new UploadValidationResult(false, "File exceeds maximum size of 5MB");
        }

        // 3. Extension check
        String lowerName = originalFilename.toLowerCase(Locale.ROOT);
        boolean hasAllowedExt = ALLOWED_EXTENSIONS.stream().anyMatch(lowerName::endsWith);
        if (!hasAllowedExt) {
            return new UploadValidationResult(false, "File extension is not permitted");
        }

        return new UploadValidationResult(true, "OK");
    }

    public static void main(String[] args) {
        UploadValidationResult malicious = validateUploadedFile("../../etc/passwd.jpg", 1024, "image/jpeg");
        boolean rejected = !malicious.valid(); // true (path traversal detected)

        UploadValidationResult valid = validateUploadedFile("statement.pdf", 2048, "application/pdf");
        boolean accepted = valid.valid(); // true
    }
}
