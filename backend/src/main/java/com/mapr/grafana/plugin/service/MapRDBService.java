package com.mapr.grafana.plugin.service;

import com.mapr.grafana.plugin.model.DatasourceStatus;
import com.mapr.grafana.plugin.model.GrafanaMetrics;
import com.mapr.grafana.plugin.model.GrafanaQueryRequest;

import java.util.Set;

/**
 * TODO doc
 */
public interface MapRDBService {

    DatasourceStatus status();

    Set<GrafanaMetrics> query(GrafanaQueryRequest queryRequest);

}
