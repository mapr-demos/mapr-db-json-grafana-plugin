package com.mapr.grafana.plugin.controller;

import com.mapr.grafana.plugin.model.DatasourceStatus;
import com.mapr.grafana.plugin.service.MapRDBService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * TODO doc
 */
@CrossOrigin
@RestController
public class TestDatasourceController {

    private final MapRDBService mapRDBService;

    @Autowired
    public TestDatasourceController(MapRDBService mapRDBService) {
        this.mapRDBService = mapRDBService;
    }

    @GetMapping("/")
    public ResponseEntity<DatasourceStatus> status() {
        DatasourceStatus status = mapRDBService.status();
        return (status.isOk()) ? ResponseEntity.ok(status) : ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(status);
    }

}
