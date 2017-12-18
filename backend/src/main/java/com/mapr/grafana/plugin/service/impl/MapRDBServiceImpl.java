package com.mapr.grafana.plugin.service.impl;

import com.mapr.grafana.plugin.model.DatasourceStatus;
import com.mapr.grafana.plugin.service.MapRDBService;
import org.ojai.exceptions.OjaiException;
import org.ojai.store.Connection;
import org.ojai.store.DriverManager;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;

/**
 * TODO doc
 */
@Service
public class MapRDBServiceImpl implements MapRDBService {

    private static final String CONNECTION_URL = "ojai:mapr:";
    private Connection connection;
    private DatasourceStatus status;

    public MapRDBServiceImpl() {
        try {
            // Create an OJAI connection to MapR cluster
            this.connection = DriverManager.getConnection(CONNECTION_URL);
            this.status = DatasourceStatus.ok();
        } catch (OjaiException e) {
            this.status = DatasourceStatus.error("Can not create OJAI connection", e.getMessage());
        }
    }

    @Override
    public DatasourceStatus status() {
        return status;
    }

    @PreDestroy
    public void destroy() {
        if (this.connection != null) {
            this.connection.close();
        }
    }

}
