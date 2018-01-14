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
package edu.cornell.cs.nlp.spf.data.collection;

import java.io.Serializable;

import edu.cornell.cs.nlp.spf.data.IDataItem;

/**
 * An iterable with a certain number of data items.
 *
 * @author Yoav Artzi
 * @param <DI>
 *            Data item type.
 */
public interface IDataCollection<DI extends IDataItem<?>>
		extends Iterable<DI>, Serializable {

	/**
	 * Size of the collection.
	 *
	 * @return
	 */
	int size();

	/**
	 * Receive notification that trainer has reached a certain epoch.
	 */
	default void handleEpoch(int epoch) {
	    // No-op.
    }

}
