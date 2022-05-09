package com.rd.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class HistoryRecord {
    public static final String HISTORY_RECORD_DELEMITER = ", Visited On ";

    private String url;
    private String date;

    @Override
    public String toString() {
        return url + HISTORY_RECORD_DELEMITER + date;
    }

    @Override
    public boolean equals(Object o) { // records are equal if they have the same url
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HistoryRecord that = (HistoryRecord) o;
        return url.equals(that.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url);
    }
}
