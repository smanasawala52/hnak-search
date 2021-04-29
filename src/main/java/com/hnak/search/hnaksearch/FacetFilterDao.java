package com.hnak.search.hnaksearch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hnak.search.modal.response.FacetResponseModal;
import com.hnak.search.modal.response.KeywordFilterResponseModal;
import com.hnak.search.modal.response.SearchResponseModal;
import io.micrometer.core.lang.NonNull;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

@Repository
public class FacetFilterDao {
    static Logger log = LoggerFactory.getLogger(FacetFilterDao.class);

    @Autowired
    ObjectMapper objectMapper;
    ExecutorService executorService = Executors.newFixedThreadPool(30);
    @Autowired
    private NamedParameterJdbcTemplate namedJdbcTemplate;

    public void loadNames(SearchResponseModal responseModal, @NonNull String langParam) {
        final StringBuilder lang = new StringBuilder();
        if (langParam.equalsIgnoreCase("en"))
            lang.append("en-SA");
        else if (langParam.equalsIgnoreCase("ar"))
            lang.append("ar-SA");

        final Map<String, String> facets = new HashMap<>();
        final Map<String, String> filters = new HashMap<>();

        if (CollectionUtils.isNotEmpty(responseModal.getFacets()))
            responseModal.getFacets().stream()
                    .filter(facetResponseModal -> CollectionUtils.isNotEmpty(facetResponseModal.getKeywordFilters()))
                    .forEach(facetResponseModal -> {

                        if (StringUtils.isNotBlank(facetResponseModal.getFacetCode()))
                            facets.put(facetResponseModal.getFacetCode(), null);

                        if (CollectionUtils.isNotEmpty(facetResponseModal.getKeywordFilters()))
                            facetResponseModal.getKeywordFilters().stream()
                                    .filter(keywordFilterResponseModal -> StringUtils.isNotBlank(keywordFilterResponseModal.getFilterCode()))
                                    .forEach(keywordFilterResponseModal -> {
                                        filters.put(keywordFilterResponseModal.getFilterCode(), null);
                                    });

                    });

        List<Callable<String>> callableTasks = new ArrayList<>();

        if (!facets.isEmpty() && CollectionUtils.isNotEmpty(facets.keySet())) {
            Callable<String> task = () -> {
                Map<String, String> f = loadFacetNames(facets, lang.toString());
                if (f != null && !f.isEmpty())
                    facets.putAll(f);

                return "facetResults";
            };
            callableTasks.add(task);
        }
        if (!filters.isEmpty() && CollectionUtils.isNotEmpty(filters.keySet())) {
            Callable<String> task1 = () -> {
                Map<String, String> fil = loadFilterNames(filters, lang.toString());
                if (fil != null && !fil.isEmpty())
                    filters.putAll(fil);
                return "filterResults";
            };
            callableTasks.add(task1);
        }

        List<Future<String>> results = null;
        try {
            results = executorService.invokeAll(callableTasks);

            results.forEach(result -> {
                try {
                    result.get();
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            });

            final Set<FacetResponseModal> finalFacets = new HashSet<>();
            responseModal.getFacets().stream()
                    .filter(facetResponseModal -> CollectionUtils.isEmpty(facetResponseModal.getKeywordFilters()))
                    .forEach(facetResponseModal -> finalFacets.add(facetResponseModal));

            responseModal.getFacets().stream()
                    .filter(facetResponseModal -> facets.containsKey(facetResponseModal.getFacetCode())
                            && StringUtils.isNotBlank(facets.get(facetResponseModal.getFacetCode())))
                    .forEach(facetResponseModal -> {
                        facetResponseModal.setFacet(facets.get(facetResponseModal.getFacetCode()));

                        if (CollectionUtils.isNotEmpty(facetResponseModal.getKeywordFilters())) {
                            final Set<KeywordFilterResponseModal> finalFilters = new HashSet<>();
                            facetResponseModal.getKeywordFilters().stream()
                                    .filter(filterModal -> filters.containsKey(filterModal.getFilterCode())
                                            && StringUtils.isNotBlank(filters.get(filterModal.getFilterCode())))
                                    .forEach(filterModal -> {
                                        filterModal.setFilter(filters.get(filterModal.getFilterCode()));
                                        finalFilters.add(filterModal);
                                    });
                            facetResponseModal.setKeywordFilters(finalFilters);
                        }
                        finalFacets.add(facetResponseModal);
                    });
            responseModal.setFacets(finalFacets);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private Map<String, String> loadFacetNames(Map<String, String> facets, @NonNull String lang) {
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("facets", facets.keySet());

        namedJdbcTemplate.query(
                "select code, name from hnak.attribute where datatype = 'list' and active = 1 and " +
                        "filterable = 1 and code in (:facets) ",
                parameters, (rs, rowNum) -> {
                    String nameJson = retriveValue(rs.getString("name"), lang);
                    facets.put(rs.getString("code"), nameJson);
                    return null;
                });
        return facets;
    }

    private Map<String, String> loadFilterNames(Map<String, String> filters, @NonNull String lang) {
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("filters", filters.keySet());

        namedJdbcTemplate.query(
                "select id, name from hnak.attribute_value where id in (:filters) ",
                parameters, (rs, rowNum) -> {
                    String nameJson = retriveValue(rs.getString("name"), lang);
                    filters.put(rs.getString("id"), nameJson);
                    return null;
                });
        return filters;
    }

    public Map<String, Integer> loadFilterCodes(Set<String> filters) {
        Map<String, Integer> finalFiltersMap = new HashMap<>();
        Set<String> finalFilters = filters.stream()
                .filter(s -> StringUtils.isNotBlank(s))
                .map(s -> s.trim().toLowerCase())
                .collect(Collectors.toSet());

        if (CollectionUtils.isNotEmpty(finalFilters)) {
            MapSqlParameterSource parameters = new MapSqlParameterSource();
            parameters.addValue("filters", finalFilters);

            namedJdbcTemplate.query(
                    "select legacy_id, id from hnak.attribute_value where legacy_id in (:filters) ",
                    parameters, (rs, rowNum) -> {
                        finalFiltersMap.put(rs.getString("legacy_id"), rs.getInt("id"));
                        return null;
                    });
        }
        return finalFiltersMap;
    }

    private String retriveValue(String jsonValue, String lang) {
        if (StringUtils.isEmpty(jsonValue)) return StringUtils.EMPTY;
        try {
            Map urlMap = objectMapper.readValue(jsonValue, Map.class);

            if (urlMap.get(lang) != null)
                return ((String) urlMap.get(lang)).trim();

        } catch (Exception e) {
            return jsonValue;
        }

        return StringUtils.EMPTY;
    }
}
