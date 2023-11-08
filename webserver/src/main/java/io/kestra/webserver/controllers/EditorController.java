package io.kestra.webserver.controllers;

import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.validation.Validated;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
@Validated
@Controller("/api/v1/editors")
public class EditorController {
    @Inject NamespaceFileController namespaceFileController;
    @Inject FlowController flowController;

    @ExecuteOn(TaskExecutors.IO)
    @Get(uri = "/distinct-namespaces", produces = MediaType.TEXT_JSON)
    @Operation(tags = {"Editor"}, summary = "Get namespaces that contains files or flows")
    public List<String> distinctNamespaces() throws IOException, URISyntaxException {
        return Stream.concat(
            namespaceFileController.distinctNamespaces().stream(),
            flowController.listDistinctNamespace().stream()
        ).distinct().toList();
    }
}
