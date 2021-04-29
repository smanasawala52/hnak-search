package com.hnak.search.hnaksearch;


import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
public class Config {

	@Value("${elastic.scheme}")
	private String elasticsearchScheme;

	@Value("${elastic.hosts}")
	private String[] elasticHosts;

	@Value("${elastic.port}")
	private int elasticsearchPort;

	@Bean
	public RestHighLevelClient client() {
		List<HttpHost> httpHosts = new ArrayList<>();
		Arrays.stream(elasticHosts).forEach(host -> {httpHosts.add(new HttpHost(host, elasticsearchPort, elasticsearchScheme));});

		HttpHost[] a = new HttpHost[httpHosts.size()];
		for (int i = 0; i < httpHosts.size(); i++) {
			a[i] = httpHosts.get(i);
		}
		return new RestHighLevelClient(
				RestClient.builder(a));
	}
	
	
	@Bean
	public ObjectMapper getObjectMapper() {
		return new ObjectMapper();
	}
}
