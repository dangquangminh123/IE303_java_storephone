package com.storephone.util;

import com.storephone.exception.ValidationException;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

public class ImageStorage {
    private static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("png", "jpg", "jpeg", "webp");

    private final Path rootDir;

    public ImageStorage(Path rootDir) {
        this.rootDir = rootDir == null ? Path.of("data/images") : rootDir;
    }

    public Path getRootDir() {
        return rootDir;
    }

    public String saveImage(Path sourceFile, String subFolder) {
        if (sourceFile == null || !Files.isRegularFile(sourceFile)) {
            throw new ValidationException("image", "Tệp ảnh không tồn tại.");
        }
        try {
            long size = Files.size(sourceFile);
            if (size <= 0 || size > MAX_FILE_SIZE_BYTES) {
                throw new ValidationException("image", "Dung lượng ảnh phải lớn hơn 0 và tối đa 5MB.");
            }
            String fileName = sourceFile.getFileName().toString();
            String ext = extractExtension(fileName);
            if (!ALLOWED_EXTENSIONS.contains(ext)) {
                throw new ValidationException("image", "Chỉ chấp nhận ảnh định dạng PNG, JPG, JPEG hoặc WEBP.");
            }
            if (!isValidImageContent(sourceFile, ext)) {
                throw new ValidationException("image", "Nội dung tệp không phải là hình ảnh hợp lệ.");
            }

            String safeFolder = (subFolder == null || subFolder.isBlank()) ? "misc" : subFolder.trim();
            Path targetFolder = rootDir.resolve(safeFolder);
            Files.createDirectories(targetFolder);

            String storedName = UUID.randomUUID() + "." + ext;
            Path destination = targetFolder.resolve(storedName);
            Files.copy(sourceFile, destination, StandardCopyOption.REPLACE_EXISTING);

            return (safeFolder + "/" + storedName).replace('\\', '/');
        } catch (IOException ex) {
            throw new IllegalStateException("Không thể lưu tệp ảnh.", ex);
        }
    }

    public Path resolvePath(String storedPath) {
        if (storedPath == null || storedPath.isBlank()) {
            return null;
        }
        Path candidate = Path.of(storedPath);
        if (Files.isRegularFile(candidate)) {
            return candidate;
        }
        return rootDir.resolve(storedPath);
    }

    public void deleteQuietly(String storedPath) {
        Path resolved = resolvePath(storedPath);
        if (resolved != null) {
            try {
                Files.deleteIfExists(resolved);
            } catch (IOException ignored) {
            }
        }
    }

    public ImageIcon loadScaledIcon(String relativePath, int width, int height, String placeholderLabel) {
        Path path = resolvePath(relativePath);
        if (path != null && Files.isRegularFile(path)) {
            try {
                BufferedImage img = ImageIO.read(path.toFile());
                if (img != null) {
                    Image scaled = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
                    return new ImageIcon(scaled);
                }
            } catch (IOException ignored) {
            }
        }
        return createPlaceholderIcon(width, height, placeholderLabel);
    }

    public ImageIcon loadPreviewFromSource(Path sourceFile, int width, int height, String placeholderLabel) {
        if (sourceFile != null && Files.isRegularFile(sourceFile)) {
            try {
                BufferedImage img = ImageIO.read(sourceFile.toFile());
                if (img != null) {
                    Image scaled = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
                    return new ImageIcon(scaled);
                }
            } catch (IOException ignored) {
            }
        }
        return createPlaceholderIcon(width, height, placeholderLabel);
    }

    private static boolean isValidImageContent(Path file, String ext) throws IOException {
        BufferedImage decoded = ImageIO.read(file.toFile());
        if (decoded != null && decoded.getWidth() > 0 && decoded.getHeight() > 0) {
            return true;
        }
        if ("webp".equals(ext)) {
            byte[] header = new byte[12];
            try (InputStream in = Files.newInputStream(file)) {
                int read = in.read(header);
                if (read == 12) {
                    String riff = new String(header, 0, 4, StandardCharsets.US_ASCII);
                    String webp = new String(header, 8, 4, StandardCharsets.US_ASCII);
                    return "RIFF".equals(riff) && "WEBP".equals(webp);
                }
            }
        }
        return false;
    }

    public static ImageIcon createPlaceholderIcon(int width, int height, String text) {
        BufferedImage canvas = new BufferedImage(Math.max(1, width), Math.max(1, height), BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = canvas.createGraphics();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(236, 240, 245));
            g2.fillRect(0, 0, width, height);
            g2.setColor(new Color(190, 200, 214));
            g2.drawRect(0, 0, width - 1, height - 1);

            String label = (text == null || text.isBlank()) ? "No Image" : text;
            g2.setColor(new Color(95, 109, 126));
            g2.setFont(new Font("SansSerif", Font.BOLD, Math.max(11, width / 10)));
            FontMetrics fm = g2.getFontMetrics();
            int x = Math.max(4, (width - fm.stringWidth(label)) / 2);
            int y = (height - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(label, x, y);
        } finally {
            g2.dispose();
        }
        return new ImageIcon(canvas);
    }

    private static String extractExtension(String fileName) {
        int idx = fileName.lastIndexOf('.');
        if (idx < 0 || idx == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(idx + 1).toLowerCase(Locale.ROOT);
    }
}
