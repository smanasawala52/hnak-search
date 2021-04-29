package com.hnak.search.modal.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hnak.search.modal.request.CategoryModal;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CategoryResponseModal {
	CategoryModal currentCat;
	Set<CategoryModal> children = new HashSet<>();
	@JsonIgnore
	Set<String> attributeSets = new HashSet<>();
	Set<CategoryModal> parent = new HashSet<CategoryModal>();
	private String bottomText;

	public String getBottomText() {
		return bottomText;
	}

	public void setBottomText(String bottomText) {
		this.bottomText = bottomText;
	}

	public CategoryResponseModal() {}

	public Set<String> getAttributeSets() {
		return attributeSets;
	}

	public void setAttributeSets(Set<String> attributeSets) {
		this.attributeSets = attributeSets;
	}

	public CategoryModal getCurrentCat() {
		return currentCat;
	}

	public void setCurrentCat(CategoryModal currentCat) {
		this.currentCat = currentCat;
	}

	public Set<CategoryModal> getChildren() {
		return children;
	}

	public void setChildren(Set<CategoryModal> children) {
		this.children = children;
	}

	public Set<CategoryModal> getParent() {
		return parent;
	}

	public void setParent(Set<CategoryModal> parent) {
		this.parent = parent;
	}

	@Override
	public String toString() {
		return "CategoryUIModal [currentCat=" + currentCat + ", children=" + children + ", parent=" + parent + "]";
	}
	
	
	
}
