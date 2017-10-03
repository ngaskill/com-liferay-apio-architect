/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.vulcan.pagination;

import aQute.bnd.annotation.ProviderType;

import com.liferay.vulcan.uri.Path;

import java.util.Collection;

/**
 * Represents a page in a collection. Writers can use instances of this
 * interface to create hypermedia representations.
 *
 * @author Alejandro Hernández
 * @author Carlos Sierra Andrés
 * @author Jorge Ferrer
 * @review
 */
@ProviderType
public interface Page<T> {

	/**
	 * Returns the items of the page.
	 *
	 * @return the items of the page.
	 * @review
	 */
	public Collection<T> getItems();

	/**
	 * Returns the number of items the user has selected on the page.
	 *
	 * @return the number of items the user has selected on the page.
	 * @review
	 */
	public int getItemsPerPage();

	/**
	 * Returns the number of the last page of the collection.
	 *
	 * @return the number of the last page of the collection.
	 * @review
	 */
	public int getLastPageNumber();

	/**
	 * Returns the model class of the page.
	 *
	 * @return the model class of the page.
	 * @review
	 */
	public Class<T> getModelClass();

	/**
	 * Returns the page number in the collection.
	 *
	 * @return the page number in the collection.
	 * @review
	 */
	public int getPageNumber();

	/**
	 * Returns the identifier.
	 *
	 * @return the identifier.
	 * @review
	 */
	public Path getPath();

	/**
	 * Returns the total number of elements in the collection.
	 *
	 * @return the total number of elements in the collection.
	 * @review
	 */
	public int getTotalCount();

	/**
	 * Returns {@code <code>true</code>} if another page follows this page in
	 * the collection.
	 *
	 * @return {@code <code>true</code>} if another page follows this page in
	 *         the collection; {@code <code>false</code>} otherwise.
	 * @review
	 */
	public boolean hasNext();

	/**
	 * Returns {@code <code>true</code>} if another page precedes this page in
	 * the collection.
	 *
	 * @return {@code <code>true</code>} if another page precedes this page in
	 *         the collection; {@code <code>false</code>} otherwise.
	 * @review
	 */
	public boolean hasPrevious();

}