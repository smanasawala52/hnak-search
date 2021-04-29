package com.hnak.search.hnaksearch;

import com.hnak.search.modal.request.AutoCompleteModal;
import com.hnak.search.modal.request.SearchRequestModal;
import com.hnak.search.modal.response.SearchResponseModal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
public class SearchController {

    @Autowired
    FacetFilterDao facetFilterDao;
    @Autowired
    private SearchFacade searchFacade;

    @PostMapping("/search/rest")
    @ResponseBody
    public SearchResponseModal searchRest(@RequestBody SearchRequestModal modal) {
        if (modal != null)
            return searchFacade.search(modal);
        return new SearchResponseModal();
    }

    @PostMapping("/search/rest/v1")
    @ResponseBody
    public SearchResponseModal searchRestV1(@RequestBody SearchRequestModal modal) {
        if (modal != null) {
        	modal.setVersion("v1");
			return searchFacade.search(modal);
		}
        return new SearchResponseModal();
    }

    @PostMapping("/search/filterCodes")
    @ResponseBody
    public Map<String, Integer> getFilterCodes(@RequestBody Set<String> filters) {
        return facetFilterDao.loadFilterCodes(filters);
    }

    @GetMapping("/search/auto")
    @ResponseBody
    public AutoCompleteModal autoComplete(@RequestParam String q) {
        List<String> suggestions = new ArrayList<String>();


        List<String> finalSuggest = new ArrayList<String>();
        suggestions.stream().forEach(s -> {
            if (s.toLowerCase().contains(q.toLowerCase()))
                finalSuggest.add(s);
        });

        System.out.println("suggesting" + finalSuggest);
        return new AutoCompleteModal(finalSuggest);
    }
}
