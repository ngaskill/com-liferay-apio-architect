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

package com.liferay.apio.architect.internal.action;

import static io.vavr.API.$;
import static io.vavr.API.Case;

import static java.util.Collections.unmodifiableList;

import com.liferay.apio.architect.annotation.Id;
import com.liferay.apio.architect.form.Body;
import com.liferay.apio.architect.form.Form;
import com.liferay.apio.architect.internal.alias.ProvideFunction;
import com.liferay.apio.architect.internal.annotation.Action;
import com.liferay.apio.architect.operation.HTTPMethod;
import com.liferay.apio.architect.resource.Resource;

import io.vavr.CheckedFunction1;
import io.vavr.Function2;
import io.vavr.control.Try;

import java.lang.annotation.Annotation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.ws.rs.ForbiddenException;

/**
 * Contains semantic information about an action.
 *
 * @author Alejandro Hernández
 */
public final class ActionSemantics {

	/**
	 * Starts creating a new {@code ActionSemantics} instance by providing
	 * information about the action's resource.
	 *
	 * @param  resource the action's resource
	 * @return the {@code NameStep}
	 * @review
	 */
	public static NameStep ofResource(Resource resource) {
		ActionSemantics actionSemantics = new ActionSemantics();

		actionSemantics._resource = resource;

		return new Builder(actionSemantics);
	}

	/**
	 * Executes the permission function with the provided parameters to check if
	 * the user has the permissions to execute an action.
	 *
	 * @param  params the parameters
	 * @return {@code true} if the user has the necessary permissions; {@code
	 *         false} otherwise
	 * @review
	 */
	public Boolean checkPermissions(List<?> params) throws Throwable {
		return _permissionCheckedFunction1.apply(params);
	}

	/**
	 * Executes the action with the provided parameters.
	 *
	 * @param  params the parameters
	 * @return the object
	 * @review
	 */
	public Object execute(List<?> params) throws Throwable {
		return _executeCheckedFunction1.apply(params);
	}

	/**
	 * Returns the action's name.
	 *
	 * @return the action's name
	 */
	public String getActionName() {
		return _name;
	}

	/**
	 * Returns the list of annotations.
	 *
	 * @return the list of annotations
	 */
	public List<Annotation> getAnnotations() {
		return unmodifiableList(_annotations);
	}

	/**
	 * Returns the transformed body value for the action, if needed. Returns
	 * {@code null} otherwise.
	 *
	 * @param  body the body
	 * @return the transformed body value, if needed; {@code null} otherwise
	 */
	public Object getBodyValue(Body body) {
		if (_bodyFunction == null) {
			return null;
		}

		return _bodyFunction.apply(body);
	}

	/**
	 * Returns the form for the action, if present. Returns {@code
	 * Optional#empty()} otherwise.
	 *
	 * @return the form, if present; {@code Optional#empty()} otherwise
	 */
	public Optional<Form> getFormOptional() {
		return Optional.ofNullable(_form);
	}

	/**
	 * Returns the method in which the action is executed.
	 *
	 * @return the method
	 */
	public String getHTTPMethod() {
		return _method;
	}

	/**
	 * Returns the list of parameter classes.
	 *
	 * @return the list of parameter classes
	 */
	public List<Class<?>> getParamClasses() {
		return unmodifiableList(_paramClasses);
	}

	public List<Object> getParams(Function<Class<?>, Object> provideFunction) {
		Stream<Class<?>> stream = getParamClasses().stream();

		return stream.map(
			provideFunction
		).collect(
			Collectors.toList()
		);
	}

	public List<Object> getPermissionParams(
		Function<Class<?>, Object> provideFunction) {

		Stream<Class<?>> stream = getPermissionProvidedClasses().stream();

		return stream.map(
			provideFunction
		).map(
			param -> {
				if (param instanceof Resource.Id) {
					return ((Resource.Id)param).asObject();
				}

				return param;
			}
		).collect(
			Collectors.toList()
		);
	}

	/**
	 * Returns the list of permission classes.
	 *
	 * @return the list of permission classes
	 */
	public List<Class<?>> getPermissionProvidedClasses() {
		return unmodifiableList(_permissionProvidedClasses);
	}

	/**
	 * Returns the action's resource.
	 *
	 * @return the action's resource
	 */
	public Resource getResource() {
		return _resource;
	}

	/**
	 * Returns the class returned by the action.
	 *
	 * @return the class returned by the action
	 */
	public Class<?> getReturnClass() {
		return _returnClass;
	}

	/**
	 * Transforms this {@code ActionSemantics} instance into its {@code Action}.
	 *
	 * @param  provideFunction the function used to provide instances of action
	 *         parameters
	 * @return the action
	 */
	@SuppressWarnings({"Convert2MethodRef", "unchecked"})
	public Action toAction(ProvideFunction provideFunction) {
		Action action = request -> Try.of(
			() -> getPermissionParams(provideFunction.apply(this, request))
		).mapTry(
			this::checkPermissions
		).filter(
			aBoolean -> aBoolean
		).mapFailure(
			Case($(), () -> new ForbiddenException())
		).mapTry(
			__ -> provideFunction.apply(this, request)
		).mapTry(
			this::getParams
		).mapTry(
			this::execute
		);

		if (Void.class.isAssignableFrom(_returnClass)) {
			return (Action.NoContent)action::execute;
		}

		return (Action.Ok)action::execute;
	}

	/**
	 * Copies the current {@code ActionSemantics} by setting a new value for the
	 * annotations attribute. If the same annotations value is provided, a
	 * shallow reference equality check prevents the copy and returns {@code
	 * this}.
	 *
	 * @param  annotations the new annotations list
	 * @return the copy of the current {@code ActionSemantics} with new
	 *         annotations; {@code this} if the annotations are the same
	 */
	public ActionSemantics withAnnotations(List<Annotation> annotations) {
		if (_annotations.equals(annotations)) {
			return this;
		}

		ActionSemantics actionSemantics = new ActionSemantics();

		actionSemantics._annotations = annotations;
		actionSemantics._bodyFunction = _bodyFunction;
		actionSemantics._executeCheckedFunction1 = _executeCheckedFunction1;
		actionSemantics._form = _form;
		actionSemantics._method = _method;
		actionSemantics._name = _name;
		actionSemantics._paramClasses = _paramClasses;
		actionSemantics._permissionCheckedFunction1 =
			_permissionCheckedFunction1;
		actionSemantics._permissionProvidedClasses = _permissionProvidedClasses;
		actionSemantics._resource = _resource;
		actionSemantics._returnClass = _returnClass;

		return actionSemantics;
	}

	/**
	 * Copies the current {@code ActionSemantics} by setting a new value for the
	 * HTTP method attribute. If the same HTTP method is provided, a shallow
	 * reference equality check prevents the copy and returns {@code this}.
	 *
	 * @param  method the new HTTP method
	 * @return the copy of the current {@code ActionSemantics} with the new HTTP
	 *         method; {@code this} if the HTTP method is the same
	 */
	public ActionSemantics withMethod(String method) {
		if (_method.equals(method)) {
			return this;
		}

		ActionSemantics actionSemantics = new ActionSemantics();

		actionSemantics._annotations = _annotations;
		actionSemantics._bodyFunction = _bodyFunction;
		actionSemantics._executeCheckedFunction1 = _executeCheckedFunction1;
		actionSemantics._form = _form;
		actionSemantics._method = method;
		actionSemantics._name = _name;
		actionSemantics._paramClasses = _paramClasses;
		actionSemantics._permissionCheckedFunction1 =
			_permissionCheckedFunction1;
		actionSemantics._permissionProvidedClasses = _permissionProvidedClasses;
		actionSemantics._resource = _resource;
		actionSemantics._returnClass = _returnClass;

		return actionSemantics;
	}

	/**
	 * Copies the current {@code ActionSemantics} by setting a new value for the
	 * name attribute. If the same name is provided, a shallow reference
	 * equality check prevents the copy and returns {@code this}.
	 *
	 * @param  name the new name
	 * @return the copy of the current {@code ActionSemantics} with the new
	 *         name; {@code this} if the name is the same
	 */
	public ActionSemantics withName(String name) {
		if (_name.equals(name)) {
			return this;
		}

		ActionSemantics actionSemantics = new ActionSemantics();

		actionSemantics._annotations = _annotations;
		actionSemantics._bodyFunction = _bodyFunction;
		actionSemantics._executeCheckedFunction1 = _executeCheckedFunction1;
		actionSemantics._form = _form;
		actionSemantics._method = _method;
		actionSemantics._name = name;
		actionSemantics._paramClasses = _paramClasses;
		actionSemantics._permissionCheckedFunction1 =
			_permissionCheckedFunction1;
		actionSemantics._permissionProvidedClasses = _permissionProvidedClasses;
		actionSemantics._resource = _resource;
		actionSemantics._returnClass = _returnClass;

		return actionSemantics;
	}

	/**
	 * Copies the current {@link ActionSemantics} by setting a new value for the
	 * resource attribute. If the same resource is provided, a shallow reference
	 * equality check prevents the copy and returns {@code this}.
	 *
	 * @param  resource the new resource
	 * @return the copy of the current {@code ActionSemantics} with the new
	 *         resource; {@code this} if the resource is the same
	 */
	public ActionSemantics withResource(Resource resource) {
		ActionSemantics actionSemantics = new ActionSemantics();

		actionSemantics._annotations = _annotations;
		actionSemantics._bodyFunction = _bodyFunction;
		actionSemantics._executeCheckedFunction1 = _executeCheckedFunction1;
		actionSemantics._form = _form;
		actionSemantics._method = _method;
		actionSemantics._name = _name;
		actionSemantics._paramClasses = _paramClasses;
		actionSemantics._permissionCheckedFunction1 =
			_permissionCheckedFunction1;
		actionSemantics._permissionProvidedClasses = _permissionProvidedClasses;
		actionSemantics._resource = resource;
		actionSemantics._returnClass = _returnClass;

		return actionSemantics;
	}

	/**
	 * Copies the current {@link ActionSemantics} by setting a new value for the
	 * return class attribute. If the same return class is provided, a shallow
	 * reference equality check prevents the copy and returns {@code this}.
	 *
	 * @param  returnClass the new return class
	 * @return the copy of the current {@code ActionSemantics} with the new
	 *         return class; {@code this} if the return class is the same
	 */
	public ActionSemantics withReturnClass(Class<?> returnClass) {
		if (_returnClass.equals(returnClass)) {
			return this;
		}

		ActionSemantics actionSemantics = new ActionSemantics();

		actionSemantics._annotations = _annotations;
		actionSemantics._bodyFunction = _bodyFunction;
		actionSemantics._executeCheckedFunction1 = _executeCheckedFunction1;
		actionSemantics._form = _form;
		actionSemantics._method = _method;
		actionSemantics._name = _name;
		actionSemantics._paramClasses = _paramClasses;
		actionSemantics._permissionCheckedFunction1 =
			_permissionCheckedFunction1;
		actionSemantics._permissionProvidedClasses = _permissionProvidedClasses;
		actionSemantics._resource = _resource;
		actionSemantics._returnClass = returnClass;

		return actionSemantics;
	}

	public static class Builder
		implements NameStep, MethodStep, ReturnStep, PermissionStep,
				   ExecuteStep, FinalStep {

		public Builder(ActionSemantics actionSemantics) {
			_actionSemantics = actionSemantics;
		}

		@Override
		public FinalStep annotatedWith(Annotation annotation) {
			_actionSemantics._annotations.add(annotation);

			return this;
		}

		@Override
		public FinalStep annotatedWith(Annotation... annotations) {
			_actionSemantics._annotations = Arrays.asList(annotations);

			return this;
		}

		@Override
		public ActionSemantics build() {
			return _actionSemantics;
		}

		@Override
		public FinalStep executeFunction(
			CheckedFunction1<List<?>, ?> executeCheckedFunction1) {

			_actionSemantics._executeCheckedFunction1 = executeCheckedFunction1;

			return this;
		}

		@Override
		public FinalStep form(
			Form form, Function2<Form, Body, Object> function2) {

			_actionSemantics._form = form;

			if (form != null) {
				_actionSemantics._bodyFunction = function2.apply(form);
			}

			return this;
		}

		@Override
		public ReturnStep method(String method) {
			_actionSemantics._method = method;

			return this;
		}

		@Override
		public MethodStep name(String name) {
			_actionSemantics._name = name;

			return this;
		}

		@Override
		public ExecuteStep permissionFunction() {
			_actionSemantics._permissionCheckedFunction1 = params -> true;

			return this;
		}

		@Override
		public ExecuteStep permissionFunction(
			CheckedFunction1<List<?>, Boolean> permissionCheckedFunction1) {

			_actionSemantics._permissionCheckedFunction1 =
				permissionCheckedFunction1;

			return this;
		}

		@Override
		public ExecuteStep permissionProvidedClasses(Class<?>... classes) {
			_actionSemantics._permissionProvidedClasses = Arrays.asList(
				classes);

			return this;
		}

		@Override
		public FinalStep receivesParams(Class<?>... classes) {
			_actionSemantics._paramClasses = Arrays.asList(classes);

			return this;
		}

		@Override
		public PermissionStep returns(Class<?> returnClass) {
			_actionSemantics._returnClass = returnClass;

			return this;
		}

		private final ActionSemantics _actionSemantics;

	}

	public interface ExecuteStep {

		/**
		 * Provides information about the function action's execute function.
		 * This function receives the list of parameters in the order provided
		 * in the {@link FinalStep#receivesParams(Class[])} method.
		 *
		 * @param  executeCheckedFunction1 the execute function
		 * @return the {@code FinalStep}
		 * @review
		 */
		public FinalStep executeFunction(
			CheckedFunction1<List<?>, ?> executeCheckedFunction1);

		/**
		 * Provides information about the permission method arguments. This
		 * function receives the list of parameter classes to provide to the
		 * {@code permissionCheckedFunction1}.
		 *
		 * @param  classes the list of parameter classes
		 * @return the {@code ExecuteStep}
		 * @review
		 */
		public ExecuteStep permissionProvidedClasses(Class<?>... classes);

	}

	public interface FinalStep {

		/**
		 * Provides information about the parameters the action needs.
		 *
		 * <p>
		 * The parameter instances must be provided to the method {@link
		 * ActionSemantics#execute(List)} in the same order as their classes in
		 * this method. {@code Void} classes will be provided as {@code null}
		 * and ignored. For the {@code Id} or {@link
		 * com.liferay.apio.architect.annotation.ParentId} parameters, the
		 * annotation class should be provided to the list.
		 * </p>
		 *
		 * @param  annotations the annotations
		 * @return the {@code FinalStep}
		 * @review
		 */
		public FinalStep annotatedWith(Annotation annotations);

		/**
		 * Provides information about the parameters the action needs.
		 *
		 * <p>
		 * The parameter instances must be provided to the method {@link
		 * ActionSemantics#execute(List)} in the same order as their classes in
		 * this method. {@code Void} classes will be provided as {@code null}
		 * and ignored. For the {@code Id} or {@link
		 * com.liferay.apio.architect.annotation.ParentId} parameters, the
		 * annotation class should be provided to the list.
		 * </p>
		 *
		 * @param  annotations the annotations
		 * @return the {@code FinalStep}
		 * @review
		 */
		public FinalStep annotatedWith(Annotation... annotations);

		/**
		 * Creates the {@code ActionSemantics} object with the information
		 * provided to the builder.
		 *
		 * @return the {@code ActionSemantics} object
		 */
		public ActionSemantics build();

		/**
		 * Provides information about the form and function used to transform
		 * the body value into the object the action needs. Don't call this
		 * method if the action doesn't need information from the body.
		 *
		 * @param  form the form
		 * @param  function2 the function
		 * @return the {@code FinalStep}
		 * @review
		 */
		public FinalStep form(
			Form form, Function2<Form, Body, Object> function2);

		/**
		 * Provides information about the parameters the action needs.
		 *
		 * <p>
		 * The parameter instances must be provided to the method {@link
		 * ActionSemantics#execute(List)} in the same order as their classes in
		 * this method. {@code Void} classes will be provided as {@code null}
		 * and ignored. For the {@code Id} or {@link
		 * com.liferay.apio.architect.annotation.ParentId} parameters, the
		 * annotation class should be provided to the list.
		 * </p>
		 *
		 * @param  classes the classes
		 * @return the {@code FinalStep}
		 * @review
		 */
		public FinalStep receivesParams(Class<?>... classes);

	}

	public interface MethodStep {

		/**
		 * Provides information about the method (supplied to this method as an
		 * {@code HTTPMethod}) in which the action is executed.
		 *
		 * @param  httpMethod the method
		 * @return the {@code ReturnStep}
		 * @review
		 */
		public default ReturnStep method(HTTPMethod httpMethod) {
			return method(httpMethod.name());
		}

		/**
		 * Provides information about the method (supplied to this method as a
		 * {@code String}) in which the action is executed.
		 *
		 * @param  method the method
		 * @return the {@code ReturnStep}
		 * @review
		 */
		public ReturnStep method(String method);

	}

	public interface NameStep {

		/**
		 * Provides information about the action's name.
		 *
		 * @param  name the action's name
		 * @return the {@code MethodStep}
		 * @review
		 */
		public MethodStep name(String name);

	}

	public interface PermissionStep {

		/**
		 * Returns the default empty implementation of the permission function.
		 *
		 * @return the {@code ExecuteStep}
		 * @review
		 */
		public ExecuteStep permissionFunction();

		/**
		 * Provides information about the permission function to check if the
		 * execute function has permissions to be executed.
		 *
		 * @param  permissionCheckedFunction1 the permission function
		 * @return the {@code ExecuteStep}
		 * @review
		 */
		public ExecuteStep permissionFunction(
			CheckedFunction1<List<?>, Boolean> permissionCheckedFunction1);

	}

	public interface ReturnStep {

		/**
		 * Provides information about the class returned by the action.
		 *
		 * @param  returnClass the class returned by the action
		 * @return the {@code PermissionStep}
		 * @review
		 */
		public PermissionStep returns(Class<?> returnClass);

	}

	private List<Annotation> _annotations = new ArrayList<>();
	private Function<Body, Object> _bodyFunction = __ -> null;
	private CheckedFunction1<List<?>, ?> _executeCheckedFunction1;
	private Form _form;
	private String _method;
	private String _name;
	private List<Class<?>> _paramClasses = new ArrayList<>();
	private CheckedFunction1<List<?>, Boolean> _permissionCheckedFunction1;
	private List<Class<?>> _permissionProvidedClasses = new ArrayList<>();
	private Resource _resource;
	private Class<?> _returnClass;

}