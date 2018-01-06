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
package edu.mit.bcs.clevros.test;

import edu.cornell.cs.nlp.spf.data.ILabeledDataItem;

import java.util.List;

/**
 * Accumulates testing statistics.
 *
 * @author Yoav Artzi
 */
public interface ISituatedTestingStatistics<SAMPLE, MR, LABEL, DI extends ILabeledDataItem<SAMPLE, LABEL>> {

	void recordNoParse(DI dataItem);

	void recordNoParseWithSkipping(DI dataItem);

	/**
	 * Record a parse.
	 */
	void recordParse(DI dataItem, MR candidate);

	void recordParses(DI dataItem, List<MR> candidates);

	void recordParsesWithSkipping(DI dataItem, List<MR> candidates);

	/**
	 * Record a parse with word skipping enabled. Assumes a record parse for
	 * this data item has been called earlier.
	 */
	void recordParseWithSkipping(DI dataItem, MR candidate);

	@Override
	String toString();

	/**
	 * Generate machine readable tab-delimited string. Formatting:
	 * <key>=<value>\t<key>=<value>...
	 *
	 * @return
	 */
	String toTabDelimitedString();
}
