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

package com.liferay.vulcan.architect.error.internal.converter;

import static javax.ws.rs.core.Response.Status.FORBIDDEN;

import com.liferay.vulcan.architect.converter.ExceptionConverter;
import com.liferay.vulcan.architect.result.APIError;

import javax.ws.rs.ForbiddenException;
import javax.ws.rs.core.Response;

import org.osgi.service.component.annotations.Component;

/**
 * Converts a {@link ForbiddenException} into its {@link APIError}
 * representation.
 *
 * @author Alejandro Hernández
 * @review
 */
@Component(immediate = true)
public class ForbiddenExceptionConverter
	extends WebApplicationExceptionConverter
	implements ExceptionConverter<ForbiddenException> {

	@Override
	public APIError convert(ForbiddenException exception) {
		return super.convert(exception);
	}

	@Override
	protected Response.StatusType getStatusType() {
		return FORBIDDEN;
	}

	@Override
	protected String getTitle() {
		return "Not permitted to access";
	}

	@Override
	protected String getType() {
		return "forbidden";
	}

}