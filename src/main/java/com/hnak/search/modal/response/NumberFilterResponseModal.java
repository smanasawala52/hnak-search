package com.hnak.search.modal.response;

import java.util.Objects;

public class NumberFilterResponseModal extends KeywordFilterResponseModal {
    private int to;
    private int from;

    public NumberFilterResponseModal(String filter, long count, int to, int from) {
        super(filter, count);
        this.to = to;
        this.from = from;
    }

    public int getTo() {
        return to;
    }

    public void setTo(int to) {
        this.to = to;
    }

    public int getFrom() {
        return from;
    }

    public void setFrom(int from) {
        this.from = from;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        NumberFilterResponseModal that = (NumberFilterResponseModal) o;
        return super.filter.equals(that.filter);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), to, from);
    }
}
