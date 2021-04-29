package com.hnak.search.modal.request;

public class PaginationModal {
	int cp;
	int ps;

	public PaginationModal() {
	}

	public PaginationModal(int cp, int ps, int totalPages) {
		this.cp = cp;
		this.ps = ps;
	}

	public int getCp() {
		return cp;
	}

	public void setCp(int cp) {
		this.cp = cp;
	}

	public int getPs() {
		return ps;
	}

	public void setPs(int ps) {
		this.ps = ps;
	}

}
