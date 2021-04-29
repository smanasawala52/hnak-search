package com.hnak.search.modal.response;

public class KeywordFilterResponseModal {
	String filter;
	String filterCode;
	long count;

	public KeywordFilterResponseModal(String filter, long count) {
		this.filter = filter;
		this.count = count;
	}

	public String getFilterCode() {
		return filterCode;
	}

	public void setFilterCode(String filterCode) {
		this.filterCode = filterCode;
	}

	public KeywordFilterResponseModal() {
		// TODO Auto-generated constructor stub
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
		result = prime * result + ((filter == null) ? 0 : filter.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {

		KeywordFilterResponseModal other = (KeywordFilterResponseModal) obj;
		return (filter != null && obj != null && filter.equals(other.filter));
	}

	public long getCount() {
		return count;
	}

	public void setCount(long count) {
		this.count = count;
	}


	@Override
	public String toString() {
		return "KeywordFilterResponseModal{" +
				", count=" + count +
				'}';
	}
}
