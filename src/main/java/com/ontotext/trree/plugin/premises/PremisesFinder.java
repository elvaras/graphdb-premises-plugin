package com.ontotext.trree.plugin.premises;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.ontotext.trree.AbstractInferencer;
import com.ontotext.trree.AbstractRepositoryConnection;
import com.ontotext.trree.ReportSupportedSolution;
import com.ontotext.trree.StatementIdIterator;
import com.ontotext.trree.query.QueryResultIterator;
import com.ontotext.trree.query.StatementSource;

public class PremisesFinder implements ReportSupportedSolution {
	
	// instance of the inference to work with
	private final AbstractInferencer inferencer;

	// connection to the raw data to get only the AXIOM statements
	private final AbstractRepositoryConnection conn;
	private final long subj, pred, obj;
	
	protected final ArrayList<Solution> solutions = new ArrayList<Solution>();

	private final Set<FinderStatement> visited;
	
	private static final int CONTEXT_MASK = StatementIdIterator.DELETED_STATEMENT_STATUS
			| StatementIdIterator.SKIP_ON_BROWSE_STATEMENT_STATUS;


	public PremisesFinder(long subj, long pred, long obj, AbstractInferencer inferencer, AbstractRepositoryConnection conn, Set<FinderStatement> visited) {
		System.out.println("ExplainFinder" + " subj, pre, obj, aContext: " + Helper.ids(subj, pred, obj));

		this.subj = subj;
		this.pred = pred;
		this.obj = obj;
		
		this.inferencer = inferencer;
		this.conn = conn;

		visited.add(new FinderStatement(subj, pred, obj));
		this.visited = visited;
	}

	public void init() {
		System.out.println("init");

		List<long[]> solutionsWithContexts = getAllSolutionsForTriple(this.subj, this.pred, this.obj);
		solutions.add(new Solution(solutionsWithContexts));

		inferencer.isSupported(subj, pred, obj, 0, 0, this);
	}

	@Override
	public boolean report(String ruleName, QueryResultIterator q) {
		System.out.println("report" + " ruleName: " + ruleName + " q: " + q);

		List<long[]> aSolution = new ArrayList<long[]>();

		if (q instanceof StatementSource) {
			StatementSource source = (StatementSource) q;
			Iterator<StatementIdIterator> sol = source.solution();

			Set<FinderStatement> implicitsToRequest = new HashSet<>();

			while (sol.hasNext()) {
				StatementIdIterator iter = sol.next();

				System.out.println("report" + " iter.subj, iter.pred, iter.obj, iter.context: "
						+ Helper.ids(iter.subj, iter.pred, iter.obj, iter.context));

				if (iter.subj == this.subj && iter.pred == this.pred && iter.obj == this.obj) {
					System.out.println("report" + " same triple, discarding solution");
					return false;
				}

				implicitsToRequest.add(new FinderStatement(iter.subj, iter.pred, iter.obj));
			}

			System.out.println("report aSolution: " + Helper.ids(aSolution));

			solutions.add(new Solution(aSolution));

			System.out.println("report calling isSupported on implicit solutions");

			for (FinderStatement st : implicitsToRequest) {
				if (visited.contains(st)) {
					System.out.println("Skipping already visited statement: " + st);
					continue;
				}

				PremisesFinder finder = new PremisesFinder(st.subj, st.pred, st.obj, inferencer, conn, visited);
				finder.init();

				solutions.addAll(finder.solutions);
			}
		}

		return false;
	}

	private List<long[]> getAllSolutionsForTriple(long subj, long pred, long obj) {
		List<long[]> solutions = new ArrayList<>();

		// try finding an existing explicit or in-context with same subj, pred and obj
		try (StatementIdIterator ctxIter = conn.getStatements(subj, pred, obj, true, 0, CONTEXT_MASK)) {

			while (ctxIter.hasNext()) {
				System.out.println("report" + " ctxIter.subj, ctxIter.pred, ctxIter.obj, ctxIter.context: "
						+ Helper.ids(ctxIter.subj, ctxIter.pred, ctxIter.obj, ctxIter.context) + " ctxIter.status: "
						+ ctxIter.status);

				solutions.add(new long[] { ctxIter.subj, ctxIter.pred, ctxIter.obj, ctxIter.context, ctxIter.status });

				ctxIter.next();
			}
			ctxIter.close();
		}

		return solutions;
	}

	@Override
	public AbstractRepositoryConnection getConnection() {
		return conn;
	}

}
