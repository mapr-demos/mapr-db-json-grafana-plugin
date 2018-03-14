package com.mapr.grafana.plugin.model.timeseries;

import org.ojai.Document;
import org.ojai.FieldPath;
import org.ojai.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO document
 */
public class AggregationTimeSeries extends AbstractGrafanaTimeSeries {

    /**
     * TODO document
     */
    public interface IntervalAggregationFunction {
        Datapoint aggregate(Datapoint existing, Datapoint incoming);
    }

    private static final Logger log = LoggerFactory.getLogger(AggregationTimeSeries.class);

    private String metricFieldPath;
    private IntervalAggregationFunction intervalAggregationFunction;

    public AggregationTimeSeries(IntervalAggregationFunction aggregationFunction, String target, String timeFieldPath,
                                 String metricFieldPath, long intervalMs) {

        super(target, timeFieldPath, intervalMs);
        this.metricFieldPath = metricFieldPath;
        this.intervalAggregationFunction = aggregationFunction;
    }

    /**
     * Note, that since this implementation uses {@link java.util.List} as underlying collection for storing datapoints,
     * the order of method invocation is significant. Thus, documents should be added in order of timestamp increasing.
     *
     * @param document document which will be converted to datapoint.
     */
    @Override
    public void addDocument(Document document) {

        try {

            if (document == null) {
                throw new IllegalArgumentException("Document can not be null");
            }

            FieldPath path = FieldPath.parseFrom(metricFieldPath);
            Value value = document.getValue(path);

            Double doubleValue;
            if (Value.Type.STRING == value.getType()) {
                doubleValue = Double.valueOf(value.getString());
            } else {
                doubleValue = value.getDouble();
            }

            long timestamp = getDocumentTimestamp(document, timeFieldPath);
            Datapoint datapoint = new Datapoint(doubleValue, timestamp);

            if (this.datapoints.isEmpty()) {
                this.datapoints.add(datapoint);
            } else {
                Datapoint last = this.datapoints.get(this.datapoints.size() - 1);
                if (datapoint.getTimestamp() - last.getTimestamp() >= intervalMs) {
                    this.datapoints.add(datapoint);
                } else {
                    Datapoint accum = intervalAggregationFunction.aggregate(last, datapoint);
                    if (accum != null) {
                        this.datapoints.set(this.datapoints.size() - 1, accum);
                    }
                }
            }

        } catch (Exception e) {
            log.debug("Exception occurred while adding OJAI document '{}' as datapoint with metric field: '{}' " +
                    "and time field: '{}'. Exception: '{}'", document, metricFieldPath, timeFieldPath, e);
        }

    }

    @Override
    public String toString() {
        return "AggregationTimeSeries{" +
                "intervalMs=" + intervalMs +
                ", metricFieldPath='" + metricFieldPath + '\'' +
                ", intervalAggregationFunction=" + intervalAggregationFunction +
                ", datapoints=" + datapoints +
                ", target='" + target + '\'' +
                ", timeFieldPath='" + timeFieldPath + '\'' +
                '}';
    }
}
