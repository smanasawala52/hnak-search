package com.hnak.search.modal.request;

import java.util.List;

public class AutoCompleteModal {
	private List<String> suggestions;

	public AutoCompleteModal(List<String> suggestions) {
		super();
		this.suggestions = suggestions;
	}

	public List<String> getSuggestions() {
		return suggestions;
	}

	public void setSuggestions(List<String> suggestions) {
		this.suggestions = suggestions;
	}
}
