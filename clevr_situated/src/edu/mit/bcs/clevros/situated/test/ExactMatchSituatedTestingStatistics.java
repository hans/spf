/*******************************************************************************
 * Copyright (C) 2011 - 2015 Yoav Artzi, All rights reserved.
 * <p>
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * <p>
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *******************************************************************************/
package edu.mit.bcs.clevros.situated.test;

import edu.cornell.cs.nlp.spf.data.ILabeledDataItem;
import edu.cornell.cs.nlp.spf.data.utils.IValidator;
import edu.cornell.cs.nlp.spf.explat.IResourceRepository;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment.Parameters;
import edu.cornell.cs.nlp.spf.explat.resources.IResourceObjectCreator;
import edu.cornell.cs.nlp.spf.explat.resources.usage.ResourceUsage;
import edu.cornell.cs.nlp.spf.test.stats.IStatistics;
import edu.cornell.cs.nlp.spf.test.stats.SimpleStats;
import edu.cornell.cs.nlp.utils.log.ILogger;
import edu.cornell.cs.nlp.utils.log.LoggerFactory;
import edu.mit.bcs.clevros.util.DummyLogger;

import java.util.List;

/**
 * Testing statistics for the exact match metric.
 *
 * @author Yoav Artzi
 * @param <SAMPLE>
 *            Testing sample.
 * @param <LABEL>
 *            Provided label.
 */
public class ExactMatchSituatedTestingStatistics<SAMPLE, MR, LABEL, DI extends ILabeledDataItem<SAMPLE, LABEL>>
		extends AbstractSituatedTestingStatistics<SAMPLE, MR, LABEL, DI> {
	private static final ILogger defaultLogger = LoggerFactory.create(ExactMatchSituatedTestingStatistics.class);
	private static final ILogger dummyLogger = new DummyLogger();
	private ILogger LOG = defaultLogger;

	private static final String	DEFAULT_METRIC_NAME	= "EXACT_SITUATED";

	private final IValidator<DI, MR> validator;

	public ExactMatchSituatedTestingStatistics(IValidator<DI, MR> validator) {
		this(validator, null, DEFAULT_METRIC_NAME);
	}

	public ExactMatchSituatedTestingStatistics(IValidator<DI, MR> validator, String prefix, String metricName) {
		this(validator, prefix, metricName, new SimpleStats<>(DEFAULT_METRIC_NAME));
	}

	public ExactMatchSituatedTestingStatistics(IValidator<DI, MR> validator, String prefix, String metricName,
                                               IStatistics<SAMPLE> stats) {
		super(prefix, metricName, stats);
		this.validator = validator;
	}

	public void disableLogger() {
		LOG = dummyLogger;
	}

	public void enableLogger() {
		LOG = defaultLogger;
	}

	@Override
	public void recordNoParse(DI dataItem) {
		LOG.info("%s stats -- recording no parse", getMetricName());
		stats.recordFailure(dataItem.getSample());
	}

	@Override
	public void recordNoParseWithSkipping(DI dataItem) {
		LOG.info("%s stats -- recording no parse with skipping",
				getMetricName());
		stats.recordSloppyFailure(dataItem.getSample());
	}

	@Override
	public void recordParse(DI dataItem, MR candidate) {
		if (validator.isValid(dataItem, candidate)) {
			LOG.info("%s stats -- recording correct parse: %s",
					getMetricName(), candidate);
			stats.recordCorrect(dataItem.getSample());
		} else {
			LOG.info("%s stats -- recording wrong parse: %s", getMetricName(),
					candidate);
			stats.recordIncorrect(dataItem.getSample());
		}
	}

	@Override
	public void recordParses(DI dataItem, List<MR> candidates) {
		recordNoParse(dataItem);
	}

	@Override
	public void recordParsesWithSkipping(DI dataItem, List<MR> labels) {
		recordNoParseWithSkipping(dataItem);
	}

	@Override
	public void recordParseWithSkipping(DI dataItem, MR candidate) {
		if (validator.isValid(dataItem, candidate)) {
			LOG.info("%s stats -- recording correct parse with skipping: %s",
					getMetricName(), candidate);
			stats.recordSloppyCorrect(dataItem.getSample());
		} else {
			LOG.info("%s stats -- recording wrong parse with skipping: %s",
					getMetricName(), candidate);
			stats.recordSloppyIncorrect(dataItem.getSample());
		}
	}

	public static class Creator<SAMPLE, MR, LABEL, DI extends ILabeledDataItem<SAMPLE, LABEL>>
			implements
			IResourceObjectCreator<ExactMatchSituatedTestingStatistics<SAMPLE, MR, LABEL, DI>> {

		private String	type;

		public Creator() {
			this("test.stats.exact");
		}

		public Creator(String type) {
			this.type = type;
		}

		@Override
		public ExactMatchSituatedTestingStatistics<SAMPLE, MR, LABEL, DI> create(
				Parameters params, IResourceRepository repo) {
			return new ExactMatchSituatedTestingStatistics<>(
					repo.get(params.get("validator")), params.get("prefix"),
					params.get("name", DEFAULT_METRIC_NAME));
		}

		@Override
		public String type() {
			return type;
		}

		@Override
		public ResourceUsage usage() {
			return ResourceUsage
					.builder(type, ExactMatchSituatedTestingStatistics.class)
					.addParam("prefix", String.class,
							"Prefix string used to identify this metric")
					.addParam(
							"name",
							String.class,
							"Metric name (default: " + DEFAULT_METRIC_NAME
									+ ")")
					.addParam("validator", "id", "Situated validator").build();
		}

	}

}
