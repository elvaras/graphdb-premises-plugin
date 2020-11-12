package com.ontotext.trree.plugin.premises;

import java.util.Objects;

public class FinderStatement {
	long subj, pred, obj;

	public FinderStatement(long subj, long pred, long obj) {
		super();
		this.subj = subj;
		this.pred = pred;
		this.obj = obj;
	}

	@Override
	public String toString() {
		return Helper.ids(subj, pred, obj);
	}

	@Override
	public int hashCode() {
		return Objects.hash(subj, pred, obj);
	}

	@Override
	public boolean equals(Object o) {
		if (o == null)
			return false;
		if (this == o)
			return true;
		if (this.getClass() != o.getClass())
			return false;

		FinderStatement other = (FinderStatement) o;

		return subj == other.subj && pred == other.pred && obj == other.obj;
	}
}
