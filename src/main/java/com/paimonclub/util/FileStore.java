package com.paimonclub.util;

import com.paimonclub.common.BusinessException;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 文件存储：接收 Base64 data-url 图片，落盘到上传目录，返回可访问 URL。
 * 上传目录优先级：系统属性 paimon.upload.dir > ${jboss.server.data.dir}/paimon-uploads。
 */
@ApplicationScoped
public class FileStore {

    private static final Logger LOG = Logger.getLogger(FileStore.class.getName());
    private static final long MAX_SIZE = 5L * 1024 * 1024;
    /** 整个上传目录的容量上限，超过后拒绝新上传，避免把磁盘写满导致整站不可用 */
    private static final long MAX_DIR_BYTES = 500L * 1024 * 1024;

    private final java.util.concurrent.atomic.AtomicLong usedBytes = new java.util.concurrent.atomic.AtomicLong();

    private Path root;

    @PostConstruct
    void init() {
        String dir = System.getProperty("paimon.upload.dir");
        if (dir == null || dir.isBlank()) {
            String dataDir = System.getProperty("jboss.server.data.dir", System.getProperty("java.io.tmpdir"));
            dir = dataDir + File.separator + "paimon-uploads";
        }
        root = Paths.get(dir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
            usedBytes.set(directorySize());
            LOG.info("上传目录: " + root + "，已用 " + (usedBytes.get() / 1024 / 1024) + "MB / 上限 "
                    + (MAX_DIR_BYTES / 1024 / 1024) + "MB");
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, "创建上传目录失败: " + root, ex);
        }
    }

    /**
     * 保存 Base64 data-url，返回 /api/files/{name}
     */
    public String saveDataUrl(String dataUrl) {
        if (dataUrl == null || dataUrl.isBlank()) {
            throw BusinessException.badRequest("文件内容为空");
        }
        String base64 = dataUrl;
        String ext = "png";
        int comma = dataUrl.indexOf(',');
        if (dataUrl.startsWith("data:") && comma > 0) {
            String meta = dataUrl.substring(5, comma);
            base64 = dataUrl.substring(comma + 1);
            if (meta.contains("jpeg") || meta.contains("jpg")) {
                ext = "jpg";
            } else if (meta.contains("gif")) {
                ext = "gif";
            } else if (meta.contains("webp")) {
                ext = "webp";
            }
        }
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(base64.replaceAll("\\s", ""));
        } catch (IllegalArgumentException ex) {
            throw BusinessException.badRequest("文件内容不是合法的 Base64 编码");
        }
        if (bytes.length > MAX_SIZE) {
            throw BusinessException.badRequest("文件不能超过 5MB");
        }
        if (usedBytes.get() + bytes.length > MAX_DIR_BYTES) {
            throw BusinessException.badRequest("上传空间已满，请联系管理员清理上传目录");
        }
        String name = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        try {
            Files.write(resolve(name), bytes);
            usedBytes.addAndGet(bytes.length);
        } catch (IOException ex) {
            throw new BusinessException(500, "文件保存失败");
        }
        return "api/files/" + name;
    }

    public Path resolve(String name) {
        Path path = root.resolve(name).normalize();
        if (!path.startsWith(root)) {
            throw BusinessException.badRequest("非法文件名");
        }
        return path;
    }

    public boolean exists(String name) {
        return Files.exists(resolve(name));
    }

    public Path getRoot() {
        return root;
    }

    /** 当前上传目录已用字节数 */
    public long usedBytes() {
        return usedBytes.get();
    }

    private long directorySize() throws IOException {
        try (java.util.stream.Stream<Path> stream = Files.walk(root, 2)) {
            return stream.filter(Files::isRegularFile).mapToLong(path -> {
                try {
                    return Files.size(path);
                } catch (IOException ignored) {
                    return 0L;
                }
            }).sum();
        }
    }
}
