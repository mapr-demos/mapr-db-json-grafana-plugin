package com.mapr.grafana.plugin.service.impl;

import com.mapr.db.exceptions.TableNotFoundException;
import com.mapr.db.util.ConditionParser;
import com.mapr.grafana.plugin.model.*;
import com.mapr.grafana.plugin.service.MapRDBService;
import org.ojai.Document;
import org.ojai.DocumentStream;
import org.ojai.exceptions.DecodingException;
import org.ojai.exceptions.OjaiException;
import org.ojai.store.Connection;
import org.ojai.store.DriverManager;
import org.ojai.store.Query;
import org.ojai.store.QueryCondition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * MapR DB Service wich is responsible of performing all Datasource logic, such as checking connection status, querying
 * metrics.
 */
@Service
public class MapRDBServiceImpl implements MapRDBService {

    // FIXME use @Value
    public static final long CONNECTION_ATTEMPTS = 5;
    public static final long CONNECTION_RETRY_TIMEOUT_MS = 1000;

    public static final long DEFAULT_RAW_DOCUMENT_LIMIT = 500;
    public static final long MAX_RAW_DOCUMENT_LIMIT = 5000;

    private static final Logger log = LoggerFactory.getLogger(MapRDBServiceImpl.class);

    private static final String CONNECTION_URL = "ojai:mapr:";
    private Connection connection;
    private DatasourceStatus status;

    public MapRDBServiceImpl() {
        tryEstablishOjaiConnection();
    }

    private void tryEstablishOjaiConnection() {

        for (int i = 0; i < CONNECTION_ATTEMPTS && this.connection == null; i++) {
            log.debug("Trying to create OJAI connection. Attempt: '{}'");
            try {
                // Create an OJAI connection to MapR cluster
                this.connection = DriverManager.getConnection(CONNECTION_URL);
                this.status = DatasourceStatus.ok();
            } catch (OjaiException e) {
                this.status = DatasourceStatus.error("Can not create OJAI connection", e.getMessage());
            }

            try {
                Thread.sleep(CONNECTION_RETRY_TIMEOUT_MS);
            } catch (InterruptedException e) {
                log.warn("InterruptedException while waiting for the next OJAI connection attempt", e);
            }
        }
    }

    private void ensureConnectionEstablished() {
        tryEstablishOjaiConnection();
        if (this.connection == null) {
            throw new IllegalStateException("OJAI connection can not be established after " +
                    CONNECTION_ATTEMPTS + "attempts");
        }
    }

    @Override
    public DatasourceStatus status() {
        // TODO actually check if the connection alive
        tryEstablishOjaiConnection();
        log.debug("MapR-DB JSON Datasource status: {}", status);
        return status;
    }

    @Override
    public Set<GrafanaMetrics> query(GrafanaQueryRequest queryRequest) {

        ensureConnectionEstablished();
        log.debug("Performing query: {}", queryRequest);

        return queryRequest.getTargets().stream()
                .filter(target -> target.getTable() != null && !target.getTable().isEmpty())
                .map(target -> {
                    if (GrafanaQueryTarget.RAW_DOCUMENT_TYPE.equals(target.getType())) {
                        return queryRawDocuments(target);
                    } else if (GrafanaQueryTarget.TIME_SERIES_TYPE.equals(target.getType())) {
                        return queryTimeSeries(target);
                    } else {
                        return Optional.<GrafanaMetrics>empty();
                    }
                })
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toSet());
    }

    private Optional<GrafanaRawDocuments> queryRawDocuments(GrafanaQueryTarget target) {

        log.debug("Querying raw documents for target: {}", target);
        try {

            Query query = targetToOjaiQuery(connection, target);
            DocumentStream documentStream = connection.getStore(target.getTable()).findQuery(query.build());

            GrafanaRawDocuments<Document> rawDocumentsMetric = new GrafanaRawDocuments<>();
            for (Document document : documentStream) {
                rawDocumentsMetric.addDatapoint(document);
            }

            return Optional.of(rawDocumentsMetric);
        } catch (TableNotFoundException e) {
            log.warn("Table '{}' not found. Can not query documents for target: {}", target.getTable(), target);
        } catch (Exception e) {
            log.warn("Exception occurred while querying raw documents for target: " + target, e);
        }

        return Optional.empty();
    }

    private Optional<GrafanaMetrics> queryTimeSeries(GrafanaQueryTarget target) {
        // TODO implement
        log.debug("Querying time series for target: {}", target);
        return Optional.empty();
    }

    private Query targetToOjaiQuery(Connection connection, GrafanaQueryTarget target) {

        long limit = (target.getLimit() <= 0)
                ? DEFAULT_RAW_DOCUMENT_LIMIT
                : (target.getLimit() > MAX_RAW_DOCUMENT_LIMIT) ? MAX_RAW_DOCUMENT_LIMIT : target.getLimit();

        if (target.getCondition() == null || target.getCondition().isEmpty()) {
            return connection.newQuery().limit(limit);
        }

        try {
            log.debug("Building OJAI query for target: {}", target);
            QueryCondition condition = new ConditionParser().parseCondition(target.getCondition());
            return connection.newQuery().where(condition).limit(limit);
        } catch (DecodingException e) {
            log.warn("Can not decode OJAI JSON condition from : '{}'", target.getCondition());
        } catch (Exception e) {
            log.warn("Exception occurred while building OJAI query for target: " + target, e);
        }

        return connection.newQuery().limit(limit);
    }

    @PreDestroy
    public void destroy() {
        if (this.connection != null) {
            this.connection.close();
        }
    }

}
