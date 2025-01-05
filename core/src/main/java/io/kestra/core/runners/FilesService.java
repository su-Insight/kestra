package io.kestra.core.runners;

import io.kestra.core.exceptions.IllegalVariableEvaluationException;
import io.kestra.core.tasks.PluginUtilsService;
import io.kestra.core.utils.ListUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.kestra.core.utils.Rethrow.throwBiConsumer;
import static io.kestra.core.utils.Rethrow.throwFunction;

public abstract class FilesService {
     public static Map<String, String> inputFiles(RunContext runContext, Object inputs) throws Exception {
         Logger logger = runContext.logger();

         Map<String, String> inputFiles = inputs == null ? Map.of() : PluginUtilsService.transformInputFiles(
             runContext,
             inputs
         );

         inputFiles
             .forEach(throwBiConsumer((fileName, input) -> {
                 var file = new File(runContext.tempDir().toString(), fileName);

                 if (!file.getParentFile().exists()) {
                     //noinspection ResultOfMethodCallIgnored
                     file.getParentFile().mkdirs();
                 }

                 var fileContent = runContext.render(input);
                 if (fileContent.startsWith("kestra://")) {
                     try (var is = runContext.uriToInputStream(URI.create(fileContent));
                          var out = new FileOutputStream(file)) {
                         IOUtils.copyLarge(is, out);
                     }
                 } else {
                     Files.write(file.toPath(), fileContent.getBytes());
                 }
             }));

         logger.info("Provided {} input(s).", inputFiles.size());

         return inputFiles;
     }

     public static Map<String, URI> outputFiles(RunContext runContext, List<String> outputs) throws Exception {
         var outputFiles = ListUtils.emptyOnNull(outputs)
            .stream()
            .flatMap(throwFunction(output -> FilesService.outputMatcher(runContext, output)))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

         runContext.logger().info("Captured {} output(s).", outputFiles.size());

        return outputFiles;
    }

    private static Stream<AbstractMap.SimpleEntry<String, URI>> outputMatcher(RunContext runContext, String output) throws IllegalVariableEvaluationException, IOException {
        var glob = runContext.render(output);
        final PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher("glob:" + glob);

        try (Stream<Path> walk = Files.walk(runContext.tempDir())) {
            return walk
                .filter(Files::isRegularFile)
                .filter(path -> pathMatcher.matches(runContext.tempDir().relativize(path)))
                .map(throwFunction(path -> new AbstractMap.SimpleEntry<>(
                    runContext.tempDir().relativize(path).toString(),
                    runContext.putTempFile(path.toFile())
                )))
                .toList()
                .stream();
        }
    }
}
