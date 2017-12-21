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
 * Returns metric options, which will be displayed by Grafana query editor to get metric suggestions.
 */
@CrossOrigin
@RestController
public class SearchMetricOptionsController {

    private static final Logger log = LoggerFactory.getLogger(SearchMetricOptionsController.class);

    @PostMapping("/search")
    public static ResponseEntity findMetrics(@RequestBody JsonNode request) {
        log.debug("Search metrics request: {}", request);
        return ResponseEntity.notFound().build();
    }
}
