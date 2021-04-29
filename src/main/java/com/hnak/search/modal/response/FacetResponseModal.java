package com.hnak.search.modal.response;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class FacetResponseModal {
	String facet;
	String facetCode;
	@JsonIgnore
	String cat;
	Set<KeywordFilterResponseModal> keywordFilters = new HashSet<KeywordFilterResponseModal>();
	Set<NumberFilterResponseModal> numberFilters = new HashSet<NumberFilterResponseModal>();
	Set<NumberFilterResponseModal> histogramFilters = new HashSet<NumberFilterResponseModal>();

	public String getFacet() {
		return facet;
	}

	public FacetResponseModal(String facet) {
		this.facet = facet;
	}

	public String getFacetCode() {
		return facetCode;
	}

	public void setFacetCode(String facetCode) {
		this.facetCode = facetCode;
	}

	public void setFacet(String facet) {
		this.facet = facet;
	}

	public FacetResponseModal() {
		// TODO Auto-generated constructor stub
	}

	public Set<KeywordFilterResponseModal> getKeywordFilters() {
		return keywordFilters;
	}

	public void setKeywordFilters(Set<KeywordFilterResponseModal> keywordFilters) {
		this.keywordFilters = keywordFilters;
	}

	public String getCat() {
		return cat;
	}

	public void setCat(String cat) {
		this.cat = cat;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		FacetResponseModal that = (FacetResponseModal) o;
		return facet.equals(that.facet);
	}

	@Override
	public int hashCode() {
		return Objects.hash(facet, cat);
	}

	public Set<NumberFilterResponseModal> getNumberFilters() {
		return numberFilters;
	}

	public void setNumberFilters(Set<NumberFilterResponseModal> numberFilters) {
		this.numberFilters = numberFilters;
	}

	public Set<NumberFilterResponseModal> getHistogramFilters() {
		return histogramFilters;
	}

	public void setHistogramFilters(Set<NumberFilterResponseModal> histogramFilters) {
		this.histogramFilters = histogramFilters;
	}
}
