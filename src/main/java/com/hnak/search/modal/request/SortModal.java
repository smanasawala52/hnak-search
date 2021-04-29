package com.hnak.search.modal.request;

import org.elasticsearch.search.sort.SortOrder;

public class SortModal {
    String facet;
    String direction;

    public SortModal() {
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public SortModal(String facet, String direction) {
        this.facet = facet;
        this.direction = direction;
    }

    public String getFacet() {
        return facet;
    }

    public void setFacet(String facet) {
        this.facet = facet;
    }


}
