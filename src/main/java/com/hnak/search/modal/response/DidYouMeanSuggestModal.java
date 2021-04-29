package com.hnak.search.modal.response;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

public class DidYouMeanSuggestModal {
    String originalText;
    Set suggestions = new LinkedHashSet();

    public String getOriginalText() {
        return originalText;
    }

    public void setOriginalText(String originalText) {
        this.originalText = originalText;
    }

    public Set getSuggestions() {
        return suggestions;
    }

    public void setSuggestions(Set suggestions) {
        this.suggestions = suggestions;
    }
}
