package io.kestra.storage.local;

import io.kestra.core.storages.FileAttributes;
import io.kestra.core.storages.StorageInterface;
import io.kestra.core.tenant.TenantService;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.net.URI;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import static io.kestra.core.utils.Rethrow.throwFunction;

@Singleton
@LocalStorageEnabled
public class LocalStorage implements StorageInterface {
    private final LocalConfig config;
    private final TenantService tenantService;

    @Inject
    public LocalStorage(LocalConfig config, TenantService tenantService) throws IOException {
        this.config = config;
        this.tenantService = tenantService;

        if (!Files.exists(config.getBasePath())) {
            Files.createDirectories(config.getBasePath());
        }
    }

    private Path getBasePath(String tenantId) throws IOException {
        if (tenantId == null) {
            return config.getBasePath().toAbsolutePath();
        }

        Path basePath = tenantService.storageConfiguration(tenantId)
            .filter(storageConfiguration -> storageConfiguration.getConfiguration() != null)
            .map(throwFunction(storageConfiguration -> {
                if (storageConfiguration.getConfiguration().containsKey("basePath")) {
                    Path tenantPath = Path.of((String) storageConfiguration.getConfiguration().get("basePath"));
                    if (!Files.exists(tenantPath)) {
                        Files.createDirectories(tenantPath);
                    }
                    return tenantPath;
                }
                return null;
            }))
            .orElse(config.getBasePath());
        return Paths.get(basePath.toString(), tenantId);
    }

    private Path getPath(String tenantId, URI uri) throws IOException {
        Path basePath = getBasePath(tenantId);
        if (uri == null) {
            return basePath;
        }

        parentTraversalGuard(uri);
        return Paths.get(basePath.toString(), uri.getPath());
    }

    @Override
    public InputStream get(String tenantId, URI uri) throws IOException {
        return new BufferedInputStream(new FileInputStream(getPath(tenantId, uri)
            .toAbsolutePath()
            .toString())
        );
    }

    @Override
    public boolean exists(String tenantId, URI uri) throws IOException {
        return Files.exists(getPath(tenantId, uri));
    }

    @Override
    public List<FileAttributes> list(String tenantId, URI uri) throws IOException {
        try (Stream<Path> stream = Files.list(getPath(tenantId, uri))) {
            return stream
                .map(throwFunction(file -> {
                    URI relative = URI.create(
                        getPath(tenantId, null).relativize(
                            Path.of(file.toUri())
                        ).toString()
                    );
                    return getAttributes(tenantId, relative);
                }))
                .toList();
        } catch (NoSuchFileException e) {
            throw new FileNotFoundException(e.getMessage());
        }
    }

    @Override
    public Long size(String tenantId, URI uri) throws IOException {
        try {
            return Files.size(getPath(tenantId, uri));
        } catch (NoSuchFileException e) {
            throw new FileNotFoundException("Unable to find file at '" + uri + "'");
        } catch (IOException e) {
            throw new IOException("Unable to find file at '" + uri + "' with message '" + e.getMessage() + "'");
        }
    }

    @Override
    public Long lastModifiedTime(String tenantId, URI uri) throws IOException {
        FileTime lastModifiedTime;
        try {
            lastModifiedTime = Files.getLastModifiedTime(getPath(tenantId, uri));
        } catch (NoSuchFileException e) {
            throw new FileNotFoundException(e.getMessage());
        }
        return lastModifiedTime.toMillis();
    }

    @Override
    public URI put(String tenantId, URI uri, InputStream data) throws IOException {
        File file = getPath(tenantId, uri).toFile();
        File parent = file.getParentFile();
        if (!parent.exists() && !parent.mkdirs()) {
            throw new RuntimeException("Cannot create directory: " + parent.getAbsolutePath());
        }

        try (data; OutputStream outStream = new FileOutputStream(file)) {
            byte[] buffer = new byte[8 * 1024];
            int bytesRead;
            while ((bytesRead = data.read(buffer)) != -1) {
                outStream.write(buffer, 0, bytesRead);
            }
        }
        return URI.create("kestra://" + uri.getPath());
    }

    @Override
    public FileAttributes getAttributes(String tenantId, URI uri) throws IOException {
        Path path = getPath(tenantId, uri);
        try {
            return LocalFileAttributes.builder()
                .fileName(path.getFileName().toString())
                .basicFileAttributes(Files.readAttributes(path, BasicFileAttributes.class))
                .build();
        } catch (NoSuchFileException e) {
            throw new FileNotFoundException(e.getMessage());
        }
    }

    @Override
    public URI createDirectory(String tenantId, URI uri) throws IOException {
        if (uri == null || uri.getPath().isEmpty()) {
            throw new IllegalArgumentException("Unable to create a directory with empty url.");
        }
        File file = getPath(tenantId, uri).toFile();
        if (!file.exists() && !file.mkdirs()) {
            throw new RuntimeException("Cannot create directory: " + file.getAbsolutePath());
        }
        return URI.create("kestra://" + uri.getPath());
    }

    @Override
    public URI move(String tenantId, URI from, URI to) throws IOException {
        try {
            Files.move(
                getPath(tenantId, from),
                getPath(tenantId, to),
                StandardCopyOption.ATOMIC_MOVE);
        } catch (NoSuchFileException e) {
            throw new FileNotFoundException(e.getMessage());
        }
        return URI.create("kestra://" + to.getPath());
    }

    @Override
    public boolean delete(String tenantId, URI uri) throws IOException {
        Path path = getPath(tenantId, uri);
        File file = path.toFile();

        if (file.isDirectory()) {
            FileUtils.deleteDirectory(file);
            return true;
        }

        return Files.deleteIfExists(path);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    public List<URI> deleteByPrefix(String tenantId, URI storagePrefix) throws IOException {
        Path path = this.getPath(tenantId, storagePrefix);

        if (!path.toFile().exists()) {
            return List.of();
        }

        try (Stream<Path> walk = Files.walk(path)) {
            return walk.sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .peek(File::delete)
                .map(throwFunction(r -> getKestraUri(tenantId, r.toPath())))
                .toList();
        }
    }

    private URI getKestraUri(String tenantId, Path path) throws IOException {
        Path prefix = getBasePath(tenantId);
        return URI.create("kestra:///" + prefix.relativize(path));
    }

    private void parentTraversalGuard(URI uri) {
        if (uri.toString().contains("..")) {
            throw new IllegalArgumentException("File should be accessed with their full path and not using relative '..' path.");
        }
    }
}
