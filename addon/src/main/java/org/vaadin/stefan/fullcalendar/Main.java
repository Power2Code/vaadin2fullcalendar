package org.vaadin.stefan.fullcalendar;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.json.JsonWriteFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.time.*;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;

/**
 * @author Stefan Uebe
 */
public class Main {

    public static final LocalDateTime DEFAULT_START = LocalDate.of(2000, 1, 1).atStartOfDay();
    public static final Instant DEFAULT_START_UTC = DEFAULT_START.toInstant(ZoneOffset.UTC);
    public static final LocalDateTime DEFAULT_END = DEFAULT_START.plusDays(1);
    public static final Instant DEFAULT_END_UTC = DEFAULT_END.toInstant(ZoneOffset.UTC);

    public static final String DEFAULT_STRING = "test";
    public static final String DEFAULT_ID = DEFAULT_STRING + 1;
    public static final String DEFAULT_TITLE = DEFAULT_STRING + 2;
    public static final String DEFAULT_COLOR = DEFAULT_STRING + 3;
    public static final String DEFAULT_DESCRIPTION = DEFAULT_STRING + 4;
    public static final Entry.RenderingMode DEFAULT_RENDERING = Entry.RenderingMode.BACKGROUND;
    public static final Timezone CUSTOM_TIMEZONE = new Timezone(ZoneId.of("Europe/Berlin"));

    public static void main(String[] args) throws JsonProcessingException {
        simpleConversion();
        updateExisting();
    }

    private static void simpleConversion() throws JsonProcessingException {
        System.out.println();
        System.out.println(" === simple conversion ===");
        System.out.println();
        Entry entry = new Entry();

        ObjectMapper mapper = createMapper();
        String json = mapper.writeValueAsString(entry);

        System.out.println(" = json =");

        System.out.println(json);


        System.out.println(" = java =");

        System.out.println(mapper.readValue(json, Entry.class));
    }

    private static void updateExisting() throws JsonProcessingException {
        System.out.println();
        System.out.println(" === update existing ===");
        System.out.println();


        FullCalendar calendar = new FullCalendar();
        calendar.setTimezone(CUSTOM_TIMEZONE);
        Entry toUpdate = new Entry(DEFAULT_ID);
        toUpdate.setTitle(DEFAULT_TITLE);
        toUpdate.setStart(DEFAULT_START_UTC);
        toUpdate.setEnd(DEFAULT_END_UTC);
        toUpdate.setAllDay(true);
        toUpdate.setEditable(true);
        toUpdate.setColor(DEFAULT_COLOR);
        toUpdate.setDescription(DEFAULT_DESCRIPTION);
        toUpdate.setRenderingMode(DEFAULT_RENDERING);
        toUpdate.setCalendar(calendar);

        Entry updater = new Entry(DEFAULT_ID);
        updater.setTitle("IGNORE ME");
        updater.setStart(DEFAULT_START_UTC.plus( 365, ChronoUnit.DAYS));
        updater.setEnd(DEFAULT_END.plus( 365, ChronoUnit.DAYS));
        updater.setEditable(false);

        ObjectMapper mapper = createMapper();
        String json = mapper.writeValueAsString(updater);

        System.out.println(" = json =");
        System.out.println(json);
        mapper.readerForUpdating(toUpdate).readValue(json);
        System.out.println(" = java =");
        System.out.println(toUpdate);
    }

    private static ObjectMapper createMapper() {
        return JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .serializationInclusion(JsonInclude.Include.NON_NULL)
                .build();
    }

}
