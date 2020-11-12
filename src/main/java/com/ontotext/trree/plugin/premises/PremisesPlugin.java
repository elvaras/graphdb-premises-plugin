package com.ontotext.trree.plugin.premises;

import java.util.HashMap;
import java.util.HashSet;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

import com.ontotext.trree.AbstractInferencer;
import com.ontotext.trree.AbstractRepositoryConnection;
import com.ontotext.trree.sdk.Entities.Scope;
import com.ontotext.trree.sdk.InitReason;
import com.ontotext.trree.sdk.ListPatternInterpreter;
import com.ontotext.trree.sdk.PatternInterpreter;
import com.ontotext.trree.sdk.PluginBase;
import com.ontotext.trree.sdk.PluginConnection;
import com.ontotext.trree.sdk.Preprocessor;
import com.ontotext.trree.sdk.Request;
import com.ontotext.trree.sdk.RequestContext;
import com.ontotext.trree.sdk.RequestOptions;
import com.ontotext.trree.sdk.StatelessPlugin;
import com.ontotext.trree.sdk.StatementIterator;
import com.ontotext.trree.sdk.SystemPlugin;
import com.ontotext.trree.sdk.SystemPluginOptions;
import com.ontotext.trree.sdk.SystemPluginOptions.Option;

/**
 * This is a plugin that can return rules and particular premises that cause a
 * particular statement to be inferred using current inferencer
 * 
 * The approach is to access the inferencer isSupported() method by providing a
 * suitable handler that handles the reported matches by rule.
 *
 * if we like to explain an inferred statement:
 *
 * PREFIX pr: http://www.ontotext.com/proof/ PREFIX onto:
 * http://www.ontotext.com/ select * { graph onto:implicit {?s ?p ?o} ?solution
 * pr:explain (?s ?p ?o) . ?solution pr:rule ?rulename . ?solution pr:subject
 * ?subj . ?solution pr:predicate ?pred. ?solution pr:object ?obj . ?solution
 * pr:context ?context . }
 * 
 * @author damyan.ognyanov
 *
 */
public class PremisesPlugin extends PluginBase
		implements StatelessPlugin, SystemPlugin, Preprocessor, PatternInterpreter, ListPatternInterpreter {
	// private key to store the connection in the request context
	private static final String REPOSITORY_CONNECTION = "repconn";
	// private key to store the inferencer in the request context
	private static final String INFERENCER = "infer";

	public static final String NAMESPACE = "http://www.ontotext.com/premises/";

	public static final IRI PREPARE_URI = SimpleValueFactory.getInstance().createIRI(NAMESPACE + "prepare");
	public static final IRI SUBJ_URI = SimpleValueFactory.getInstance().createIRI(NAMESPACE + "subject");
	public static final IRI PRED_URI = SimpleValueFactory.getInstance().createIRI(NAMESPACE + "predicate");
	public static final IRI OBJ_URI = SimpleValueFactory.getInstance().createIRI(NAMESPACE + "object");
	public static final IRI CONTEXT_URI = SimpleValueFactory.getInstance().createIRI(NAMESPACE + "context");
	private final static String KEY_STORAGE = "storage";

	public static final IRI DISTINCT_CONTEXTS_URI = SimpleValueFactory.getInstance()
			.createIRI(NAMESPACE + "distinctContexts");

	long prepareId = 0;
	long subjId = 0;
	long predId = 0;
	long objId = 0;
	long contextId = 0;
	long distinctContextsId = 0;

	public static PluginConnection lastPluginConnection;

	@Override
	public String getName() {
		return "premises";
	}

	/**
	 * this is the context implementation where the plugin stores currently running
	 * patterns it just keeps some values using sting keys for further access
	 *
	 */
	class ContextImpl implements RequestContext {
		HashMap<String, Object> map = new HashMap<String, Object>();
		Request request;

		@Override
		public Request getRequest() {
			return request;
		}

		@Override
		public void setRequest(Request request) {
			this.request = request;
		}

		public Object getAttribute(String key) {
			return map.get(key);
		}

		public void setAttribute(String key, Object value) {
			map.put(key, value);
		}

		public void removeAttribute(String key) {
			map.remove(key);
		}
	}

	/*
	 * main entry for predicate resolution of the ProvenancePlugin
	 * 
	 * (non-Javadoc)
	 * 
	 * @see com.ontotext.trree.sdk.PatternInterpreter#interpret(long, long, long,
	 * long, com.ontotext.trree.sdk.Statements, com.ontotext.trree.sdk.Entities,
	 * com.ontotext.trree.sdk.RequestContext)
	 */
	@Override
	public StatementIterator interpret(long subject, long predicate, long object, long context,
			PluginConnection pluginConnection, RequestContext requestContext) {
		lastPluginConnection = pluginConnection;

		System.out.println("interpret" + " subject, predicate, object, context: "
				+ Helper.ids(subject, predicate, object, context));

		// check if the predicate is one of the plugin's ones
		if (predicate != prepareId && predicate != contextId && predicate != subjId && predicate != predId
				&& predicate != objId)
			return null;

		// make sure we have the proper request context set when preprocess() has been
		// invoked
		// if not return EMPTY
		ContextImpl ctx = (requestContext instanceof ContextImpl) ? (ContextImpl) requestContext : null;

		// not our context
		if (ctx == null)
			return StatementIterator.EMPTY;

		if (predicate == subjId) {
			// same for the object
			PremisesIterator task = (PremisesIterator) ctx.getAttribute(KEY_STORAGE + subject);
			if (task == null || (task.currentSolution == null))
				return StatementIterator.EMPTY;
			if (object != 0 && task.currentValues[0] != object)
				return StatementIterator.EMPTY;
			// bind the value of the predicate from the current solution as object of the
			// triple pattern
			return StatementIterator.create(task.subject, predicate, task.currentValues[0], 0);
		} else if (predicate == predId) {
			// same for the object
			PremisesIterator task = (PremisesIterator) ctx.getAttribute(KEY_STORAGE + subject);
			if (task == null || (task.currentSolution == null))
				return StatementIterator.EMPTY;
			if (object != 0 && task.currentValues[1] != object)
				return StatementIterator.EMPTY;
			// bind the value of the predicate from the current solution as object of the
			// triple pattern
			return StatementIterator.create(task.subject, predicate, task.currentValues[1], 0);
		} else if (predicate == objId) {
			// same for the object
			PremisesIterator task = (PremisesIterator) ctx.getAttribute(KEY_STORAGE + subject);
			if (task == null || (task.currentSolution == null))
				return StatementIterator.EMPTY;
			if (object != 0 && task.currentValues[2] != object)
				return StatementIterator.EMPTY;
			// bind the value of the predicate from the current solution as object of the
			// triple pattern
			return StatementIterator.create(task.subject, predicate, task.currentValues[2], 0);
		} else if (predicate == contextId) {
			// same for the object
			PremisesIterator task = (PremisesIterator) ctx.getAttribute(KEY_STORAGE + subject);
			if (task == null || (task.currentSolution == null))
				return StatementIterator.EMPTY;
			if (object != 0 && task.currentValues[3] != object)
				return StatementIterator.EMPTY;
			// bind the value of the predicate from the current solution as object of the
			// triple pattern
			return StatementIterator.create(task.subject, predicate, task.currentValues[3], 0);
		}

		// if the predicate is not one of the registered in the ProvenancePlugin return
		// null
		return null;
	}

	/**
	 * returns some cardinality values for the plugin patterns to make sure that
	 * derivedFrom is evaluated first and binds the solution designator the solution
	 * indicator is used by the assess predicates to get the subject, pred or object
	 * of the current solution
	 */
	@Override
	public double estimate(long subject, long predicate, long object, long context, PluginConnection pluginConnection,
			RequestContext requestContext) {

		// if subject is not bound, any patttern return max value until there is some
		// binding ad subject place
		if (subject == 0)
			return Double.MAX_VALUE;

		// explain fetching predicates
		if (predicate == subjId || predicate == predId || predicate == objId || predicate == contextId) {
			return 1.0;
		}

		// unknown predicate??? maybe it is good to throw an exception
		return Double.MAX_VALUE;
	}

	/**
	 * the plugin uses preprocess to register its request context and access the
	 * system options where the current inferencer and repository connections are
	 * placed
	 */
	@Override
	public RequestContext preprocess(Request request) {
		// create a context instance
		ContextImpl impl = new ContextImpl();
		impl.setRequest(request);

		// check if there is a valid request and it has options
		if (request != null) {
			RequestOptions ops = request.getOptions();
			if (ops != null && ops instanceof SystemPluginOptions) {

				// retrieve the inferencer from the systemPluginOptions instance
				Object obj = ((SystemPluginOptions) ops).getOption(Option.ACCESS_INFERENCER);
				if (obj instanceof AbstractInferencer) {
					impl.setAttribute(INFERENCER, obj);
				}

				// retrieve the repository connection from the systemPluginOptions instance
				obj = ((SystemPluginOptions) ops).getOption(Option.ACCESS_REPOSITORY_CONNECTION);
				if (obj instanceof AbstractRepositoryConnection) {
					impl.setAttribute(REPOSITORY_CONNECTION, obj);
				}
			}
		}
		return impl;
	}

	/**
	 * init the plugin
	 */
	@Override
	public void initialize(InitReason initReason, PluginConnection pluginConnection) {
		// register the predicates
		prepareId = pluginConnection.getEntities().put(PREPARE_URI, Scope.SYSTEM);
		subjId = pluginConnection.getEntities().put(SUBJ_URI, Scope.SYSTEM);
		predId = pluginConnection.getEntities().put(PRED_URI, Scope.SYSTEM);
		objId = pluginConnection.getEntities().put(OBJ_URI, Scope.SYSTEM);
		contextId = pluginConnection.getEntities().put(CONTEXT_URI, Scope.SYSTEM);
		distinctContextsId = pluginConnection.getEntities().put(DISTINCT_CONTEXTS_URI, Scope.SYSTEM);
	}

	@Override
	public double estimate(long subject, long predicate, long[] objects, long context,
			PluginConnection pluginConnection, RequestContext requestContext) {
		if (predicate == prepareId) {
			if (objects.length != 3)
				return Double.MAX_VALUE;
			if (objects[0] == 0 || objects[1] == 0 || objects[2] == 0)
				return Double.MAX_VALUE;
			return 10L;
		}
		return Double.MAX_VALUE;
	}

	@Override
	public StatementIterator interpret(long subject, long predicate, long[] objects, long context,
			PluginConnection pluginConnection, RequestContext requestContext) {
		lastPluginConnection = pluginConnection;

		System.out.println("interpret" + " subject, predicate, context: " + Helper.ids(subject, predicate, context));
		System.out.println("interpret" + " objects: " + Helper.ids(objects));

		// make sure we have the proper request context set when preprocess() has been
		// invoked
		// if not return EMPTY
		ContextImpl ctx = (requestContext instanceof ContextImpl) ? (ContextImpl) requestContext : null;

		// not our context
		if (ctx == null)
			return StatementIterator.EMPTY;

		if (predicate == prepareId) {
			System.out.println("Interpreting predicate: " + Helper.ids(predicate));

			if (objects == null || objects.length != 3)
				return StatementIterator.EMPTY;

			long subj = objects[0];
			long pred = objects[1];
			long obj = objects[2];

			// empty if no binding, or some of the nodes is not a regular entity
			if (subj <= 0 || obj <= 0 || pred <= 0)
				return StatementIterator.EMPTY;

			// a context if an explicit exists
			AbstractInferencer inferencer = (AbstractInferencer) ctx.getAttribute(INFERENCER);
			if (inferencer.getInferStatementsFlag() == false)
				return StatementIterator.EMPTY;

			AbstractRepositoryConnection conn = (AbstractRepositoryConnection) ctx.getAttribute(REPOSITORY_CONNECTION);

			PremisesFinder finder = new PremisesFinder(subj, pred, obj, inferencer, conn, new HashSet<>());
			finder.init();

			// Create task associated with the predicate
			// Allocate a request scope id
			long reificationId = pluginConnection.getEntities().put(SimpleValueFactory.getInstance().createBNode(),
					Scope.REQUEST);

			// create a Task instance and pass the iterator of the statements from the
			// target graph
			PremisesIterator ret = new PremisesIterator(reificationId, prepareId, finder.solutions);

			// store the task into request context
			ctx.setAttribute(KEY_STORAGE + reificationId, ret);

			// return the newly created task instance (it is a valid StatementIterator that
			// could be reevaluated until all solutions are
			// generated)
			return ret;
		}

		return null;
	}

}
