package com.hnak.search.modal.request;


public class KeywordFacetModal {
	String facet;
	String filter;

	KeywordFacetModal(){}

	public KeywordFacetModal(String facet, String filter) {
		super();
		this.facet = facet;
		this.filter = filter;
	}

	public String getFacet() {
		return facet;
	}

	public void setFacet(String facet) {
		this.facet = facet;
	}

	public String getFilter() {
		return filter;
	}

	public void setFilter(String filter) {
		this.filter = filter;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((facet == null) ? 0 : facet.hashCode());
		result = prime * result + ((filter == null) ? 0 : filter.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		KeywordFacetModal other = (KeywordFacetModal) obj;
		
		return (facet != null && other.facet != null && facet.equals(other.facet) && filter != null && other.filter != null && filter.equals(other.filter));
	}
}