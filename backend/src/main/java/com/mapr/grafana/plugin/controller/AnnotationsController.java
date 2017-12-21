package com.mapr.grafana.plugin.controller;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Handles annotation requests and returns annotations, which will be displayed by Grafana dashboards.
 */
@CrossOrigin
@RestController
public class AnnotationsController {

    private static final Logger log = LoggerFactory.getLogger(AnnotationsController.class);

    @PostMapping("/annotations")
    public static ResponseEntity annotations(@RequestBody JsonNode request) {
        log.debug("Annotations request: {}", request);
        return ResponseEntity.notFound().build();
    }
}
