package com.mapr.grafana.plugin.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import javax.validation.constraints.NotNull;
import java.util.Set;

/**
 * TODO
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GrafanaQueryTarget {

    public static final String DOCUMENT_COUNT_METRIC = "Document count";
    public static final String FIELD_VALUE_METRIC = "Field value";
    public static final String FIELD_MIN_METRIC = "Field min";
    public static final String FIELD_MAX_METRIC = "Field max";
    public static final String FIELD_AVG_METRIC = "Field avg";

    public static final String RAW_DOCUMENT_TYPE = "Raw Document";
    public static final String TIME_SERIES_TYPE = "Timeseries";

    @NotNull
    private String refId;

    @NotNull
    private String type;

    @NotNull
    private String table;


    /**
     * JSON condition, equivalent to '--c, --where' or '$where' sub-command of the MapR DBShell '--query' option.<br/><br/>
     * Examples of some valid values:<br/>
     * <code>{"$condition": {"$gt": {"rating": 3.7}}}</code><br/>
     * <code>{"$where":{"$and":[{"$eq":{"status":"Promotion"}},{"$or":[{"$ne":{"packaging":""}},{"$gt":{"rating":3}},{"$lt":{"rating":5}}]}]}}</code><br/>
     * <p>
     * Note: in examples above escaping of " character is omitted for brevity.
     *
     * @see <a href="https://maprdocs.mapr.com/60/ReferenceGuide/dbshell-find-findbyid.html">MapR Docs: dbshell 'find' or 'findbyid'</a>
     * @see <a href="https://maprdocs.mapr.com/60/ReferenceGuide/dbshell-find-query.html">MapR Docs: dbshell query with '--query' option</a>
     * @see <a href="https://maprdocs.mapr.com/60/ReferenceGuide/dbshell-find-examples.html">MapR Docs: dbshell query examples </a>
     */
    private String condition;

    private long limit;

    private String timeField;

    private Set<String> selectFields;

    private String target;

    private String metricField;

    private String metric;

    public String getRefId() {
        return refId;
    }

    public void setRefId(String refId) {
        this.refId = refId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTable() {
        return table;
    }

    public void setTable(String table) {
        this.table = table;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public long getLimit() {
        return limit;
    }

    public void setLimit(long limit) {
        this.limit = limit;
    }

    public String getTimeField() {
        return timeField;
    }

    public void setTimeField(String timeField) {
        this.timeField = timeField;
    }

    public Set<String> getSelectFields() {
        return selectFields;
    }

    public void setSelectFields(Set<String> selectFields) {
        this.selectFields = selectFields;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getMetricField() {
        return metricField;
    }

    public void setMetricField(String metricField) {
        this.metricField = metricField;
    }

    public String getMetric() {
        return metric;
    }

    public void setMetric(String metric) {
        this.metric = metric;
    }

    @Override
    public String toString() {
        return "GrafanaQueryTarget{" +
                "refId='" + refId + '\'' +
                ", type='" + type + '\'' +
                ", table='" + table + '\'' +
                ", condition='" + condition + '\'' +
                ", limit=" + limit +
                ", timeField='" + timeField + '\'' +
                ", selectFields=" + selectFields +
                ", target='" + target + '\'' +
                ", metricField='" + metricField + '\'' +
                ", metric='" + metric + '\'' +
                '}';
    }
}
