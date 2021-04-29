package com.hnak.search.modal.request;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Objects;

public class CategoryModal {
	private String name;
	private String code;
	private long count;

	public long getCount() {
		return count;
	}

	public void setCount(long count) {
		this.count = count;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public CategoryModal(String name, String code) {
		super();
		this.name = name;
		this.code = code;
	}

	@Override
	public String toString() {
		return "CategoryModal [name=" + name + ", code=" + code + "]";
	}



	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		CategoryModal that = (CategoryModal) o;
		if (this.code != null && that.code != null)
			return this.code.equals(that.code);
		return name.equals(that.name);
	}

	@Override
	public int hashCode() {
		if(this.code != null)return Objects.hash(this.code);
			return Objects.hash(name);
	}
}
