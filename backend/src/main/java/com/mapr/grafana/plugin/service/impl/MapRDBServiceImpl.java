package com.mapr.grafana.plugin.service.impl;

import com.mapr.db.exceptions.TableNotFoundException;
import com.mapr.grafana.plugin.model.*;
import com.mapr.grafana.plugin.model.timeseries.AggregationTimeSeries;
import com.mapr.grafana.plugin.model.timeseries.DocumentCountTimeSeries;
import com.mapr.grafana.plugin.model.timeseries.FieldAverageTimeSeries;
import com.mapr.grafana.plugin.model.timeseries.GrafanaTimeSeries;
import com.mapr.grafana.plugin.service.MapRDBService;
import com.mapr.grafana.plugin.util.MetricsQueryBuilder;
import org.ojai.Document;
import org.ojai.DocumentStream;
import org.ojai.exceptions.OjaiException;
import org.ojai.store.Connection;
import org.ojai.store.DriverManager;
import org.ojai.store.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.mapr.grafana.plugin.model.GrafanaQueryTarget.*;

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
                        return queryRawDocuments(queryRequest, target);
                    } else if (GrafanaQueryTarget.TIME_SERIES_TYPE.equals(target.getType())) {
                        return queryTimeSeries(queryRequest, target);
                    } else {
                        return Optional.<GrafanaMetrics>empty();
                    }
                })
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toSet());
    }

    private Optional<GrafanaRawDocuments> queryRawDocuments(GrafanaQueryRequest queryRequest, GrafanaQueryTarget target) {

        log.debug("Querying raw documents for target: {}", target);
        try {

            Query query = MetricsQueryBuilder.forConnection(connection)
                    .select(target.getSelectFields())
                    .withJsonConditon(target.getCondition())
                    .withTimeRange(target.getTimeField(), queryRequest.getRange())
                    .withLimit(target.getLimit(), DEFAULT_RAW_DOCUMENT_LIMIT, MAX_RAW_DOCUMENT_LIMIT)
                    .constructQuery();

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

    private Optional<GrafanaMetrics> queryTimeSeries(GrafanaQueryRequest queryRequest, GrafanaQueryTarget target) {

        log.debug("Querying time series for target: {}", target);
        if (target.getTarget() == null || target.getTarget().isEmpty() ||
                target.getTimeField() == null || target.getTimeField().isEmpty() ||
                target.getMetric() == null || target.getMetric().isEmpty()) {

            log.warn("Target name, metric and time field are required for querying time series. Invalid target: {}",
                    target);

            return Optional.empty();
        }

        GrafanaTimeSeries series;
        if (DOCUMENT_COUNT_METRIC.equals(target.getMetric())) {
            series = new DocumentCountTimeSeries(target.getTarget(), target.getTimeField(), queryRequest.getIntervalMs());
        } else if (FIELD_VALUE_METRIC.equals(target.getMetric())) {

            series = new AggregationTimeSeries((existing, incoming) -> existing, target.getTarget(),
                    target.getTimeField(), target.getMetricField(), queryRequest.getIntervalMs());

        } else if (FIELD_MIN_METRIC.equals(target.getMetric())) {

            series = new AggregationTimeSeries(
                    (existing, incoming) -> existing.getValue() < incoming.getValue() ? existing : incoming,
                    target.getTarget(), target.getTimeField(), target.getMetricField(), queryRequest.getIntervalMs());

        } else if (FIELD_MAX_METRIC.equals(target.getMetric())) {

            series = new AggregationTimeSeries(
                    (existing, incoming) -> existing.getValue() > incoming.getValue() ? existing : incoming,
                    target.getTarget(), target.getTimeField(), target.getMetricField(), queryRequest.getIntervalMs());

        } else if (FIELD_AVG_METRIC.equals(target.getMetric())) {

            series = new FieldAverageTimeSeries(target.getTarget(), target.getTimeField(), target.getMetricField(),
                    queryRequest.getIntervalMs());

        } else {
            log.warn("Specified metric '{}' is not supported.", target.getMetric());
            return Optional.empty();
        }

        MetricsQueryBuilder queryBuilder = MetricsQueryBuilder.forConnection(connection)
                .select(target.getTimeField())
                .withJsonConditon(target.getCondition())
                .withTimeRange(target.getTimeField(), queryRequest.getRange())
                .withLimit(target.getLimit(), DEFAULT_RAW_DOCUMENT_LIMIT, MAX_RAW_DOCUMENT_LIMIT)
                .orderBy(target.getTimeField());

        if (target.getMetricField() != null && !target.getMetricField().isEmpty()) {
            queryBuilder.select(target.getMetricField());
        }

        DocumentStream documentStream = connection.getStore(target.getTable()).findQuery(queryBuilder.constructQuery().build());
        for (Document document : documentStream) {
            series.addDocument(document);
        }

        return Optional.of(series);
    }

    @PreDestroy
    public void destroy() {
        if (this.connection != null) {
            this.connection.close();
        }
    }

}
