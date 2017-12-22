package com.mapr.grafana.plugin.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.ojai.types.ODate;
import org.ojai.types.OTime;
import org.springframework.context.annotation.Bean;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.function.Function;

@Component
public class JacksonConfig {

    /**
     * Serializer for OJAI date types: {@link ODate}, {@link OTime}, {@link org.ojai.types.OTimestamp}. Serializes
     * instances of these classes to single timestamp value.
     *
     * @param <T> - OJAI date type, such as {@link ODate}, {@link OTime}, {@link org.ojai.types.OTimestamp}.
     */
    private class OjaiDateSerializer<T> extends JsonSerializer<T> {

        Function<T, Long> ojaiDateToEpoch;

        OjaiDateSerializer(Function<T, Long> ojaiDateToEpoch) {
            this.ojaiDateToEpoch = ojaiDateToEpoch;
        }

        @Override
        public void serialize(T ojaiDate, JsonGenerator jsonGenerator, SerializerProvider provider) throws IOException {
            Long epoch = ojaiDateToEpoch.apply(ojaiDate);
            if (epoch == null) {
                jsonGenerator.writeNull();
            } else {
                jsonGenerator.writeNumber(epoch);
            }
        }
    }

    @Bean
    public Jackson2ObjectMapperBuilder objectMapperBuilder() {

        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();

        builder.serializerByType(ODate.class,
                new OjaiDateSerializer<ODate>(oDate -> oDate == null ? null : oDate.toDate().getTime()));

        builder.serializerByType(OTime.class,
                new OjaiDateSerializer<OTime>(oTime -> oTime == null ? null : oTime.toDate().getTime()));

        builder.serializerByType(OTime.class,
                new OjaiDateSerializer<OTime>(oTimestamp -> oTimestamp == null ? null : oTimestamp.toDate().getTime()));

        return builder;
    }

}
