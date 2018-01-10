package com.mapr.grafana.plugin.controller;

import com.mapr.grafana.plugin.model.GrafanaMetrics;
import com.mapr.grafana.plugin.model.GrafanaQueryRequest;
import com.mapr.grafana.plugin.service.MapRDBService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.Set;

/**
 * Handles 'query' requests and returns metrics based on input.
 */
@CrossOrigin
@RestController
public class QueryController {

    private static final Logger log = LoggerFactory.getLogger(QueryController.class);

    private final MapRDBService mapRDBService;

    @Autowired
    public QueryController(MapRDBService mapRDBService) {
        this.mapRDBService = mapRDBService;
    }

    @PostMapping("/query")
    public Set<GrafanaMetrics> query(@Valid @RequestBody GrafanaQueryRequest queryRequest) {
        log.debug("Grafana query request: {}", queryRequest);
        return mapRDBService.query(queryRequest);
    }
}
