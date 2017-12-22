package com.mapr.grafana.plugin.util;

import com.mapr.db.util.ConditionParser;
import com.mapr.grafana.plugin.model.GrafanaQueryRequest;
import org.ojai.exceptions.DecodingException;
import org.ojai.store.Connection;
import org.ojai.store.Query;
import org.ojai.store.QueryCondition;
import org.ojai.types.ODate;
import org.ojai.types.OTime;
import org.ojai.types.OTimestamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Optional;

import static org.ojai.store.QueryCondition.Op.GREATER_OR_EQUAL;
import static org.ojai.store.QueryCondition.Op.LESS_OR_EQUAL;

public final class MetricsQueryBuilder {

    private static final Logger log = LoggerFactory.getLogger(MetricsQueryBuilder.class);

    private Connection connection;
    private Long limit;
    private String timeField;
    private String jsonCondition;
    private GrafanaQueryRequest.Range range;

    private MetricsQueryBuilder(Connection connection) {
        this.connection = connection;
    }

    public static MetricsQueryBuilder forConnection(Connection connection) {
        if (connection == null) {
            throw new IllegalArgumentException("OJAI Connection can not be null");
        }
        return new MetricsQueryBuilder(connection);
    }

    public MetricsQueryBuilder withLimit(Long limit) {
        this.limit = limit;
        return this;
    }

    public MetricsQueryBuilder withLimit(Long limit, long defaultValue) {
        this.limit = (limit != null) ? limit : defaultValue;
        return this;
    }

    public MetricsQueryBuilder withLimit(Long limit, long defaultValue, long maxValue) {

        this.limit = (limit != null) ? limit : defaultValue;
        if (this.limit > maxValue) {
            this.limit = maxValue;
        }

        return this;
    }

    public MetricsQueryBuilder withTimeRange(String timeField, GrafanaQueryRequest.Range range) {
        this.timeField = timeField;
        this.range = range;
        return this;
    }

    public MetricsQueryBuilder withJsonConditon(String jsonConditon) {
        this.jsonCondition = jsonConditon;
        return this;
    }

    /**
     * Constructs and returns non-built {@link org.ojai.store.Query}.
     *
     * @return non-built {@link org.ojai.store.Query}
     */
    public Query constructQuery() {

        boolean rangeSpecified = this.range != null && this.timeField != null && !this.timeField.isEmpty();
        Optional<QueryCondition> userSpecifiedCondition = parseJsonCondition(this.jsonCondition);

        Optional<QueryCondition> condition;
        if (rangeSpecified && userSpecifiedCondition.isPresent()) { // range and condition are specified
            condition = Optional.of(
                    connection.newCondition()
                            .and()
                            .condition(timeRangeCondition(connection, this.timeField, this.range).build())
                            .condition(userSpecifiedCondition.get())
                            .close()
                            .build()
            );
        } else if (!rangeSpecified && userSpecifiedCondition.isPresent()) { // only condition is specified
            condition = Optional.of(userSpecifiedCondition.get());
        } else if (rangeSpecified) { // only range is specified
            condition = Optional.of(timeRangeCondition(connection, this.timeField, this.range).build());
        } else { // neither range nor condition are specified
            condition = Optional.empty();
        }

        Query query = (condition.isPresent())
                ? this.connection.newQuery().where(condition.get())
                : this.connection.newQuery();

        if (this.limit != null) {
            query.limit(this.limit);
        }

        return query;
    }

    private Optional<QueryCondition> parseJsonCondition(String jsonCondition) {

        if (this.jsonCondition == null || this.jsonCondition.isEmpty()) {
            return Optional.empty();
        }

        try {
            return Optional.of(new ConditionParser().parseCondition(jsonCondition));
        } catch (DecodingException e) {
            log.warn("Can not decode OJAI JSON condition from : '{}'", jsonCondition);
        } catch (Exception e) {
            log.warn("Exception occurred while parsing JSON OJAI condition: " + jsonCondition, e);
        }

        return Optional.empty();
    }

    /**
     * At the time of query construction we don't know actual type of the specified field path values. Field path can
     * point to the ODate, OTime, OTimestamp, so we construct query to cover all of these.
     * TODO  add support pattern of non-ojai types using time pattern parameter.
     *
     * @param connection OJAI connection.
     * @param fieldPath  field path of time field.
     * @param range      time range.
     * @return non-built query condition, which can be used to querying documents within the specified time range.
     */
    private QueryCondition timeRangeCondition(Connection connection, String fieldPath, GrafanaQueryRequest.Range range) {
        return connection.newCondition()
                .or()
                .condition(
                        rangeOTimestampCondition(connection, fieldPath, range).build()
                )
                .condition(
                        rangeOTimeCondition(connection, fieldPath, range).build()
                )
                .condition(
                        rangeODateCondition(connection, fieldPath, range).build()
                )
                .close();
    }

    private QueryCondition rangeODateCondition(Connection connection, String fieldPath, GrafanaQueryRequest.Range range) {
        QueryCondition from = dateCondition(connection, fieldPath, GREATER_OR_EQUAL, range.getFrom());
        QueryCondition to = dateCondition(connection, fieldPath, LESS_OR_EQUAL, range.getTo());
        return rangeOjaiCondition(connection, from, to);
    }

    private QueryCondition rangeOTimeCondition(Connection connection, String fieldPath, GrafanaQueryRequest.Range range) {
        QueryCondition from = timeCondition(connection, fieldPath, GREATER_OR_EQUAL, range.getFrom());
        QueryCondition to = timeCondition(connection, fieldPath, LESS_OR_EQUAL, range.getTo());
        return rangeOjaiCondition(connection, from, to);
    }

    private QueryCondition rangeOTimestampCondition(Connection connection, String fieldPath, GrafanaQueryRequest.Range range) {
        QueryCondition from = timestampCondition(connection, fieldPath, GREATER_OR_EQUAL, range.getFrom());
        QueryCondition to = timestampCondition(connection, fieldPath, LESS_OR_EQUAL, range.getTo());
        return rangeOjaiCondition(connection, from, to);
    }

    private QueryCondition rangeOjaiCondition(Connection connection, QueryCondition from, QueryCondition to) {
        return connection.newCondition()
                .and()
                .condition(
                        from.build()
                )
                .condition(
                        to.build()
                )
                .close();
    }

    private QueryCondition timeCondition(Connection connection, String field, QueryCondition.Op op, Date date) {
        return connection.newCondition().is(field, op, new OTime(date.getTime()));
    }

    private QueryCondition timestampCondition(Connection connection, String field, QueryCondition.Op op, Date date) {
        return connection.newCondition().is(field, op, new OTimestamp(date.getTime()));
    }

    private QueryCondition dateCondition(Connection connection, String field, QueryCondition.Op op, Date date) {
        return connection.newCondition().is(field, op, new ODate(date.getTime()));
    }


}
