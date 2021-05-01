package com.hnak.search.hnaksearch;

import com.hnak.search.modal.request.CategoryModal;
import com.hnak.search.modal.request.SearchRequestModal;
import com.hnak.search.modal.request.SortModal;
import com.hnak.search.modal.response.*;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.lucene.search.function.CombineFunction;
import org.elasticsearch.common.lucene.search.function.FunctionScoreQuery;
import org.elasticsearch.index.query.*;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder.FilterFunctionBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.ParsedSingleBucketAggregation;
import org.elasticsearch.search.aggregations.bucket.filter.ParsedFilter;
import org.elasticsearch.search.aggregations.bucket.histogram.ParsedHistogram;
import org.elasticsearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.metrics.ParsedStats;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.search.suggest.SortBy;
import org.elasticsearch.search.suggest.Suggest;
import org.elasticsearch.search.suggest.SuggestBuilder;
import org.elasticsearch.search.suggest.SuggestBuilders;
import org.elasticsearch.search.suggest.term.TermSuggestionBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Service
public class SearchFacade {
    static Logger log = LoggerFactory.getLogger(SearchFacade.class);
    final String all_path = "all_";
    final String DID_YOU_MEAN = "didYouMean";
    // NUMBER FACETS
    @Value("${search.numberfacets.path}")
    String numberFacetsNestedPath;
    String all_number_aggs_name = "all_number_aggs";
    // HISTOGRAM FACETS
    @Value("${search.histogramFacets.path}")
    String histogramFacetsNestedPath;
    @Value("${search.histogramFacets.interval}")
    int histogramIntervalDefault;
    String all_histogram_aggs_name = "all_histogram_aggs";
    // KEYWORD FACETS
    String all_keyword_aggs_name = "all_keyword_aggs";
    @Value("${search.keywordFacets.path}")
    String keywordFacetsNestedPath;
    // QUERY
    @Value("${search.query.path}")
    String queryPath;
    @Value("${search.path}")
    String searchPath;
    // SORT
    @Value("${search.facet.sort.default}")
    String sortDefault;
    @Value("${search.filter.size}")
    int filterSize;
    ExecutorService executorService = Executors.newFixedThreadPool(30);
    int pageSize = 0;
    @Autowired
    FacetFilterDao facetFilterDao;
    @Value("${search.numberfacets.value.path}")
    private String numberValuePath;
    @Value("${search.number.facets}")
    private String[] numberFacets;
    @Value("${search.histo.facets}")
    private String[] histoGramFacets;
    @Value("${search.histogramFacets.value.path}")
    private String histogramValuePath;
    @Value("${search.keywordFacets.cat.path}")
    private String catPath;
    @Value("${search.keywordFacets.code.path}")
    private String keywordCodePath;
    @Value("${search.keywordFacets.value.path}")
    private String keywordValuePath;
    @Value("${search.keywordFacets.code.path.v1}")
    private String keywordCodePathV1;
    @Value("${search.keywordFacets.value.path.v1}")
    private String keywordValuePathV1;
    @Value("${elastic.index}")
    private String elastcindex;
    @Value("${search.query.size}")
    private int defaultPageSize;
    @Value("${search.category.index}")
    private String categoryIndex;
    @Value("${search.category.field}")
    private String categorySearchField;
    @Value("${search.select.fields}")
    private String[] selectFields;
    @Autowired
    private RestHighLevelClient client;
    @Value("${boosted.categories}")
    private String[] boostedCategories;

    public SearchResponseModal search(SearchRequestModal requestModal) {
        if (requestModal.getFilterSize() > 0)
            filterSize = requestModal.getFilterSize();

        if (requestModal.getPagination() != null
                && requestModal.getPagination().getPs() > 0)
            pageSize = requestModal.getPagination().getPs();
        else
            pageSize = defaultPageSize;

        SearchResponseModal searchResponseModal = new SearchResponseModal();
        try {
           // if (requestModal.getLang() == null || requestModal.getLang().isBlank())
                requestModal.setLang("en");

            List<Callable<String>> callableTasks = new ArrayList<>();

            // CATEGORY
            if (requestModal.getCat() != null && !requestModal.getCat().isEmpty()) {
                // 1 search inside on category index
                prepareCategory(requestModal);

                // ATTRIBUTE SETS
                // 2 search inside on product index to compute attribute sets
//                if (StringUtils.isBlank(requestModal.getAttributeSet()))
//                    searchAttributeSets(requestModal);
            }

            // MAIN SEARCH
            mainSearch(requestModal, searchResponseModal, callableTasks);

            // SELECTED AGGS
            prepareSelectedAgg(requestModal, searchResponseModal, callableTasks);

            // HISTOGRAM AGG
            prepareAllHistogramAgg(requestModal, searchResponseModal, callableTasks);

            // KEYWORD AGG
            prepareAllKeywordAgg(requestModal, searchResponseModal, callableTasks);

            // NUMBER AGG
            prepareAllNumberAgg(requestModal, searchResponseModal, callableTasks);

            // Searching Category Aggregation
            prepareAllCategoryAgg(requestModal, callableTasks);
            List<Future<String>> results = executorService.invokeAll(callableTasks);

            results.forEach(result -> {
                try {
                    result.get();
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            });
            if (StringUtils.isNotBlank(requestModal.getVersion())
                    && requestModal.getVersion().trim().equalsIgnoreCase("v1"))
                facetFilterDao.loadNames(searchResponseModal, requestModal.getLang());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            searchResponseModal.setErrorModal(populateErrorResponse(e));
        }
        if (searchResponseModal != null && searchResponseModal.getTotalResults() == 0)
            searchResponseModal.setFacets(new HashSet<>());
        return searchResponseModal;
    }

    private void mainSearch(SearchRequestModal requestModal, SearchResponseModal responseModal, List<Callable<String>> callableTasks) {

//        Search Products
        Callable<String> task = () -> {
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            searchSourceBuilder.fetchSource(selectFields, null);
            searchSourceBuilder.size(pageSize);
            searchSourceBuilder.minScore(0.2F);
            prepareDidYouMeanSuggest(requestModal, searchSourceBuilder);

            SearchRequest searchRequest = new SearchRequest(elastcindex + "_" + requestModal.getLang());
            searchRequest.searchType(SearchType.DFS_QUERY_THEN_FETCH);
            searchRequest.source(searchSourceBuilder);

            // QUERY
            searchSourceBuilder.query(prepareQuery(requestModal));
            // SORT
            FieldSortBuilder sortBuilder = prepareSort(requestModal);
            if (sortBuilder != null)
                searchSourceBuilder.sort(sortBuilder);

            // PAGINATION
            preparePagination(requestModal, searchSourceBuilder);
            // POST FILTER
            AbstractQueryBuilder filters = prepareFilters(requestModal, null);
            if (filters != null)
                searchSourceBuilder.postFilter(filters);

            SearchResponse elasticSearchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
            responseModal.setTotalResults(elasticSearchResponse.getHits().getTotalHits().value);
            responseModal.setCategoryResponseModal(requestModal.getCurrCat());
            prepareProductsResponse(requestModal, elasticSearchResponse, responseModal);
            parseDidYouMeanSuggest(elasticSearchResponse, responseModal);
            return "success";
        };
        callableTasks.add(task);
    }

    void prepareDidYouMeanSuggest(SearchRequestModal requestModal, SearchSourceBuilder searchSourceBuilder) {
        if (StringUtils.isNotBlank(requestModal.getQ())) {
            TermSuggestionBuilder suggest = SuggestBuilders
                    .termSuggestion("completionTerms")
                    .text(requestModal.getQ().trim().toLowerCase())
                    .suggestMode(TermSuggestionBuilder.SuggestMode.POPULAR)
                    .sort(SortBy.FREQUENCY);

            searchSourceBuilder.suggest(new SuggestBuilder().addSuggestion(DID_YOU_MEAN, suggest));
        }
    }

    private void parseDidYouMeanSuggest(SearchResponse elasticSearchResponse, SearchResponseModal responseModal) {
        if (elasticSearchResponse.getSuggest() != null &&
                elasticSearchResponse.getSuggest().getSuggestion(DID_YOU_MEAN) != null &&
                CollectionUtils.isNotEmpty(elasticSearchResponse.getSuggest().getSuggestion(DID_YOU_MEAN).getEntries())) {
            List<? extends Suggest.Suggestion.Entry<? extends Suggest.Suggestion.Entry.Option>> suggestions = elasticSearchResponse.getSuggest().getSuggestion(DID_YOU_MEAN).getEntries();

            if (CollectionUtils.isNotEmpty(suggestions)) {
                int index = 0;
                responseModal.getDidYouMean().setOriginalText(suggestions.get(0).getText().string());
                for (Suggest.Suggestion.Entry.Option option : suggestions.get(0).getOptions()) {
                    if (index < 3)
                        responseModal.getDidYouMean().getSuggestions().add(option.getText().string());
                    index++;
                }
            }
        }
    }

    private FieldSortBuilder prepareSort(SearchRequestModal requestModal) {
        SortModal sortModal = requestModal.getSortModal();
        SortOrder order = SortOrder.DESC;

        if (sortModal == null)
            return null;

        if (sortModal.getDirection() != null ) {
            if (sortModal.getDirection().trim().toUpperCase().equalsIgnoreCase("ASC"))
                order = SortOrder.ASC;
            else if (sortModal.getDirection().trim().toUpperCase().equalsIgnoreCase("DESC"))
                order = SortOrder.DESC;
        }
        return new FieldSortBuilder(sortModal.getFacet()).order(order);
    }

    public AbstractQueryBuilder prepareQuery(SearchRequestModal requestModal) {
        BoolQueryBuilder mainQuery = QueryBuilders.boolQuery();
        mainQuery.boost(0.2F);
        mainQuery.must(QueryBuilders.rangeQuery("stock").gt(0));

        //mainQuery.filter(QueryBuilders.termQuery("catalogueId",1));

        if (requestModal.getCurrCat() != null && requestModal.getCurrCat().getCurrentCat() != null)
            mainQuery.must(QueryBuilders.nestedQuery("categories",
                    QueryBuilders.termQuery("categories.all-parents", requestModal.getCurrCat().getCurrentCat().getCode().toLowerCase()),
                    ScoreMode.Avg));

        // ONLY INCLUDING CATEGORY TAXONOMY.
        // EXCLUDING ATTRIBUTE SET
        if (requestModal.getCurrCat().getParent() != null && !requestModal.getCurrCat().getParent().isEmpty())
            requestModal.getCurrCat().getParent().stream().forEach(categoryModal -> {
                mainQuery.must(QueryBuilders.nestedQuery("categories",
                        QueryBuilders.termQuery("categories.all-parents", categoryModal.getCode().toLowerCase()),
                        ScoreMode.Avg));
            });

        if (StringUtils.isNotBlank(requestModal.getAttributeSet()))
            mainQuery.must(QueryBuilders.nestedQuery("categories",
                    QueryBuilders.termQuery("categories.attributeSets", requestModal.getAttributeSet().trim().toLowerCase()),
                    ScoreMode.Avg));


        if (!StringUtils.isEmpty(requestModal.getQ())) {

            String fullTextBoosted = searchPath + "." + "fullTextBoosted";
            String fullText = searchPath + "." + "fullText";
            String fullTextBoostedEdge = searchPath + "." + "fullTextBoosted.edge";
            String fullTextEdge = searchPath + "." + "fullText.edge";

            Map<String, Float> fields = new LinkedHashMap<>();


            // STRICT : CROSS FIELDS
            // OPERATOR : AND

            fields.put(fullTextBoosted, Float.valueOf(1.5F));
            fields.put(fullTextBoostedEdge, Float.valueOf(1.5F));
            fields.put(fullText, Float.valueOf(1));
            fields.put(fullTextEdge, Float.valueOf(1));

            // mainQuery.must(QueryBuilders.nestedQuery(searchPath,
                    // QueryBuilders.multiMatchQuery(requestModal.getQ().toLowerCase().trim(),
                            // fields.keySet().toArray(String[]::new))
                            // .fields(fields)
                            // .operator(Operator.AND)
                            // .type(MultiMatchQueryBuilder.Type.CROSS_FIELDS),
                    // ScoreMode.Avg));
            mainQuery.should(QueryBuilders.matchQuery("name", requestModal.getQ().toLowerCase().trim())
                    .operator(Operator.AND).boost(1));
        }

        if (StringUtils.isNotBlank(requestModal.getGlobalBrand())) {
            mainQuery.must(QueryBuilders.matchQuery("gbrand", requestModal.getGlobalBrand().toLowerCase().trim()));
        }

        if (requestModal.getProducts() != null && requestModal.getProducts().length > 0) {
            String[] products = new String[requestModal.getProducts().length];
            Arrays.stream(requestModal.getProducts()).sequential()
                    .filter(s -> StringUtils.isNotBlank(s))
                    .map(s -> s.trim().toLowerCase())
                    .collect(Collectors.toSet()).toArray(products);
            mainQuery.must(QueryBuilders.termsQuery("sku", products));
        }


        // BOOSTED
        List<FunctionScoreQueryBuilder.FilterFunctionBuilder> filterFunctionBuilders = new ArrayList<>();
        Arrays.stream(boostedCategories).forEach(s -> {
            String[] boostArray = s.split(":");

            filterFunctionBuilders.add(new FunctionScoreQueryBuilder.FilterFunctionBuilder(
                    QueryBuilders.boolQuery()
                            .should(QueryBuilders
                                    .nestedQuery("categories",
                                            QueryBuilders.termQuery("categories.all-parents", boostArray[0]), ScoreMode.Avg)),
                    ScoreFunctionBuilders
                            .weightFactorFunction(
                                    (StringUtils.isNotBlank(boostArray[1]) ? Float.valueOf(boostArray[1]) : 1F)
                            )));

        });

        if (filterFunctionBuilders.size() > 0) {
            FilterFunctionBuilder[] filterFunctions = new FilterFunctionBuilder[filterFunctionBuilders.size()];

            return QueryBuilders.functionScoreQuery(mainQuery, filterFunctionBuilders.toArray(filterFunctions))
                    .boostMode(CombineFunction.MULTIPLY).scoreMode(FunctionScoreQuery.ScoreMode.MULTIPLY)
                    .maxBoost(4f);
        }

        return QueryBuilders.functionScoreQuery(mainQuery);
    }


    private void prepareCategory(SearchRequestModal requestModal) throws IOException {
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder
                .query(QueryBuilders.boolQuery().filter(QueryBuilders.termQuery(categorySearchField, requestModal.getCat())));

        SearchRequest searchRequest = new SearchRequest(categoryIndex + requestModal.getLang());
        searchRequest.searchType(SearchType.DFS_QUERY_THEN_FETCH);
        searchRequest.source(searchSourceBuilder);

        SearchResponse categoryElasticResponse = client.search(searchRequest, RequestOptions.DEFAULT);

        categoryElasticResponse.getHits().forEach(hit -> {
            Map<String, Object> sourceMap = hit.getSourceAsMap();

            CategoryResponseModal currentCat = requestModal.getCurrCat();
            currentCat.setCurrentCat(new CategoryModal(String.valueOf(sourceMap.get("name")),
                    String.valueOf(sourceMap.get(categorySearchField))));

            if (sourceMap.get("bottomText") != null) {
                currentCat.setBottomText((String) sourceMap.get("bottomText"));
            }

            List<Map<String, Object>> childrenList = (List) sourceMap.get("children");

            if (CollectionUtils.isNotEmpty(childrenList))
                childrenList.stream().forEach(child -> {
                    Map<String, Object> childMap = (Map) child;

                    currentCat.getChildren().add(new CategoryModal(String.valueOf(childMap.get("name")),
                            String.valueOf(childMap.get("url"))));
                });

            List<Map<String, Object>> parentsList = (List) sourceMap.get("parents");
            if (CollectionUtils.isNotEmpty(parentsList))

                parentsList.stream()
                        .forEach(parent -> {
                            Map<String, Object> parentmap = (Map) parent;
                            String catName = String.valueOf(parentmap.get("name"));
                            if (catName != null && !catName.toLowerCase().startsWith("abraj"))
                                currentCat.getParent().add(new CategoryModal(String.valueOf(parentmap.get("name")),
                                        String.valueOf(parentmap.get("url"))));

                        });
            requestModal.setCurrCat(currentCat);
        });
    }

    private void searchAttributeSets(SearchRequestModal requestModal) throws IOException {
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(prepareQuery(requestModal));
        searchSourceBuilder.size(0);
        searchSourceBuilder.aggregation(
                AggregationBuilders.nested("attributeSets_nested", "categories")
                        .subAggregation(AggregationBuilders.terms("attributeSets").field("categories.attributeSets")
                                .size(100))
        );
        SearchRequest request = new SearchRequest();
        request.source(searchSourceBuilder);
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        response.getAggregations().asList().stream().forEach(aggregation -> {
            if (aggregation instanceof ParsedNested) {
                ((ParsedNested) aggregation).getAggregations().asList().forEach(aggregation1 -> {
                    if (aggregation1 instanceof ParsedStringTerms) {
                        ((ParsedStringTerms) aggregation1).getBuckets().forEach(bucket -> {
                            requestModal.getCurrCat().getAttributeSets().add(bucket.getKeyAsString().toLowerCase());
                        });
                    }
                });
            }
        });
    }

    private void preparePagination(SearchRequestModal requestModal, SearchSourceBuilder searchSourceBuilder) {
        int currentPage = (requestModal.getPagination() != null && requestModal.getPagination().getCp() > 0) ?
                requestModal.getPagination().getCp() : 0;
        int offset = currentPage * pageSize;
        searchSourceBuilder.from(offset).size(pageSize);
    }

    private void extractParseString(Aggregation catAgg, SearchResponseModal result, boolean processAllAgg) {
        if (catAgg instanceof ParsedStringTerms) {
            ParsedStringTerms attrStrTerms = (ParsedStringTerms) catAgg;
            attrStrTerms.getBuckets().stream().forEach(facetBucket -> {

                FacetResponseModal facetResponseModal = new FacetResponseModal(
                        facetBucket.getKeyAsString()
                                .trim());
                facetResponseModal.setFacetCode(facetBucket.getKeyAsString()
                        .trim());
                facetBucket.getAggregations().asList().stream()
                        .forEach(attrValAgg -> {
                            if (attrValAgg instanceof ParsedStringTerms) {
                                ParsedStringTerms filterStrTerms = (ParsedStringTerms) attrValAgg;

                                filterStrTerms.getBuckets().stream()
                                        .forEach(filterBucket -> {
                                            KeywordFilterResponseModal kf = new KeywordFilterResponseModal(
                                                    filterBucket
                                                            .getKeyAsString(),
                                                    filterBucket
                                                            .getDocCount());
                                            kf.setFilterCode(filterBucket
                                                    .getKeyAsString());
                                            facetResponseModal.getKeywordFilters()
                                                    .add(kf);
                                        });
                            }
                        });
                if (!processAllAgg)
                    result.getFacets().remove(facetResponseModal);
                result.getFacets().add(facetResponseModal);
            });
        }
    }

    private void extractHistoGram(Aggregation agg, SearchResponseModal result, boolean processAllAgg) {
        ParsedHistogram histogram = (ParsedHistogram) agg;
        FacetResponseModal histoFacet = new FacetResponseModal(histogram.getName().replaceAll(all_path, ""));

        if (!processAllAgg)
            result.getFacets().remove(histoFacet);

        if (result.getFacets().add(histoFacet))
            histogram.getBuckets().stream().forEach(histoBucket -> {
                Double from = (Double) histoBucket.getKey();
                histoFacet.getHistogramFilters()
                        .add(new NumberFilterResponseModal(histogram.getName()
                                .replaceAll(all_path, ""), histoBucket.getDocCount(),
                                from.intValue() + histogramIntervalDefault, from.intValue()));
            });
    }

    private void parseAggResponse(SearchRequestModal requestModal, Aggregations aggs,
                                  SearchResponseModal responseModal, boolean processAllAgg) {
        if (aggs != null)
            aggs.asList().stream()
                    .forEach(agg -> {
                        if ((processAllAgg && agg.getName().startsWith(all_path)) ||
                                (!processAllAgg && !agg.getName().startsWith(all_path)))

                            if (agg instanceof ParsedSingleBucketAggregation) {
                                ParsedSingleBucketAggregation lAgg = (ParsedSingleBucketAggregation) agg;
                                parseAggResponse(requestModal, lAgg.getAggregations(), responseModal, processAllAgg);

                            } else if (agg instanceof ParsedStringTerms)
                                extractParseString(agg, responseModal, processAllAgg);
                            else if (agg instanceof ParsedHistogram)
                                extractHistoGram(agg, responseModal, processAllAgg);
                            else if (agg instanceof ParsedStats) {
                                ParsedStats stats = (ParsedStats) agg;
                                NumberFilterResponseModal numberFilter =
                                        new NumberFilterResponseModal("price", stats.getCount(),
                                                (int) stats.getMax(), (int) stats.getMin());
                                FacetResponseModal facetResponseModal = new FacetResponseModal();
                                facetResponseModal.setFacet("price");
                                facetResponseModal.getNumberFilters().add(numberFilter);

                                if (!processAllAgg)
                                    responseModal.getFacets().remove(facetResponseModal);
                                responseModal.getFacets().add(facetResponseModal);
                            }
                    });
    }

    private void prepareProductsResponse(SearchRequestModal requestModal, SearchResponse elasticSearchResponse,
                                         SearchResponseModal result) {
        elasticSearchResponse.getHits().forEach(hitOuter -> {
            Map<String, Object> productsMap = new HashMap<>();
            Map<String, Object> sourceMap = hitOuter.getSourceAsMap();

            sourceMap.entrySet().stream()
                    .filter(set -> set.getKey().equals("images"))
                    .forEach(set -> {
                        List<Map> images = (List<Map>) set.getValue();

                        images.stream().forEach(imageMap -> {
                            Boolean primary = (Boolean) imageMap.get("primary");
                            if (primary) productsMap.put("image", imageMap.get("base"));
                        });
                    });

            sourceMap.remove("images");
            productsMap.putAll(sourceMap);

            //            result.getProducts()
//                    .add(new ProductResponseModal(hitOuter.getId(), (String) sourceMap.get("name"),
//                            (String) sourceMap.get("legacyId"),
//                            (String) sourceMap.get("sku"), price, priceSell, priceRrp, null,
//                            mageId, stock, (String) sourceMap.get("url")));

            result.getProducts().add(productsMap);
        });
    }

    private SearchErrorResponseModal populateErrorResponse(Exception e) {
        return new SearchErrorResponseModal(e.getMessage());
    }

    private AbstractQueryBuilder prepareCategoryFilters(SearchRequestModal requestModal) {
        BoolQueryBuilder boolCatQuery = QueryBuilders.boolQuery();

        if (requestModal.getCurrCat().getCurrentCat() != null) {
            boolCatQuery.should().add(QueryBuilders.termQuery(catPath,
                    requestModal.getCurrCat().getCurrentCat().getCode().toLowerCase().trim()));
            Set categories = new HashSet();
            requestModal.getCurrCat().getParent().stream()
                    .filter(parent -> parent != null && parent.getCode() != null)
                    .forEach(parent -> {
                        categories.add(parent.getCode().toLowerCase().trim());
                    });
            categories.stream().forEach(o -> {
                boolCatQuery.should()
                        .add(QueryBuilders.termQuery(catPath, o));
            });
        }

        if (StringUtils.isNotBlank(requestModal.getAttributeSet()))
            boolCatQuery.should()
                    .add(QueryBuilders.termQuery(catPath, requestModal.getAttributeSet().toLowerCase().trim()));

        else if (requestModal.getCurrCat().getAttributeSets().size() > 0)
            requestModal.getCurrCat().getAttributeSets().stream()
                    .forEach(attrSet -> {
                        boolCatQuery.should()
                                .add(QueryBuilders.termQuery(catPath, attrSet.toLowerCase().trim()));
                    });
        if (boolCatQuery.should() != null && boolCatQuery.should().size() > 0)
            return QueryBuilders.nestedQuery(searchPath,
                    QueryBuilders.nestedQuery(keywordFacetsNestedPath, boolCatQuery, ScoreMode.None), ScoreMode.None);
        return null;
    }

    private AbstractQueryBuilder prepareFilters(SearchRequestModal requestModal, String escapeFacet) {
        BoolQueryBuilder fullQuery = QueryBuilders.boolQuery();

        // CATEGORY FILTER
        // WILL INCLUDE ATTRIBUTES SETS ALSO IN CATEGORY FILTER, TO REDUCE ATTRIBUTES FOR ONLY THOSE CATS / ATTRSet
//        AbstractQueryBuilder catFilter = prepareCategoryFilters(requestModal);
//        if (catFilter != null)
//            fullQuery.filter().add(catFilter);

        // ATTRIBUTES FILTER

        if (!requestModal.getKeywordFacets().isEmpty()) {
            Map<String, List<Object>> selectedKeywordFacets = new HashMap<String, List<Object>>();
            requestModal.getKeywordFacets()
                    .stream()
                    .filter(kf -> kf != null
                            && StringUtils.isNotBlank(kf.getFacet())
                            && StringUtils.isNotBlank(kf.getFilter()))
                    .forEach(f -> {
                        if (escapeFacet != null && f.getFacet().equals(escapeFacet)) return;

                        if (selectedKeywordFacets.get(f.getFacet().trim()) == null)
                            selectedKeywordFacets.put(f.getFacet().trim(), new ArrayList<Object>());

                        f.setFacet(f.getFacet().trim());
                        f.setFilter(f.getFilter().trim());
                        selectedKeywordFacets.get(f.getFacet().trim()).add(f.getFilter().trim());
                    });

            String localKeywordCodePath = (StringUtils.isNotBlank(requestModal.getVersion())
                    && requestModal.getVersion().trim().equalsIgnoreCase("v1")) ? keywordCodePathV1 : keywordCodePath;

            String localKeywordValuePath = (StringUtils.isNotBlank(requestModal.getVersion())
                    && requestModal.getVersion().trim().equalsIgnoreCase("v1")) ? keywordValuePathV1 : keywordValuePath;

            if (selectedKeywordFacets != null && !selectedKeywordFacets.isEmpty()) {
                selectedKeywordFacets.entrySet().forEach(set -> {
                    BoolQueryBuilder attrBoolQuery = QueryBuilders.boolQuery();
                    attrBoolQuery.filter().add(QueryBuilders.termQuery(localKeywordCodePath, set.getKey()));

                    BoolQueryBuilder attrValBoolQuery = QueryBuilders.boolQuery();
                    set.getValue().stream().forEach(filter -> {
                        attrValBoolQuery.should().add(QueryBuilders.termQuery(localKeywordValuePath, filter));
                    });

                    attrBoolQuery.filter().add(attrValBoolQuery);
                    fullQuery.filter()
                            .add(QueryBuilders.nestedQuery(searchPath, QueryBuilders.nestedQuery(keywordFacetsNestedPath,
                                    attrBoolQuery, ScoreMode.None), ScoreMode.None));
                });
            }
        }
        if (CollectionUtils.isNotEmpty(requestModal.getNumberFacetModals())) {
            requestModal.getNumberFacetModals().stream()
                    .filter(f -> !f.isHistogram())
                    .forEach(f -> {
                        if (escapeFacet != null && f.getFacet().equals(escapeFacet)) return;

                        RangeQueryBuilder rangeQueryBuilder = QueryBuilders.rangeQuery(numberValuePath);
                        rangeQueryBuilder.gte(f.getFrom()).lte(f.getTo());

                        fullQuery.filter()
                                .add(QueryBuilders.nestedQuery(searchPath, QueryBuilders.nestedQuery(numberFacetsNestedPath,
                                        rangeQueryBuilder, ScoreMode.None), ScoreMode.None));
                    });
            requestModal.getNumberFacetModals().stream()
                    .filter(f -> f.isHistogram())
                    .forEach(f -> {
                        if (escapeFacet != null && f.getFacet().equals(escapeFacet)) return;

                        RangeQueryBuilder rangeQueryBuilder = QueryBuilders.rangeQuery(histogramValuePath);
                        rangeQueryBuilder.gte(f.getFrom()).lte(f.getTo());

                        fullQuery.filter()
                                .add(QueryBuilders.nestedQuery(searchPath, QueryBuilders.nestedQuery(histogramFacetsNestedPath,
                                        rangeQueryBuilder, ScoreMode.None), ScoreMode.None));
                    });
        }

        if (fullQuery.filter() != null && fullQuery.filter().size() > 0)
            return fullQuery;
        return null;
    }

    private void prepareAllKeywordAgg(SearchRequestModal requestModal, SearchResponseModal responseModal,
                                      List<Callable<String>> callableTasks) {
        String localKeywordCodePath = (StringUtils.isNotBlank(requestModal.getVersion())
                && requestModal.getVersion().trim().equalsIgnoreCase("v1")) ? keywordCodePathV1 : keywordCodePath;

        String localKeywordValuePath = (StringUtils.isNotBlank(requestModal.getVersion())
                && requestModal.getVersion().trim().equalsIgnoreCase("v1")) ? keywordValuePathV1 : keywordValuePath;

        Set<AbstractAggregationBuilder> aggs = new HashSet<>();
        BoolQueryBuilder filterQuery = (BoolQueryBuilder) prepareFilters(requestModal, null);

        NestedAggregationBuilder agg = AggregationBuilders.nested(all_path + "keyword_nested1", searchPath)
                .subAggregation(AggregationBuilders.nested(all_path + "keyword_nested2", keywordFacetsNestedPath)
                        .subAggregation(AggregationBuilders.terms(all_path + "facets").field(localKeywordCodePath)
                                .subAggregation(AggregationBuilders.terms(all_path + "filters")
                                        .field(localKeywordValuePath).size(filterSize))));

        if (filterQuery != null && filterQuery.filter().size() > 0)
            aggs.add(AggregationBuilders.filter(all_keyword_aggs_name, filterQuery)
                    .subAggregation(agg));
        else
            aggs.add(agg);


        prepareAllAggCallables(aggs, requestModal, responseModal, callableTasks, true);
    }

    private void prepareAllNumberAgg(SearchRequestModal requestModal, SearchResponseModal responseModal, List<Callable<String>> callableTasks) {
        Set<AbstractAggregationBuilder> aggs = new HashSet<>();
        BoolQueryBuilder filterQuery = (BoolQueryBuilder) prepareFilters(requestModal, null);
        Arrays.stream(numberFacets).sequential().forEach(numberFacet -> {

            NestedAggregationBuilder numberAgg = AggregationBuilders.nested(all_path + "numberNest", searchPath)
                    .subAggregation(AggregationBuilders.nested(all_path + "agg_number_facet", numberFacetsNestedPath)
                            .subAggregation(AggregationBuilders.filter(all_path + numberFacet + "_filter",
                                    QueryBuilders.termQuery(numberFacetsNestedPath + ".code", numberFacet))
                                    .subAggregation(AggregationBuilders.stats(all_path + numberValuePath)
                                            .field(numberValuePath))));

            if (filterQuery != null && filterQuery.filter().size() > 0)
                aggs.add(AggregationBuilders.filter(all_number_aggs_name, filterQuery)
                        .subAggregation(numberAgg));
            else
                aggs.add(numberAgg);

            prepareAllAggCallables(aggs, requestModal, responseModal, callableTasks, true);
        });
    }

    private void prepareAllAggCallables(Set<AbstractAggregationBuilder> aggs, SearchRequestModal requestModal, SearchResponseModal responseModal, List<Callable<String>> callableTasks, boolean processAllAgg) {
        aggs.forEach(agg -> {

            Callable<String> task = () -> {
                SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
                searchSourceBuilder.query(prepareQuery(requestModal));
                searchSourceBuilder.size(0);
                searchSourceBuilder.aggregation(agg);

                SearchRequest searchRequest = new SearchRequest(elastcindex + "_" + requestModal.getLang());
                searchRequest.searchType(SearchType.DFS_QUERY_THEN_FETCH);
                searchRequest.source(searchSourceBuilder);
                try {
                    SearchResponse elasticSearchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
                    parseAggResponse(requestModal, elasticSearchResponse.getAggregations(), responseModal, processAllAgg);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return agg.getName();
            };
            callableTasks.add(task);
        });
    }

    private void prepareAllCategoryAgg(SearchRequestModal requestModal, List<Callable<String>> callableTasks) {
        Callable<String> task = () -> {
            try {
                SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
                searchSourceBuilder.query(prepareQuery(requestModal));
                searchSourceBuilder.size(0);
                SearchRequest searchRequest = new SearchRequest(elastcindex + "_" + requestModal.getLang());
                searchRequest.searchType(SearchType.DFS_QUERY_THEN_FETCH);
                searchRequest.source(searchSourceBuilder);
                NestedAggregationBuilder agg = AggregationBuilders.nested(all_path + "categories", "categories")
                        .subAggregation(AggregationBuilders.terms("cats").field("categories.all-parents").size(100));

                BoolQueryBuilder filterQuery = (BoolQueryBuilder) prepareFilters(requestModal, null);
                if (filterQuery != null && filterQuery.filter().size() > 0)
                    searchSourceBuilder.aggregation(AggregationBuilders.filter("cats_filter", filterQuery)
                            .subAggregation(agg));
                else
                    searchSourceBuilder.aggregation(agg);
                SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);
                response.getAggregations().asList().stream().forEach(aggs -> {
                    ParsedNested parsedNested = null;
                    if (aggs instanceof ParsedNested)
                        parsedNested = (ParsedNested) aggs;
                    else if (aggs instanceof ParsedFilter) {
                        ParsedFilter filter = (ParsedFilter) aggs;
                        parsedNested = filter.getAggregations().asList().size() > 0 ?
                                (ParsedNested) filter.getAggregations().asList().get(0) : null;
                    }

                    if (parsedNested != null)
                        parsedNested.getAggregations().asList().stream().forEach(aggregation -> {
                            if (aggregation instanceof ParsedStringTerms) {
                                ParsedStringTerms stringTerms = (ParsedStringTerms) aggregation;
                                if (stringTerms.getBuckets().size() > 0) {
                                    stringTerms.getBuckets().stream().forEach(catBucket -> {
                                        String searchedCategory = catBucket.getKeyAsString().toLowerCase();

                                        AtomicBoolean notFound = new AtomicBoolean(true);
                                        // CHECKING WITH CURRENT CAT
                                        if (requestModal.getCurrCat().getCurrentCat() != null &&
                                                requestModal.getCurrCat().getCurrentCat().getCode().toLowerCase()
                                                        .equals(searchedCategory)) {
                                            requestModal.getCurrCat().getCurrentCat().setCount(catBucket.getDocCount());
                                            notFound.set(false);
                                        }
                                        // ELSE CHECKING WITHIN ALL PARENTS
                                        if (notFound.get()) {
                                            requestModal.getCurrCat().getParent().forEach(cat -> {
                                                if (cat.getCode().toLowerCase().equals(searchedCategory)) {
                                                    cat.setCount(catBucket.getDocCount());
                                                    notFound.set(false);
                                                }
                                            });
                                            // ELSE CHECKING WITHIN ALL CHILDREN
                                        }

                                        if (notFound.get()) {
                                            requestModal.getCurrCat().getChildren().forEach(cat -> {
                                                if (cat.getCode().toLowerCase().equals(searchedCategory)) {
                                                    cat.setCount(catBucket.getDocCount());
                                                }
                                            });
                                        }
                                    });
                                }
                            }
                        });
                });

            } catch (IOException e) {
                log.error(e.getMessage() + "_catAggFailure", e);
                return "catAggFailure";
            }
            return all_path + "categories";
        };
        callableTasks.add(task);
    }

    private void prepareAllHistogramAgg(SearchRequestModal requestModal, SearchResponseModal responseModal, List<Callable<String>> callableTasks) {
        Set<AbstractAggregationBuilder> aggs = new HashSet<>();
        BoolQueryBuilder filterQuery = (BoolQueryBuilder) prepareFilters(requestModal, null);

        Arrays.stream(histoGramFacets)
                .filter(histogramFacet -> histogramFacet != null && !histogramFacet.isEmpty())
                .sequential().forEach(histogramFacet -> {


            NestedAggregationBuilder histoAgg = AggregationBuilders.nested(all_path + "histoNest1", searchPath)
                    .subAggregation(AggregationBuilders.nested(all_path + "agg_histogram_facet", histogramFacetsNestedPath)
                            .subAggregation(AggregationBuilders.filter(all_path + histogramFacet + "_filter",
                                    QueryBuilders.termQuery(histogramFacetsNestedPath + ".code", histogramFacet))
                                    .subAggregation(AggregationBuilders.histogram(all_path + histogramFacet)
                                            .field(histogramValuePath).interval(histogramIntervalDefault))));

            if (filterQuery != null && filterQuery.filter().size() > 0)
                aggs.add(AggregationBuilders.filter(all_histogram_aggs_name, filterQuery)
                        .subAggregation(histoAgg));
            else
                aggs.add(histoAgg);

            prepareAllAggCallables(aggs, requestModal, responseModal, callableTasks, true);
        });
    }

    private void prepareSelectedAgg(SearchRequestModal requestModal, SearchResponseModal responseModal, List<Callable<String>> callableTasks) {
        Set<AbstractAggregationBuilder> selectedAggs = new HashSet<>();
        Map<String, Set<Object>> selectedFacets = new HashMap<String, Set<Object>>();
        Map<String, Set<Object>> histoFacets = new HashMap<String, Set<Object>>();
        Map<String, Set<Object>> priceFacets = new HashMap<String, Set<Object>>();

        // KEYWORDS
        if (!requestModal.getKeywordFacets().isEmpty()) {
            requestModal.getKeywordFacets().stream()
                    .filter(kf -> kf != null
                            && StringUtils.isNotBlank(kf.getFacet())
                            && StringUtils.isNotBlank(kf.getFilter()))
                    .forEach(f -> {
                        if (selectedFacets.get(f.getFacet().trim()) == null)
                            selectedFacets.put(f.getFacet().trim(), new HashSet<>());
                        f.setFacet(f.getFacet().trim());
                        selectedFacets.get(f.getFacet().trim()).add(f);
                    });
        }

        // HISTO GRAMS
        if (!requestModal.getNumberFacetModals().isEmpty())
            requestModal.getNumberFacetModals().stream()
                    .filter(f -> f.isHistogram())
                    .forEach(f -> {
                        if (histoFacets.get(f.getFacet().trim()) == null)
                            histoFacets.put(f.getFacet().trim(), new HashSet<>());

                        histoFacets.get(f.getFacet().trim()).add(f);
                    });

        // NUMBER FACETS
        if (!requestModal.getNumberFacetModals().isEmpty())
            requestModal.getNumberFacetModals().stream()
                    .filter(f -> !f.isHistogram())
                    .forEach(f -> {
                        if (priceFacets.get(f.getFacet().trim()) == null)
                            priceFacets.put(f.getFacet().trim(), new HashSet<>());

                        priceFacets.get(f.getFacet().trim()).add(f);
                    });

        String localKeywordCodePath = (StringUtils.isNotBlank(requestModal.getVersion())
                && requestModal.getVersion().trim().equalsIgnoreCase("v1")) ? keywordCodePathV1 : keywordCodePath;

        String localKeywordValuePath = (StringUtils.isNotBlank(requestModal.getVersion())
                && requestModal.getVersion().trim().equalsIgnoreCase("v1")) ? keywordValuePathV1 : keywordValuePath;

        int totalSelectedSize = selectedFacets.size() + histoFacets.size() + priceFacets.size();
        if (totalSelectedSize > 0 && totalSelectedSize == 1) {
            if (selectedFacets.size() == 1)
                selectedFacets.entrySet().forEach(selectedCurrentFacetSet -> {
                    selectedAggs.add(AggregationBuilders.nested(selectedCurrentFacetSet.getKey() + "_nested1", searchPath)
                            .subAggregation(AggregationBuilders.nested(selectedCurrentFacetSet.getKey() + "_nested2", keywordFacetsNestedPath)
                                    .subAggregation(AggregationBuilders
                                            .filter(selectedCurrentFacetSet.getKey() + "_aggs2",
                                                    QueryBuilders.termQuery(localKeywordCodePath,
                                                            selectedCurrentFacetSet.getKey()))
                                            .subAggregation(AggregationBuilders.terms("facets").field(localKeywordCodePath)
                                                    .subAggregation(AggregationBuilders.terms("filters")
                                                            .field(localKeywordValuePath).size(filterSize))))));
                });
            else if (histoFacets.size() == 1)
                histoFacets.entrySet().forEach(selectedCurrentFacetSet -> {
                    selectedAggs.add(AggregationBuilders.nested(selectedCurrentFacetSet.getKey() + "_nested1", searchPath)
                            .subAggregation(AggregationBuilders.nested(selectedCurrentFacetSet.getKey() + "_nested2", histogramFacetsNestedPath)
                                    .subAggregation(AggregationBuilders.filter(selectedCurrentFacetSet.getKey() + "_filter",
                                            QueryBuilders.termQuery(histogramFacetsNestedPath + ".code", selectedCurrentFacetSet.getKey()))
                                            .subAggregation(AggregationBuilders.histogram(selectedCurrentFacetSet.getKey())
                                                    .field(histogramValuePath).interval(histogramIntervalDefault)))));
                });
            else if (priceFacets.size() == 1)
                priceFacets.entrySet().forEach(selectedCurrentFacetSet -> {
                    selectedAggs.add(AggregationBuilders.nested(selectedCurrentFacetSet.getKey() + "_nested1", searchPath)
                            .subAggregation(AggregationBuilders.nested(selectedCurrentFacetSet.getKey() + "_nested2", numberFacetsNestedPath)
                                    .subAggregation(AggregationBuilders.filter(selectedCurrentFacetSet.getKey() + "_filter",
                                            QueryBuilders.termQuery(numberFacetsNestedPath + ".code", selectedCurrentFacetSet.getKey()))
                                            .subAggregation(AggregationBuilders.stats(numberValuePath).field(numberValuePath)))));
                });
        } else {
            // KEYWORDS
            if (selectedFacets != null && !selectedFacets.isEmpty()) {
                selectedFacets.entrySet().forEach(selectedCurrentFacetSet -> {
                    BoolQueryBuilder aggAllFilterBoolQuery = (BoolQueryBuilder) prepareFilters(requestModal, selectedCurrentFacetSet.getKey());

                    if ((aggAllFilterBoolQuery != null && aggAllFilterBoolQuery.filter().size() > 0))
                        selectedAggs.add(AggregationBuilders
                                .filter(selectedCurrentFacetSet.getKey() + "_aggs", aggAllFilterBoolQuery)
                                .subAggregation(AggregationBuilders.nested(selectedCurrentFacetSet.getKey() + "_nested1", searchPath)
                                        .subAggregation(AggregationBuilders.nested(selectedCurrentFacetSet.getKey() + "_nested2", keywordFacetsNestedPath)
                                                .subAggregation(AggregationBuilders
                                                        .filter(selectedCurrentFacetSet.getKey() + "_aggs2",
                                                                QueryBuilders.termQuery(localKeywordCodePath,
                                                                        selectedCurrentFacetSet.getKey()))
                                                        .subAggregation(AggregationBuilders.terms("facets").field(localKeywordCodePath)
                                                                .subAggregation(AggregationBuilders.terms("filters")
                                                                        .field(localKeywordValuePath).size(filterSize)))))));

                });
            }

            // HISTO
            histoFacets.entrySet().forEach(selectedCurrentFacetSet -> {
                BoolQueryBuilder aggAllFilterBoolQuery = (BoolQueryBuilder) prepareFilters(requestModal, selectedCurrentFacetSet.getKey());

                if (aggAllFilterBoolQuery != null && aggAllFilterBoolQuery.filter().size() > 0)
                    selectedAggs.add(AggregationBuilders.filter(selectedCurrentFacetSet.getKey() + "_aggs", aggAllFilterBoolQuery)
                            .subAggregation(AggregationBuilders.nested(selectedCurrentFacetSet.getKey() + "_nested1", searchPath)
                                    .subAggregation(AggregationBuilders.nested(selectedCurrentFacetSet.getKey() + "_nested2", histogramFacetsNestedPath)
                                            .subAggregation(AggregationBuilders.filter(selectedCurrentFacetSet.getKey() + "_filter",
                                                    QueryBuilders.termQuery(histogramFacetsNestedPath + ".code", selectedCurrentFacetSet.getKey()))
                                                    .subAggregation(AggregationBuilders.histogram(selectedCurrentFacetSet.getKey())
                                                            .field(histogramValuePath).interval(histogramIntervalDefault))))));
            });

            // price
            priceFacets.entrySet().forEach(selectedCurrentFacetSet -> {
                BoolQueryBuilder aggAllFilterBoolQuery = (BoolQueryBuilder) prepareFilters(requestModal, selectedCurrentFacetSet.getKey());

                if (aggAllFilterBoolQuery != null && aggAllFilterBoolQuery.filter().size() > 0)
                    selectedAggs.add(AggregationBuilders.filter(selectedCurrentFacetSet.getKey() + "_aggs", aggAllFilterBoolQuery)
                            .subAggregation(AggregationBuilders.nested(selectedCurrentFacetSet.getKey() + "_nested1", searchPath)
                                    .subAggregation(AggregationBuilders.nested(selectedCurrentFacetSet.getKey() + "_nested2", numberFacetsNestedPath)
                                            .subAggregation(AggregationBuilders.filter(selectedCurrentFacetSet.getKey() + "_filter",
                                                    QueryBuilders.termQuery(numberFacetsNestedPath + ".code", selectedCurrentFacetSet.getKey()))
                                                    .subAggregation(AggregationBuilders.stats(numberValuePath).field(numberValuePath))))));
            });
        }

        prepareAllAggCallables(selectedAggs, requestModal, responseModal, callableTasks, false);
    }
}
