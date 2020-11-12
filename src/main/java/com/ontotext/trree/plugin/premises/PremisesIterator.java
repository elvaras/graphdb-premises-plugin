package com.ontotext.trree.plugin.premises;

import java.util.Iterator;
import java.util.List;

import com.ontotext.trree.sdk.StatementIterator;

public class PremisesIterator extends StatementIterator {

	// this the the Value(Request scoped bnode) designating the currently running
	// instance (used to fetch the task from the context if multiple instances are
	// evaluated within same query0
	private final Iterator<Solution> iter;
	
	protected Solution currentSolution = null;
	private int currentNo = -1;
	
	protected long[] currentValues = null;
	
	public PremisesIterator(long reificationId, long explainId, List<Solution> solutions) {
		System.out.println("ExplainIter" + " reificationId: " + reificationId + " solutions: " + solutions);

		this.subject = reificationId;
		this.predicate = explainId;

		iter = solutions.iterator();
		if (iter.hasNext())
			currentSolution = iter.next();
		if (currentSolution != null) {
			currentNo = 0;
		}
	}

	
	@Override
	public void close() {
		currentSolution = null;
	}

	@Override
	public boolean next() {
		System.out.println("next");

		while (currentSolution != null) {
			System.out.println(
					"next" + " currentNo: " + currentNo + " current.premises.size: " + currentSolution.premises.size());

			if (currentNo < currentSolution.premises.size()) {
				currentValues = currentSolution.premises.get(currentNo);

				System.out.println("next" + " values: " + Helper.ids(currentValues));

				currentNo++;
				return true;
			} else {
				currentValues = null;
				currentNo = 0;

				System.out.println("next" + " values: null");

				if (iter.hasNext())
					currentSolution = iter.next();
				else
					currentSolution = null;
			}
		}
		return false;
	}
}
