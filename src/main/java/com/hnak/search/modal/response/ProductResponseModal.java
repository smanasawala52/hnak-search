package com.hnak.search.modal.response;

import java.util.Map;

public class ProductResponseModal {
	Map<String, Object> responseMap;


	public Map<String, Object> getResponseMap() {
		return responseMap;
	}

	public void setResponseMap(Map<String, Object> responseMap) {
		this.responseMap = responseMap;
	}

	public ProductResponseModal(Map<String, Object> responseMap){this.responseMap = responseMap;}


	public ProductResponseModal() {
		// TODO Auto-generated constructor stub
	}
}
