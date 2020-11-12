package com.ontotext.trree.plugin.premises;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;

import org.eclipse.rdf4j.model.Value;

public class Helper {
	
	public static String ids(long... ids) {
		if (PremisesPlugin.lastPluginConnection == null)
			return Arrays.toString(ids);

		List<Value> values = new ArrayList<>();

		for (int i = 0; i < ids.length; i++) {
			values.add(PremisesPlugin.lastPluginConnection.getEntities().get(ids[i]));
		}

		return Arrays.toString(ids) + " " + values.toString();
	}

	public static String ids(List<long[]> ids) {
		StringJoiner sj = new StringJoiner("\n", "\n", "\n");

		for (long[] elem : ids) {
			sj.add(ids(elem));
		}

		return sj.toString();
	}
}
