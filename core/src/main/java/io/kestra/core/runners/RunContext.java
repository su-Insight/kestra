package io.kestra.core.runners;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.kestra.core.encryption.EncryptionService;
import io.kestra.core.exceptions.IllegalVariableEvaluationException;
import io.kestra.core.models.executions.AbstractMetricEntry;
import io.kestra.core.storages.Storage;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 *
 */
public abstract class RunContext {

    /**
     * Returns the trigger execution id attached to this context.
     *
     * @return  the string id.
     * @throws IllegalStateException if trigger execution id is defined.
     */
    @JsonIgnore
    public abstract String getTriggerExecutionId();

    /**
     * Returns an immutable {@link Map} containing all the variables attached to this context.
     *
     * @return The map variables.
     */
    @JsonInclude
    public abstract Map<String, Object> getVariables();

    public abstract String render(String inline) throws IllegalVariableEvaluationException;

    public abstract String render(String inline, Map<String, Object> variables) throws IllegalVariableEvaluationException;

    public abstract List<String> render(List<String> inline) throws IllegalVariableEvaluationException;

    public abstract List<String> render(List<String> inline, Map<String, Object> variables) throws IllegalVariableEvaluationException;

    public abstract Set<String> render(Set<String> inline) throws IllegalVariableEvaluationException;

    public abstract Set<String> render(Set<String> inline, Map<String, Object> variables) throws IllegalVariableEvaluationException;

    public abstract Map<String, Object> render(Map<String, Object> inline) throws IllegalVariableEvaluationException;

    public abstract Map<String, Object> render(Map<String, Object> inline, Map<String, Object> variables) throws IllegalVariableEvaluationException;

    public abstract Map<String, String> renderMap(Map<String, String> inline) throws IllegalVariableEvaluationException;

    public abstract Map<String, String> renderMap(Map<String, String> inline, Map<String, Object> variables) throws IllegalVariableEvaluationException;

    public abstract String decrypt(String encrypted) throws GeneralSecurityException;

    /**
     * Encrypt a plaintext string using the {@link EncryptionService} and the default encryption key.
     * If the key is not configured, it will log a WARNING and return the plaintext string as is.
     */
    public abstract String encrypt(String plaintext) throws GeneralSecurityException;

    /**
     * Gets the {@link Logger} attached to this {@link RunContext}.
     *
     * @return the {@link Logger}.
     */
    public abstract Logger logger();

    // for serialization backward-compatibility
    @JsonIgnore
    public abstract URI getStorageOutputPrefix();

    /**
     * Gets access to the Kestra's internal storage.
     *
     * @return a {@link Storage} object.
     */
    public abstract Storage storage();

    public abstract List<AbstractMetricEntry<?>> metrics();

    public abstract <T> RunContext metric(AbstractMetricEntry<T> metricEntry);

    public abstract void dynamicWorkerResult(List<WorkerTaskResult> workerTaskResults);

    public abstract List<WorkerTaskResult> dynamicWorkerResults();

    /**
     * Returns the local temporary directory associated to this context.
     *
     * <p>
     * This method automatically creates the temporary directory if not already exists.
     *
     * @return  the temp-dir path.
     */
    public abstract Path tempDir();

    /**
     * Returns the local temporary directory associated to this context.
     *
     * @param create specifies if the directory must be created if not exists.
     *
     * @return  the temp-dir path.
     */
    public abstract Path tempDir(boolean create);

    /**
     * Resolve a path inside the working directory (a.k.a. the tempDir).
     * If the resolved path escapes the working directory, an IllegalArgumentException will be thrown to protect against path traversal security issue.
     * This method is null-friendly: it will return the working directory (a.k.a. the tempDir) if called with a null path.
     */
    public abstract Path resolve(Path path);

    /**
     * @deprecated use {@link #tempFile(String)} instead
     */
    public abstract Path tempFile() throws IOException;

    public abstract Path tempFile(String extension) throws IOException;

    public abstract Path tempFile(byte[] content) throws IOException;

    public abstract Path tempFile(byte[] content, String extension) throws IOException;

    public abstract Path file(String filename) throws IOException;

    public abstract Path file(byte[] content, String filename) throws IOException;

    /**
     * Get the file extension including the '.' to be used with the various methods that took a suffix.
     *
     * @param fileName the name of the file
     * @return the file extension including the '.' or null
     */
    public abstract String fileExtension(String fileName);

    /**
     * Cleanup any temporary resources, files created through this context.
     */
    public abstract void cleanup();

    public abstract String tenantId();

    public abstract FlowInfo flowInfo();

    /**
     * Returns the value of the specified configuration property for the plugin type
     * associated to the current task or trigger.
     *
     * @param name the configuration property name.
     * @param <T>  the type of the configuration property value.
     * @return the {@link Optional} configuration property value.
     */
    public abstract <T> Optional<T> pluginConfiguration(String name);

    /**
     * Returns a map containing all the static configuration properties for the plugin type
     * associated to the current task or trigger.
     *
     * @return an unmodifiable map of key/value properties.
     */
    public abstract Map<String, Object> pluginConfigurations();

    /**
     * Gets the version of Kestra.
     *
     * @return the string version.
     */
    public abstract String version();

    public record FlowInfo(String tenantId, String namespace, String id, Integer revision) {
    }
}
