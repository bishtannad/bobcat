package com.cognifide.qa.bb.cumber;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.CharEncoding;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

import com.google.common.collect.Sets;

public class BobcumberListener extends RunListener {

	private static final String FEATURE = "feature";

	private static final String SCENARIO = "Scenario";

	private static final Map<String, Set<String>> ADDED_FEATURES = new HashMap<>();

	private static final String COLON = ":";

	private Integer scenarioCounter = 0;

	private Integer testFailureCounter = 0;

	private boolean alreadyRegistered;

	private Bobcumber bobcumber;

	public BobcumberListener(Bobcumber bobcumber) {
		this.bobcumber = bobcumber;
	}

	@Override
	public void testRunFinished(Result result) throws Exception {
		try (PrintWriter writer = new PrintWriter(bobcumber.getStatisticsFile(), CharEncoding.UTF_8)) {
			writer.println(scenarioCounter);
			writer.println(testFailureCounter);
		}
	}

	@Override
	public void testStarted(Description description) throws Exception {
		String displayName = description.getDisplayName();
		String testStep = displayName.substring(0, displayName.lastIndexOf(COLON));
		if (SCENARIO.equals(testStep)) {
			scenarioCounter++;
			alreadyRegistered = false;
		}
	}

	@Override
	public void testFailure(Failure failure) throws Exception {
		String trace = normalizeTrace(failure.getTrace());
		if (trace.contains(FEATURE)) {
			addScenario(trace);
			if (!alreadyRegistered) {
				testFailureCounter++;
				alreadyRegistered = true;
			}
		}
	}

	private String normalizeTrace(String trace) {
		return trace.substring(trace.lastIndexOf("(") + 1, trace.lastIndexOf(")"));
	}

	private synchronized void addScenario(String failedScenario) throws IOException {

		String featureName = failedScenario.substring(0, failedScenario.lastIndexOf(COLON));
		String failedLineNumber = failedScenario.substring(failedScenario.lastIndexOf(COLON) + 1,
				failedScenario.length());

		if (ADDED_FEATURES.containsKey(featureName)) {
			Set<String> featureFailedLines = ADDED_FEATURES.get(featureName);
			featureFailedLines.add(failedLineNumber);
			ADDED_FEATURES.put(featureName, featureFailedLines);
		} else {
			ADDED_FEATURES.put(featureName, Sets.newHashSet(failedLineNumber));
		}

		PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(bobcumber.getFeatureFile(), false)));
		for (String feature : ADDED_FEATURES.keySet()) {
			out.print(feature);
			Set<String> lines = ADDED_FEATURES.get(feature);
			for (String line : lines) {
				out.print(COLON + line);
			}
			out.print(" ");
		}
		out.close();
	}
}