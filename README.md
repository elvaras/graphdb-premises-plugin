# graphdb-premises-plugin

GraphDB Premises Plugin. Can be used to find which statements (premises) can be used to derive a specific statement. This includes:
- The statement itself, if this is explicitly asserted.
- The explicit statements (premises) from which the statement is derived through inference. If the statement is derived by chaining several inference rules, all the statements in the chain are provided.
- All intermediate implicit statements (also considered premises) from which the statement is derived.

A specific statement can be derived through several inference chains. For example, a statement can be both explicitly asserted and "derivable" through inference from a different explicit statement. In this case, the result includes the explicit statement itself with its original context, the statement itself with the onto:implicit context, and the premises which lead to infer the statement.

Also, if an explicit premise is asserted with different contexts, the triple is provided with all of the contexts.


## Building the plugin

The plugin is a Maven project.

Run `mvn clean package` to build the plugin and execute the tests.

The built plugin can be found in the `target` directory:

- `premises-plugin-graphdb-plugin.zip`

## Installing the plugin

External plugins are installed under `lib/plugins` in the GraphDB distribution
directory. To install the plugin follow these steps:

1. Remove the directory containing another version of the plugin from `lib/plugins` (e.g. `premises-plugin`).
1. Unzip the built zip file in `lib/plugins`.
1. Restart GraphDB. 

## Motivation

Often there is a need to find out how a particular statement has been derived by the inferencer, e.g. which premises have been matched to produce that statement.

## Predicates Supported

Namespace of the plugin is <http://www.ontotext.com/premises/>, its internal name "premises"

It supports following predicates:
- **premises:prepare**  - the subject will be bound to the state variable (a unique bnode in request scope) and the object is a list with 3 arguments, the subject, predicate and object of the statement to be explained.
When the subject is bound with the id of the state var, the other predicates can be used to fetch a part of the current solution (subject, predicate, object and context of the matching premise).
Upon re-evaluation, values from the next premise of the rule are used or we advance to the next solution to enumerate its premises for each of the rules that derive the statement.
For brevity of the results, a solution is checked whether it contains a premise that is equal to the source statement we explore and if so, that solution is skipped. That removes matches for self-supporting statements ( e.g when the same statement is also a premise of a rule that derives it).
- **premises:subject** -  the subject is the state variable and the object is bound to the subject of the premise
- **premises:predicate** -  the subject is the state variable and the object is bound to the predicate of the premise
- **premises:object** -  the subject is the state variable and the object is bound to the object of the premise
- **premises:context** -  the subject is the state variable and the object is bound to the context of the premise (or onto:explicit/onto:implicit)
