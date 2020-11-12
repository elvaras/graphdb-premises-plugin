package com.ontotext.trree.plugin.premises;

import java.util.List;

public class Solution {
	List<long[]> premises;

	Solution(List<long[]> premises) {
		this.premises = premises;
	}

	@Override
	public String toString() {
		return "Solution" + " premises: " + Helper.ids(premises);
	}
}
