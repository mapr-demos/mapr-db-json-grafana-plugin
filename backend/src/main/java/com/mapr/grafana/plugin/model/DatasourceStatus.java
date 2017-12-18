package com.mapr.grafana.plugin.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Arrays;

/**
 * TODO doc
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DatasourceStatus {

    @JsonProperty("is_ok")
    private boolean ok;

    private String[] errors;

    private DatasourceStatus(boolean ok, String... errors) {
        this.ok = ok;
        this.errors = errors;
    }

    public static DatasourceStatus ok() {
        return new DatasourceStatus(true, null);
    }

    public static DatasourceStatus error(String... errors) {
        return new DatasourceStatus(false, errors);
    }

    public static DatasourceStatus error(String description, Throwable t) {
        return error(description, t.getMessage());
    }

    public static DatasourceStatus error(Throwable t) {
        return error(t.getMessage());
    }

    public boolean isOk() {
        return ok;
    }

    public void setOk(boolean ok) {
        this.ok = ok;
    }

    public String[] getErrors() {
        return errors;
    }

    public void setErrors(String[] errors) {
        this.errors = errors;
    }

    @Override
    public String toString() {
        return "DatasourceStatus{" +
                "ok=" + ok +
                ", error=" + Arrays.toString(errors) +
                '}';
    }
}
