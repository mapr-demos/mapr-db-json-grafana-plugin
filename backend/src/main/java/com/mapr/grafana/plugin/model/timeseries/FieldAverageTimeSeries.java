package com.mapr.grafana.plugin.model.timeseries;

import org.ojai.Document;
import org.ojai.FieldPath;
import org.ojai.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * TODO document
 */
public class FieldAverageTimeSeries extends AbstractGrafanaTimeSeries {

    private static final Logger log = LoggerFactory.getLogger(FieldAverageTimeSeries.class);

    private Map<Long, List<Datapoint>> intervalDatapoints = new HashMap<>();
    private String metricFieldPath;

    public FieldAverageTimeSeries(String target, String timeFieldPath, String metricFieldPath, long intervalMs) {
        super(target, timeFieldPath, intervalMs);
        this.metricFieldPath = metricFieldPath;
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

            // First doc at interval
            if (this.datapoints.isEmpty() || timestamp - datapoints.get(datapoints.size() - 1).getTimestamp() >= intervalMs) {
                Datapoint datapoint = new Datapoint(doubleValue, timestamp);
                datapoints.add(datapoint);

                List<Datapoint> datapoints = new ArrayList<>();
                // add copy of datapoint, so the origin one can be modified safely
                datapoints.add(new Datapoint(doubleValue, timestamp));
                intervalDatapoints.put(timestamp, datapoints);

            } else if (!this.datapoints.isEmpty()) {

                Datapoint last = datapoints.get(datapoints.size() - 1);
                List<Datapoint> existingIntervalDps = intervalDatapoints.get(last.getTimestamp());
                existingIntervalDps.add(new Datapoint(doubleValue, timestamp));

                OptionalDouble average = existingIntervalDps.stream().mapToDouble(Datapoint::getValue).average();
                average.ifPresent(last::setValue);
            }

        } catch (Exception e) {
            log.debug("Exception occurred while adding OJAI document '{}' as datapoint with time field: '{}'. " +
                    "Exception: '{}'", document, timeFieldPath, e);
        }
    }

    @Override
    public String toString() {
        return "FieldAverageTimeSeries{" +
                "intervalDatapoints=" + intervalDatapoints +
                ", metricFieldPath='" + metricFieldPath + '\'' +
                ", datapoints=" + datapoints +
                ", target='" + target + '\'' +
                ", timeFieldPath='" + timeFieldPath + '\'' +
                ", intervalMs=" + intervalMs +
                '}';
    }
}
