package com.hnak.search.modal.response;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.*;

public class SearchResponseModal {
	CategoryResponseModal categoryResponseModal = new CategoryResponseModal();
	List<Map<String, Object>> products = new ArrayList<Map<String, Object>>();;
	Set<FacetResponseModal> facets = new HashSet<FacetResponseModal>();
	SearchErrorResponseModal errorModal;
	DidYouMeanSuggestModal didYouMean = new DidYouMeanSuggestModal();
	long totalResults;

	public CategoryResponseModal getCategoryResponseModal() {
		return categoryResponseModal;
	}

	public DidYouMeanSuggestModal getDidYouMean() {
		return didYouMean;
	}

	public void setCategoryResponseModal(CategoryResponseModal categoryResponseModal) {
		this.categoryResponseModal = categoryResponseModal;
	}

	public List<Map<String, Object>> getProducts() {
		return products;
	}

	public void setProducts(List<Map<String, Object>> products) {
		this.products = products;
	}

	public Set<FacetResponseModal> getFacets() {
		return facets;
	}

	public void setFacets(Set<FacetResponseModal> facets) {
		this.facets = facets;
	}

	public SearchErrorResponseModal getErrorModal() {
		return errorModal;
	}

	public void setErrorModal(SearchErrorResponseModal errorModal) {
		this.errorModal = errorModal;
	}

	/**
	 * @return the totalResults
	 */
	public long getTotalResults() {
		return totalResults;
	}

	/**
	 * @param value
	 *            the totalResults to set
	 */
	public void setTotalResults(long value) {
		this.totalResults = value;
	}
}
