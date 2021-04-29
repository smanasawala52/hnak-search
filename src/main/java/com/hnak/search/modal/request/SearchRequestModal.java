package com.hnak.search.modal.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hnak.search.modal.response.CategoryResponseModal;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SearchRequestModal {
    @JsonProperty("pids")
    String[] products;
    @JsonProperty("q")
    String q;
    @JsonProperty("gb")
    String globalBrand;
    @JsonProperty("c")
    String cat;
    @JsonProperty("as")
    String attributeSet;
    @JsonProperty("l")
    String lang;
    @JsonProperty("p")
    PaginationModal pagination = new PaginationModal();
    @JsonProperty("keywordFacets")
    List<KeywordFacetModal> keywordFacets = new ArrayList<KeywordFacetModal>();
    @JsonProperty("numberFacets")
    Set<NumberFacetModal> numberFacetModals = new HashSet<>();
    @JsonProperty("s")
    SortModal sortModal;
    @JsonProperty("fs")
    int filterSize;
    String version = StringUtils.EMPTY;
    CategoryResponseModal currCat = new CategoryResponseModal();

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String[] getProducts() {
        return products;
    }

    public void setProducts(String[] products) {
        this.products = products;
    }

    public String getGlobalBrand() {
        return globalBrand;
    }

    public void setGlobalBrand(String globalBrand) {
        this.globalBrand = globalBrand;
    }

    public String getAttributeSet() {
		return attributeSet;
	}

	public void setAttributeSet(String attributeSet) {
		this.attributeSet = attributeSet;
	}

	public SearchRequestModal() {
    }

    public SearchRequestModal(String q, String cat, List<KeywordFacetModal> pFacetFilters, Set<NumberFacetModal> numberFacetModals, SortModal sortModal) {
        super();
        this.q = q;
        this.cat = cat;

        for (KeywordFacetModal facetFilter : pFacetFilters) {
            this.keywordFacets.add(facetFilter);
        }
        this.sortModal = sortModal;
        this.numberFacetModals = numberFacetModals;
    }

    public SearchRequestModal(String q, String cat, List<KeywordFacetModal> pFacetFilters) {
        super();
        this.q = q;
        this.cat = cat;

        for (KeywordFacetModal facetFilter : pFacetFilters) {
            this.keywordFacets.add(facetFilter);
        }
    }

    public int getFilterSize() {
        return filterSize;
    }

    public void setFilterSize(int filterSize) {
        this.filterSize = filterSize;
    }

    public String getQ() {
        return q;
    }

    public void setQ(String q) {
        this.q = q;
    }

    public String getCat() {
        return cat;
    }

    public void setCat(String cat) {
        this.cat = cat;
    }

    public List<KeywordFacetModal> getKeywordFacets() {
        return keywordFacets;
    }

    public void setKeywordFacets(List<KeywordFacetModal> keywordFacets) {
        this.keywordFacets = keywordFacets;
    }

    public CategoryResponseModal getCurrCat() {
        return currCat;
    }

    public void setCurrCat(CategoryResponseModal currCat) {
        this.currCat = currCat;
    }

    /**
     * @return the pagination
     */
    public PaginationModal getPagination() {
        return pagination;
    }

    /**
     * @param pagination the pagination to set
     */
    public void setPagination(PaginationModal pagination) {
        this.pagination = pagination;
    }

    public Set<NumberFacetModal> getNumberFacetModals() {
        return numberFacetModals;
    }

    public void setNumberFacetModals(Set<NumberFacetModal> numberFacetModals) {
        this.numberFacetModals = numberFacetModals;
    }

    public SortModal getSortModal() {
        return sortModal;
    }

    public void setSortModal(SortModal sortModal) {
        this.sortModal = sortModal;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }
}
