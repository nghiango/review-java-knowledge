package lab.springmvc.questions;

import java.util.Set;

public class Q12MultipartFileUploadSecurityExample {

    record UploadedFile(String originalFilename, String contentType, long sizeBytes) {}

    static class FileUploadValidator {
        private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "png", "pdf");
        private static final long MAX_FILE_SIZE = 5L * 1024 * 1024; // 5 MB

        public boolean isFileSafe(UploadedFile file) {
            if (file.sizeBytes() > MAX_FILE_SIZE) {
                return false;
            }
            String filename = file.originalFilename();
            if (filename == null || !filename.contains(".")) {
                return false;
            }
            String ext =
                    filename.substring(filename.lastIndexOf('.') + 1)
                            .toLowerCase(java.util.Locale.ROOT);
            return ALLOWED_EXTENSIONS.contains(ext);
        }
    }

    public static void main(String[] args) {
        FileUploadValidator validator = new FileUploadValidator();
        UploadedFile safeFile = new UploadedFile("avatar.png", "image/png", 1024 * 50);
        UploadedFile executableFile = new UploadedFile("script.sh", "application/x-sh", 500);

        boolean isAvatarSafe = validator.isFileSafe(safeFile); // true
        boolean isScriptSafe = validator.isFileSafe(executableFile); // false

        System.out.println("Avatar safe: " + isAvatarSafe + ", Script safe: " + isScriptSafe);
    }
}
