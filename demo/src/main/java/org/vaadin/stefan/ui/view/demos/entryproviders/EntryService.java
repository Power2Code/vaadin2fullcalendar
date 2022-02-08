package org.vaadin.stefan.ui.view.demos.entryproviders;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.vaadin.stefan.fullcalendar.Entry;
import org.vaadin.stefan.fullcalendar.dataprovider.EntryQuery;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Stream;

/**
 * @author Stefan Uebe
 */
public class EntryService {

    private final Map<String, EntryData> database = new HashMap<>();

    public static EntryService createInstance() {
        return new EntryService();
    }

    public EntryService() {
        LocalDate date = LocalDate.now().minusYears(3).withDayOfYear(1);
        LocalDate end = LocalDate.now().plusYears(3);

        Random random = new Random();

        while (date.isBefore(end)) {
            int maxDays = date.lengthOfMonth();
            for (int i = 0; i < 8; i++) {
                LocalDate start = date.withDayOfMonth(random.nextInt(maxDays) + 1);
                createAt(start, random.nextBoolean());
            }

            date = date.plusMonths(1);
        }
    }

    public Optional<Entry> getEntry(String id) {
        return Optional.ofNullable(database.get(id)).map(this::toEntry);
    }

    public int count() {
        return database.size();
    }

    private void createAt(LocalDate date, boolean allDay) {
        if (allDay) {
            EntryData day = new EntryData("" + database.size(), "Day event " + database.size(), date.atStartOfDay(), date.plusDays(1).atStartOfDay(), true);
            database.put(day.getId(), day);
        } else {
            EntryData time = new EntryData("" + database.size(), "Time event " + database.size(), date.atStartOfDay(), date.atTime(10, 0), false);
            database.put(time.getId(), time);
        }
    }

    public Stream<Entry> streamEntries() {
        return database.values().stream().map(this::toEntry);
    }

    public Stream<Entry> streamEntries(EntryQuery query) {
        Stream<EntryData> stream = database.values().stream();
        return applyFilter(stream, query).map(this::toEntry)/*.peek(e -> e.setCalendar(query.getSource()))*/;
    }

    public Stream<EntryData> applyFilter(Stream<EntryData> stream, EntryQuery query) {
        LocalDateTime start = query.getStart();
        LocalDateTime end = query.getEnd();

        if (start == null && end == null) {
            return stream;
        }

        stream = stream.filter(item -> {
            if (end != null && item.getStart().isAfter(end)) {
                return false;
            }

            return !(start != null && item.getEnd().isBefore(start));
        });

        return stream;
    }

    public Entry toEntry(EntryData entryData) {
        Entry entry = new Entry();
        entry.setTitle(entryData.getTitle());
        entry.setStart(entryData.getStart());
        entry.setEnd(entryData.getEnd());
        entry.setAllDay(entryData.isAllDay());
        return entry;
    }

    public void addEntries(List<Entry> entries) {
        entries.forEach(entry -> database.put(entry.getId(), new EntryData(entry.getId(), entry.getTitle(), entry.getStart(), entry.getEnd(), entry.isAllDay())));
    }

    public void removeAll() {
        database.clear();
    }

    @Data
    @AllArgsConstructor
    private static class EntryData {
        private final String id;
        private String title;
        private LocalDateTime start;
        private LocalDateTime end;
        private boolean allDay;
    }
}