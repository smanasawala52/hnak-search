package com.hnak.search.modal.request;

import org.springframework.lang.NonNull;

public class NumberFacetModal {
    @NonNull
    String facet;
    @NonNull
    int from;
    @NonNull
    int to;
    boolean histogram;
    private int interval;

    public String getFacet() {
        return facet;
    }

    public void setFacet(String facet) {
        this.facet = facet;
    }

    public int getFrom() {
        return from;
    }

    public void setFrom(int from) {
        this.from = from;
    }

    public int getTo() {
        return to;
    }

    public void setTo(int to) {
        this.to = to;
    }

    public boolean isHistogram() {
        return histogram;
    }

    public void setHistogram(boolean histogram) {
        this.histogram = histogram;
    }

    public int getInterval() {
        return interval;
    }

    public void setInterval(int interval) {
        this.interval = interval;
    }
}
