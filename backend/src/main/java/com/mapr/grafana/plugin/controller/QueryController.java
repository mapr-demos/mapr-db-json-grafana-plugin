package com.mapr.grafana.plugin.controller;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * TODO doc
 */
public class QueryController {

    private static final Logger log = LoggerFactory.getLogger(QueryController.class);

    @PostMapping("/query")
    public static ResponseEntity query(@RequestBody JsonNode request) {
        log.info("Query request: {}", request);
        return ResponseEntity.notFound().build();
    }
}
