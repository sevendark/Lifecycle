/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright (c) 2013-2020 Madz. All rights reserved.
 * 
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License"). You
 * may not use this file except in compliance with the License. You can
 * obtain a copy of the License at
 * https://raw.github.com/zhongdj/Lifecycle/master/License.txt
 * . See the License for the specific
 * language governing permissions and limitations under the License.
 * 
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 * 
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 * 
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license." If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above. However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package net.imadz.lifecycle.meta.builder.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.imadz.bcel.intercept.LifecycleInterceptContext;
import net.imadz.bcel.intercept.UnlockableStack;
import net.imadz.lifecycle.AbsStateMachineRegistry;
import net.imadz.lifecycle.LifecycleCommonErrors;
import net.imadz.lifecycle.LifecycleContext;
import net.imadz.lifecycle.LifecycleEventHandler;
import net.imadz.lifecycle.LifecycleException;
import net.imadz.lifecycle.LifecycleLockStrategy;
import net.imadz.lifecycle.StateConverter;
import net.imadz.lifecycle.SyntaxErrors;
import net.imadz.lifecycle.annotations.LifecycleLock;
import net.imadz.lifecycle.annotations.LifecycleMeta;
import net.imadz.lifecycle.annotations.StateIndicator;
import net.imadz.lifecycle.annotations.Event;
import net.imadz.lifecycle.annotations.action.Condition;
import net.imadz.lifecycle.annotations.action.ConditionalEvent;
import net.imadz.lifecycle.annotations.relation.Parent;
import net.imadz.lifecycle.annotations.relation.Relation;
import net.imadz.lifecycle.annotations.state.Converter;
import net.imadz.lifecycle.impl.LifecycleContextImpl;
import net.imadz.lifecycle.impl.LifecycleEventImpl;
import net.imadz.lifecycle.meta.builder.ConditionObjectBuilder;
import net.imadz.lifecycle.meta.builder.StateMachineMetaBuilder;
import net.imadz.lifecycle.meta.builder.StateMachineObjectBuilder;
import net.imadz.lifecycle.meta.builder.impl.helpers.CallbackMethodConfigureScanner;
import net.imadz.lifecycle.meta.builder.impl.helpers.CallbackMethodVerificationScanner;
import net.imadz.lifecycle.meta.builder.impl.helpers.ConditionProviderMethodScanner;
import net.imadz.lifecycle.meta.builder.impl.helpers.CoverageVerifier;
import net.imadz.lifecycle.meta.builder.impl.helpers.MethodSignatureScanner;
import net.imadz.lifecycle.meta.builder.impl.helpers.MethodWrapper;
import net.imadz.lifecycle.meta.builder.impl.helpers.RelationGetterConfigureScanner;
import net.imadz.lifecycle.meta.builder.impl.helpers.RelationGetterScanner;
import net.imadz.lifecycle.meta.builder.impl.helpers.RelationIndicatorPropertyMethodScanner;
import net.imadz.lifecycle.meta.builder.impl.helpers.RelationObjectConfigure;
import net.imadz.lifecycle.meta.builder.impl.helpers.ScannerForVerifyConditionCoverage;
import net.imadz.lifecycle.meta.builder.impl.helpers.StateIndicatorDefaultMethodScanner;
import net.imadz.lifecycle.meta.builder.impl.helpers.StateIndicatorGetterMethodScanner;
import net.imadz.lifecycle.meta.builder.impl.helpers.EventMethodScanner;
import net.imadz.lifecycle.meta.object.ConditionObject;
import net.imadz.lifecycle.meta.object.FunctionMetadata;
import net.imadz.lifecycle.meta.object.RelationObject;
import net.imadz.lifecycle.meta.object.StateMachineObject;
import net.imadz.lifecycle.meta.object.StateObject;
import net.imadz.lifecycle.meta.object.EventObject;
import net.imadz.lifecycle.meta.type.ConditionMetadata;
import net.imadz.lifecycle.meta.type.RelationConstraintMetadata;
import net.imadz.lifecycle.meta.type.RelationMetadata;
import net.imadz.lifecycle.meta.type.StateMachineMetadata;
import net.imadz.lifecycle.meta.type.StateMetadata;
import net.imadz.lifecycle.meta.type.EventMetadata;
import net.imadz.meta.KeySet;
import net.imadz.util.ConverterAccessor;
import net.imadz.util.EagerSetterImpl;
import net.imadz.util.KeyedList;
import net.imadz.util.LazySetterImpl;
import net.imadz.util.MethodScanCallback;
import net.imadz.util.MethodScanner;
import net.imadz.util.Readable;
import net.imadz.util.Setter;
import net.imadz.util.StateAccessible;
import net.imadz.util.StateFieldAccessor;
import net.imadz.util.StatePropertyAccessor;
import net.imadz.util.StringUtil;
import net.imadz.utils.Null;
import net.imadz.verification.VerificationException;
import net.imadz.verification.VerificationFailureSet;

import static net.imadz.lifecycle.meta.builder.impl.helpers.MethodOverridingUtils.overridesBy;

public class StateMachineObjectBuilderImpl<S>
		extends
		ObjectBuilderBase<StateMachineObject<S>, StateMachineObject<S>, StateMachineMetadata>
		implements StateMachineObjectBuilder<S> {

	private final HashMap<EventMetadata, LinkedList<EventObject>> eventMetadataMap = new HashMap<EventMetadata, LinkedList<EventObject>>();
	private final KeyedList<EventObject> eventObjectList = new KeyedList<EventObject>();
	private final KeyedList<ConditionObject> conditionObjectList = new KeyedList<ConditionObject>();
	@SuppressWarnings("rawtypes")
	private final KeyedList<StateObject> stateObjectList = new KeyedList<StateObject>();
	private final KeyedList<RelationObject> relationObjectList = new KeyedList<RelationObject>();
	private final ArrayList<CallbackObject> specificPreStateChangeCallbackObjects = new ArrayList<CallbackObject>();
	private final ArrayList<CallbackObject> specificPostStateChangeCallbackObjects = new ArrayList<CallbackObject>();
	private final ArrayList<CallbackObject> commonPreStateChangeCallbackObjects = new ArrayList<CallbackObject>();
	private final ArrayList<CallbackObject> commonPostStateChangeCallbackObjects = new ArrayList<CallbackObject>();
	private final ArrayList<EventCallbackObject> commonEventCallbackObjects = new ArrayList<EventCallbackObject>();
	private StateAccessible<String> stateAccessor;
	@SuppressWarnings("unused")
	private RelationObject parentRelationObject;
	private LifecycleLockStrategy lifecycleLockStrategry;
	private StateConverter<S> stateConverter;
	private final Map<StateAccessible<String>, String> initialStates = new HashMap<StateAccessible<String>, String>();

	public StateMachineObjectBuilderImpl(StateMachineMetaBuilder template,
			String name) {
		super(null, name);
		this.setMetaType(template);
	}

	@Override
	public void addCommonPostStateChangeCallbackObject(CallbackObject item) {
		for (CallbackObject callbackObject : commonPostStateChangeCallbackObjects) {
			if (callbackObject.overrides(item)) return;
		}
		this.commonPostStateChangeCallbackObjects.add(item);
	}

	@Override
	public void addCommonPreStateChangeCallbackObject(CallbackObject item) {
		for (CallbackObject callbackObject : commonPreStateChangeCallbackObjects) {
			if (callbackObject.overrides(item)) return;
		}
		this.commonPreStateChangeCallbackObjects.add(item);
	}

	public void addRelation(Class<?> klass,
			final RelationObject relationObject, final Object primaryKey) {
		this.relationObjectList.add(relationObject);
		// [TODO] [Tracy] Need to test parent
		if (isParentRelation(klass)) {
			this.parentRelationObject = relationObject;
		}
	}

	@Override
	public void addSpecificPostStateChangeCallbackObject(CallbackObject item) {
		for (CallbackObject callbackObject : specificPostStateChangeCallbackObjects) {
			if (callbackObject.overrides(item)) return;
		}
		this.specificPostStateChangeCallbackObjects.add(item);
	}

	@Override
	public void addSpecificPreStateChangeCallbackObject(CallbackObject item) {
		for (CallbackObject callbackObject : specificPreStateChangeCallbackObjects) {
			if (callbackObject.overrides(item)) return;
		}
		this.specificPreStateChangeCallbackObjects.add(item);
	}

	@Override
	public StateMachineObjectBuilder<S> build(Class<?> klass,
			StateMachineObject<S> parent) throws VerificationException {
		setPrimaryKey(klass);
		addKey(klass);
		addKeys(getMetaType().getKeySet());
		verifySyntax(klass);
		configureStateIndicatorAccessor(klass);
		configureConditions(klass);
		configureEventObjects(klass);
		configureStateObjects(klass);
		configureRelationObject(klass);
		configureLifecycleLock(klass);
		configureCallbacks(klass);
		return this;
	}

	private void checkRelationInstanceWhetherExists(Class<?> klass,
			final Set<Class<?>> relations, final Relation relation)
			throws VerificationException {
		if (null != relation) {
			if (relations.contains(relation.value())) {
				throw newVerificationException(getDottedPath(),
						SyntaxErrors.LM_RELATION_INSTANCE_MUST_BE_UNIQUE,
						klass.getName(), relation.value().getName());
			}
			relations.add(relation.value());
		}
	}

	private void configureCallbacks(Class<?> klass)
			throws VerificationException {
		final CallbackMethodConfigureScanner scanner = new CallbackMethodConfigureScanner(
				this, klass);
		scanner.scanMethod();
	}

	protected void configureCondition(Class<?> klass, Method method,
			ConditionMetadata conditionMetadata) throws VerificationException {
		ConditionObjectBuilder builder = new ConditionObjectBuilderImpl(this,
				method, conditionMetadata);
		builder.build(klass, this);
		conditionObjectList.add(builder.getMetaData());
	}

	private void configureConditions(final Class<?> klass) {
		MethodScanner.scanMethodsOnClasses(klass, new MethodScanCallback() {

			@Override
			public boolean onMethodFound(Method method) {
				if (method.isBridge()) return false;
				final Condition conditionMeta = method
						.getAnnotation(Condition.class);
				if (null == conditionMeta) {
					return false;
				}
				final ConditionMetadata conditionMetadata = getMetaType()
						.getCondtion(conditionMeta.value());
				try {
					configureCondition(klass, method, conditionMetadata);
				} catch (VerificationException e) {
					throw new IllegalStateException(e);
				}
				return false;
			}
		});
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void configureFieldStateAccessor(Field stateField) {
		if (String.class.equals(stateField.getType())) {
			this.stateAccessor = new StateFieldAccessor<String>(stateField);
		} else {
			try {
				this.stateConverter = (StateConverter<S>) stateField
						.getAnnotation(Converter.class).value().newInstance();
				this.stateAccessor = new ConverterAccessor(stateConverter,
						new StateFieldAccessor(stateField));
			} catch (InstantiationException e) {
				throw new IllegalStateException(e);
			} catch (IllegalAccessException e) {
				throw new IllegalStateException(e);
			}
		}
	}

	private void configureLifecycleLock(Class<?> klass)
			throws VerificationException {
		final LifecycleLock annotation = klass
				.getAnnotation(LifecycleLock.class);
		if (annotation != null) {
			try {
				this.lifecycleLockStrategry = annotation.value().newInstance();
			} catch (InstantiationException e) {
				throw newVerificationException(
						getDottedPath(),
						SyntaxErrors.LIFECYCLE_LOCK_SHOULD_HAVE_NO_ARGS_CONSTRUCTOR,
						annotation.value());
			} catch (IllegalAccessException e) {
				throw newVerificationException(
						getDottedPath(),
						SyntaxErrors.LIFECYCLE_LOCK_SHOULD_HAVE_NO_ARGS_CONSTRUCTOR,
						annotation.value());
			}
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void configurePropertyAccessor(Class<?> klass, Method getter) {
		final String setterName = convertSetterName(getter.getName(),
				getter.getReturnType());
		final Setter setter = configureSetter(klass, getter, setterName);
		if (String.class.equals(getter.getReturnType())) {
			this.stateAccessor = new StatePropertyAccessor<String>(getter,
					setter);
		} else {
			try {
				this.stateConverter = (StateConverter<S>) getter
						.getAnnotation(Converter.class).value().newInstance();
				this.stateAccessor = new ConverterAccessor(stateConverter,
						new StatePropertyAccessor(getter, setter));
			} catch (InstantiationException e) {
				throw new IllegalStateException(e);
			} catch (IllegalAccessException e) {
				throw new IllegalStateException(e);
			}
		}
	}

	private void configureRelationObject(Class<?> klass)
			throws VerificationException {
		final ArrayList<RelationMetadata> extendedRelationMetadata = new ArrayList<RelationMetadata>();
		configureRelationObjectsFromField(klass, extendedRelationMetadata);
		configureRelationObjectsOnProperties(klass, extendedRelationMetadata);
	}

	private void configureRelationObjectsFromField(final Class<?> klass,
			final ArrayList<RelationMetadata> extendedRelationMetadata)
			throws VerificationException {
		if (null == klass || klass.isInterface() || Object.class == klass) {
			return;
		}
		for (Class<?> clazz = klass; null != clazz && clazz != Object.class; clazz = clazz
				.getSuperclass()) {
			for (Field field : clazz.getDeclaredFields()) {
				if (null == field.getAnnotation(Relation.class)) {
					continue;
				}
				final Object relationKey = getRelationKey(field);
				final StateMachineMetadata relatedStateMachine = getMetaType()
						.getRegistry().loadStateMachineMetadata(
								clazz.getAnnotation(LifecycleMeta.class)
										.value());
				final RelationObjectConfigure configure = new RelationObjectConfigure(
						klass, extendedRelationMetadata, this);
				final RelationObject relationObject = configure.configure(
						relatedStateMachine, relationKey, field);
				if (null == relationObject) {
					continue;
				} else {
					addRelation(klass, relationObject, relationObject
							.getMetaType().getPrimaryKey());
				}
			}
		}
	}

	private void configureRelationObjectsOnProperties(Class<?> klass,
			ArrayList<RelationMetadata> extendedRelationMetadata)
			throws VerificationException {
		if (Object.class == klass || null == klass) {
			return;
		}
		final VerificationFailureSet failureSet = new VerificationFailureSet();
		RelationGetterConfigureScanner scanner = new RelationGetterConfigureScanner(
				klass, this, this, failureSet, extendedRelationMetadata);
		MethodScanner.scanMethodsOnClasses(klass, scanner);
		if (0 < failureSet.size())
			throw new VerificationException(failureSet);
	}

	private Setter<?> configureSetter(Class<?> klass, Method getter,
			final String setterName) {
		if (klass.isInterface()) {
			return new LazySetterImpl(getter);
		} else {
			final Method setterMethod = findMethod(klass, setterName,
					getter.getReturnType());
			return new EagerSetterImpl(setterMethod);
		}
	}

	private void configureStateIndicatorAccessor(Class<?> klass)
			throws VerificationException {
		if (!klass.isInterface()) {
			Field specifiedStateField = findFieldWith(klass,
					StateIndicator.class);
			if (null != specifiedStateField) {
				configureFieldStateAccessor(specifiedStateField);
				return;
			}
		}
		final Method specifiedGetter = findCustomizedStateIndicatorGetter(klass);
		if (null != specifiedGetter) {
			configurePropertyAccessor(klass, specifiedGetter);
		} else {
			final Method defaultGetter = findDefaultStateGetterMethod(klass);
			configurePropertyAccessor(klass, defaultGetter);
		}
	}

	private void configureStateObjects(Class<?> klass)
			throws VerificationException {
		final StateMetadata[] allStates = getMetaType().getAllStates();
		for (StateMetadata stateMetadata : allStates) {
			StateObjectBuilderImpl<S> stateObject = new StateObjectBuilderImpl<S>(
					this, stateMetadata);
			stateObject.setRegistry(getRegistry());
			stateObject.build(klass, this);
			this.stateObjectList.add(stateObject.getMetaData());
			if (stateMetadata.isInitial()) {
				initialStates.put(this.stateAccessor, stateMetadata.getSimpleName());
			}
		}
	}

	private void configureEventObject(final Class<?> klass,
			final Method method, final EventMetadata eventMetadata)
			throws VerificationException {
		final EventObjectBuilderImpl eventObjectBuilder = new EventObjectBuilderImpl(
				this, method, eventMetadata);
		eventObjectBuilder.build(klass, this);
		final EventObject eventObject = eventObjectBuilder.getMetaData();
		eventObjectList.add(eventObject);
		if (null == eventMetadataMap.get(eventMetadata)) {
			final LinkedList<EventObject> eventObjects = new LinkedList<EventObject>();
			eventObjects.add(eventObject);
			eventMetadataMap.put(eventMetadata, eventObjects);
		} else {
			eventMetadataMap.get(eventMetadata).add(eventObject);
		}
	}

	private void configureEventObjects(final Class<?> klass) {
		MethodScanner.scanMethodsOnClasses(klass, new MethodScanCallback() {

			@Override
			public boolean onMethodFound(Method method) {
				if (method.isBridge()) return false;
				final Event eventAnno = method.getAnnotation(Event.class);
				if (null == eventAnno) {
					return false;
				}
				final EventMetadata eventMetadata;
				if (Null.class == eventAnno.value()) {
					eventMetadata = getMetaType().getEvent(
							StringUtil.toUppercaseFirstCharacter(method
									.getName()));
				} else {
					eventMetadata = getMetaType().getEvent(eventAnno.value());
				}
				try {
					configureEventObject(klass, method, eventMetadata);
				} catch (VerificationException e) {
					throw new IllegalStateException(e);
				}
				return false;
			}
		});
	}

	private String convertSetterName(String getterName, Class<?> type) {
		if (type != Boolean.TYPE && type != Boolean.class) {
			return "set" + getterName.substring(3);
		} else {
			return "set" + getterName.substring(2);
		}
	}

	@SuppressWarnings("unchecked")
	private ConditionalEvent<Object> createConditionalEvent(
			final FunctionMetadata functionMetadata)
			throws InstantiationException, IllegalAccessException {
		return (ConditionalEvent<Object>) functionMetadata.getEvent()
				.getJudgerClass().newInstance();
	}

	@Override
	public void doInterceptAfter(LifecycleInterceptContext context) {
		validateInboundConstraintAfterMethodInvocation(context, this);
		doUpdateNextState(context);
		performCallbacksAfterStateChange(context);
		doUnlock(context);
		fireLifecycleEvents(this, context);
	}

	@Override
	public void doInterceptBefore(LifecycleInterceptContext context) {
		doLock(context);
		String fromState = evaluateState(context.getTarget());
		context.setFromState(fromState);
		validateStateValidWhiles(context);
		validateEvent(context);
		validateInboundConstrantBeforeMethodInvocation(context, this);
		performCallbacksBeforeStateChange(context);
	}

	@Override
	public void doInterceptException(LifecycleInterceptContext lifecycleContext) {
		doUnlock(lifecycleContext);
	}

	private void doLock(LifecycleInterceptContext context) {
		if (isLockEnabled()) {
			final LifecycleLockStrategy lock = getLifecycleLockStrategy();
			lock.lockWrite(context.getTarget());
		}
		// Lock Related object is delegated to Validate both ValidWhiles and
		// InboundWhiles
	}

	private void doUnlock(LifecycleInterceptContext context) {
		context.unlockRelatedReactiveObjects();
		unlockTargetReactiveObject(context);
	}

	private void doUpdateNextState(LifecycleInterceptContext context) {
		context.logStep6_1SetupNextStateStart();
		transitToNextState(context.getTarget(), context.getEventKey());
		context.logStep6_2SetupNextStateFinsihed();
	}

	private boolean evaluateConditionBeforeEvent(Object eventKey) {
		EventMetadata event = getMetaType().getEvent(eventKey);
		return !event.postValidate();
	}

	private Object evaluateJudgeable(Object target,
			final EventMetadata eventMetadata) throws IllegalAccessException,
			InvocationTargetException {
		final ConditionObject conditionObject = getConditionObject(eventMetadata
				.getConditionClass());
		return conditionObject.conditionGetter().invoke(target);
	}

	private String evaluateNextState(Object target, Object eventKey) {
		final StateObject<S> state = getState(evaluateState(target));
		final FunctionMetadata functionMetadata = state.getMetaType()
				.getFunctionMetadata(eventKey);
		requireFunctionNotNull(eventKey, state, functionMetadata);
		if (isConditional(functionMetadata)) {
			return evaluateNextStateWithConditionalEvent(target,
					functionMetadata);
		} else {
			StateMetadata nextState = findStateFromBottomToTop(functionMetadata);
			nextState = handleCompositeStateMachineLinkage(nextState);
			return nextState.getSimpleName();
		}
	}

	private String evaluateNextStateWithConditionalEvent(Object target,
			final FunctionMetadata functionMetadata) {
		try {
			final ConditionalEvent<Object> conditionalEvent = createConditionalEvent(functionMetadata);
			final Class<?> nextStateClass = conditionalEvent
					.doConditionJudge(evaluateJudgeable(target,
							functionMetadata.getEvent()));
			final StateMetadata nextState = handleCompositeStateMachineLinkage(getState(
					nextStateClass).getMetaType());
			return nextState.getSimpleName();
		} catch (Exception e) {
			if (e instanceof InstantiationException
					| e instanceof IllegalAccessException
					| e instanceof IllegalArgumentException
					| e instanceof InvocationTargetException)
				throw new IllegalStateException(
						"Cannot create judger instance of Class: "
								+ functionMetadata.getEvent().getJudgerClass()
								+ ". Please provide no-arg constructor.");
			else
				throw new RuntimeException(e);
		}
	}

	@Override
	public String evaluateState(Object target) {
		return this.stateAccessor.read(target);
	}

	private HashMap<Class<?>, Object> evaluatorRelationsInMethodParameters(
			LifecycleInterceptContext context) {
		final Object[] arguments = context.getArguments();
		final Method method = context.getMethod();
		final Annotation[][] parameterAnnotations = method
				.getParameterAnnotations();
		final HashMap<Class<?>, Object> relationObjectMap = new HashMap<Class<?>, Object>();
		int parameterIndex = 0;
		for (Annotation[] annotations : parameterAnnotations) {
			for (Annotation annotation : annotations) {
				if (Relation.class == annotation.annotationType()) {
					relationObjectMap.put(((Relation) annotation).value(),
							arguments[parameterIndex]);
				}
			}
			parameterIndex++;
		}
		return relationObjectMap;
	}

	private Method findCustomizedStateIndicatorGetter(Class<?> klass)
			throws VerificationException {
		final VerificationFailureSet failureSet = new VerificationFailureSet();
		final StateIndicatorGetterMethodScanner scanner = new StateIndicatorGetterMethodScanner(
				this, klass, failureSet);
		MethodScanner.scanMethodsOnClasses(klass, scanner);
		if (failureSet.size() > 0)
			throw new VerificationException(failureSet);
		else {
			return scanner.getStateGetterMethod();
		}
	}

	private Method findDefaultStateGetterMethod(Class<?> klass)
			throws VerificationException {
		final StateIndicatorDefaultMethodScanner scanner = new StateIndicatorDefaultMethodScanner();
		MethodScanner.scanMethodsOnClasses(klass, scanner);
		return scanner.getDefaultMethod();
	}

	private Field findFieldWith(Class<?> klass,
			Class<? extends Annotation> aClass) throws VerificationException {
		if (klass.isInterface())
			return null;
		for (Class<?> index = klass; null != index && index != Object.class; index = index
				.getSuperclass()) {
			for (Field field : index.getDeclaredFields()) {
				if (null != field.getAnnotation(aClass)) {
					return field;
				}
			}
		}
		return null;
	}

	private Method findMethod(Class<?> klass, String setterName,
			Class<?> returnType) {
		final MethodSignatureScanner scanner = new MethodSignatureScanner(
				setterName, new Class<?>[] { returnType });
		MethodScanner.scanMethodsOnClasses(klass, scanner);
		return scanner.getMethod();
	}

	private StateMetadata findStateFromBottomToTop(
			final FunctionMetadata functionMetadata) {
		StateMetadata nextState = functionMetadata.getNextStates().get(0);
		nextState = getState(nextState.getPrimaryKey()).getMetaType();
		return nextState;
	}

	private void fireLifecycleEvents(StateMachineObject<?> stateMachine,
			LifecycleInterceptContext context) {
		context.logStep9FireLifecycleEvents();
		final LifecycleEventHandler eventHandler = AbsStateMachineRegistry
				.getInstance().getLifecycleEventHandler();
		if (null != eventHandler) {
			eventHandler.onEvent(new LifecycleEventImpl(context));
		}
	}

	private ConditionObject getConditionObject(Class<?> conditionClass) {
		return this.conditionObjectList.get(conditionClass);
	}

	private Readable<?> getEvaluator(Object relationKey) {
		if (relationObjectList.containsKey(relationKey)) {
			return (Readable<?>) relationObjectList.get(relationKey)
					.getEvaluator();
		}
		throw new IllegalStateException(
				"The evaluate is not found, which should not happen. Check the verifyRelationsAllBeCoveraged method with key:"
						+ relationKey);
	}

	@Override
	public LifecycleLockStrategy getLifecycleLockStrategy() {
		return this.lifecycleLockStrategry;
	}

	private String getMethodDottedPath(Method method) {
		return method.getDeclaringClass().getName() + "." + method.getName();
	}

	private Object getRelationInMethodParameters(
			HashMap<Class<?>, Object> relationsInMethodParameters, KeySet keySet) {
		Iterator<Object> iterator = keySet.iterator();
		while (iterator.hasNext()) {
			final Object key = iterator.next();
			if (relationsInMethodParameters.containsKey(key)) {
				return relationsInMethodParameters.get(key);
			}
		}
		return null;
	}

	private Object getRelationInstance(
			Object contextTarget,
			final HashMap<Class<?>, Object> relationsInMethodParameters,
			final Entry<String, List<RelationConstraintMetadata>> relationMetadataEntry) {
		Object relationObject = getRelationInMethodParameters(
				relationsInMethodParameters, relationMetadataEntry.getValue()
						.get(0).getKeySet());
		if (null == relationObject) {
			Readable<?> evaluator = getEvaluator(relationMetadataEntry
					.getValue().get(0).getRelationMetadata().getPrimaryKey());
			relationObject = evaluator.read(contextTarget);
		}
		return relationObject;
	}

	private Object getRelationKey(Field field) {
		final Class<?> relationClass = field.getAnnotation(Relation.class)
				.value();
		if (Null.class != relationClass) {
			return relationClass;
		} else {
			return StringUtil.toUppercaseFirstCharacter(field.getName());
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public StateObject<S> getState(Object stateKey) {
		return (StateObject<S>) stateObjectList.get(stateKey);
	}

	@SuppressWarnings("unchecked")
	private S getStateRawTypeValue(String stateName) {
		if (null == stateName || 0 >= stateName.trim().length()) {
			return null;
		}
		if (null != this.stateConverter) {
			return this.stateConverter.fromState(stateName);
		} else {
			return (S) String.valueOf(stateName);
		}
	}

	private EventMetadata[] getEventsToState(StateMetadata state) {
		final ArrayList<EventMetadata> events = new ArrayList<EventMetadata>();
		for (final StateMetadata stateMetadata : getMetaType().getAllStates()) {
			for (final EventMetadata eventMetadata : stateMetadata
					.getPossibleLeavingEvents()) {
				if (isEventIn(state, eventMetadata)) {
					events.add(eventMetadata);
				}
			}
		}
		return events.toArray(new EventMetadata[0]);
	}

	private StateMetadata handleCompositeStateMachineLinkage(
			StateMetadata nextState) {
		if (nextState.isCompositeState()) {
			nextState = nextState.getCompositeStateMachine().getInitialState();
		} else if (nextState.getStateMachine().isComposite()
				&& nextState.isFinal()) {
			nextState = nextState.getLinkTo();
		}
		if (nextState.isCompositeState()
				|| nextState.getStateMachine().isComposite()
				&& nextState.isFinal()) {
			nextState = handleCompositeStateMachineLinkage(nextState);
		}
		return nextState;
	}

	private boolean hasOnlyOneStateCandidate(
			StateMachineObject<?> stateMachine,
			LifecycleInterceptContext context) {
		final String stateName = stateMachine
				.evaluateState(context.getTarget());
		final StateMetadata state = stateMachine.getMetaType().getState(
				stateName);
		if (state.hasMultipleStateCandidatesOn(context.getEventKey())) {
			return false;
		} else {
			return true;
		}
	}

	private boolean hasRelationOnField(
			final RelationConstraintMetadata relation, Field field) {
		Relation r = field.getAnnotation(Relation.class);
		if (null == r)
			return false;
		if (Null.class != r.value()) {
			if (isKeyOfRelationMetadata(relation, r.value())) {
				return true;
			}
		} else {
			if (isKeyOfRelationMetadata(relation,
					StringUtil.toUppercaseFirstCharacter(field.getName()))) {
				return true;
			}
		}
		return false;
	}

	private boolean hasRelationOnMethodParameters(
			final RelationConstraintMetadata relation, final Method method)
			throws VerificationException {
		for (Annotation[] annotations : method.getParameterAnnotations()) {
			for (Annotation annotation : annotations) {
				if (annotation instanceof Relation) {
					Relation r = (Relation) annotation;
					if (Null.class == r.value()) {
						throw newVerificationException(
								getDottedPath(),
								SyntaxErrors.LM_RELATION_ON_METHOD_PARAMETER_MUST_SPECIFY_VALUE,
								method);
					}
					if (isKeyOfRelationMetadata(relation, r.value()))
						return true;
				}
			}
		}
		return false;
	}

	private void invokeCommonPostStateChangeCallbacks(CallbackObject cbo,
			LifecycleContext<?, S> callbackContext) {
		cbo.doCallback(callbackContext);
	}

	private void invokeCommonPreStateChangeCallbacks(
			LifecycleContext<?, S> callbackContext) {
		for (CallbackObject callbackObject : this.commonPreStateChangeCallbackObjects) {
			callbackObject.doCallback(callbackContext);
		}
	}

	private void invokeSpecificPreStateChangeCallbacks(
			LifecycleContext<?, S> callbackContext) {
		for (CallbackObject callbackObject : this.specificPreStateChangeCallbackObjects) {
			if (callbackObject.matches(callbackContext)) {
				callbackObject.doCallback(callbackContext);
			}
		}
	}

	private boolean isConditional(final FunctionMetadata functionMetadata) {
		return 1 < functionMetadata.getNextStates().size();
	}

	public boolean isKeyOfRelationMetadata(
			final RelationConstraintMetadata relation, Object key) {
		return relation.getKeySet().contains(key);
	}

	@Override
	public boolean isLockEnabled() {
		return null != lifecycleLockStrategry;
	}

	private boolean isParentRelation(Class<?> klass) {
		return null != klass.getAnnotation(Parent.class);
	}

	private boolean isEventIn(StateMetadata state, EventMetadata eventMetadata) {
		for (final StateMetadata stateMetadata : getMetaType().getAllStates()) {
			for (FunctionMetadata item : stateMetadata
					.getDeclaredFunctionMetadata()) {
				if (item.getEvent().getDottedPath()
						.equals(eventMetadata.getDottedPath())) {
					for (StateMetadata nextState : item.getNextStates()) {
						if (nextState.getDottedPath() == state.getDottedPath()) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	private HashMap<String, List<RelationConstraintMetadata>> mergeRelations(
			RelationConstraintMetadata[] relations) {
		final HashMap<String, List<RelationConstraintMetadata>> mergedRelations = new HashMap<String, List<RelationConstraintMetadata>>();
		for (final RelationConstraintMetadata relationMetadata : relations) {
			final String relationKey = relationMetadata
					.getRelatedStateMachine().getDottedPath().getAbsoluteName();
			if (mergedRelations.containsKey(relationKey)) {
				final List<RelationConstraintMetadata> list = mergedRelations
						.get(relationKey);
				list.add(relationMetadata);
			} else {
				final ArrayList<RelationConstraintMetadata> list = new ArrayList<RelationConstraintMetadata>();
				list.add(relationMetadata);
				mergedRelations.put(relationKey, list);
			}
		}
		return mergedRelations;
	}

	private boolean nextStateCanBeEvaluatedBeforeTranstion(
			StateMachineObject<?> stateMachine,
			LifecycleInterceptContext context) {
		if (hasOnlyOneStateCandidate(stateMachine, context)) {
			return true;
		} else if (evaluateConditionBeforeEvent(context.getEventKey())) {
			return true;
		}
		return false;
	}

	private void performCallbacksAfterStateChange(
			LifecycleInterceptContext context) {
		context.logStep7Callback();
		performPostStateChangeCallback(context);
		context.logStep8Callback();
		performOnEventCallback(context);
	}

	private void performOnEventCallback(LifecycleInterceptContext context) {
		final S fromStateType = this.getStateRawTypeValue(context.getFromState());
        S toStateType = null;
        if ( null != context.getToState() ) {
            toStateType = this.getStateRawTypeValue(context.getToState());
        }
        final LifecycleContext<?, S> callbackContext = new LifecycleContextImpl(context, fromStateType, toStateType);
        final Object eventKey = context.getEventKey();
        getEvent(eventKey).invokeEventCallbacks(callbackContext);
        for (EventCallbackObject commonCallback : commonEventCallbackObjects) {
        	commonCallback.doCallback(callbackContext);
        }
	}

	private void performCallbacksBeforeStateChange(
			LifecycleInterceptContext context) {
		context.logStep4PreStateChangeCallback();
		performPreStateChangeCallback(context);
	}

	private void performPostStateChangeCallback(
			LifecycleInterceptContext context) {
		final S fromStateType = this.getStateRawTypeValue(context
				.getFromState());
		S toStateType = null;
		if (null != context.getToState()) {
			toStateType = this.getStateRawTypeValue(context.getToState());
		}
		final LifecycleContext<?, S> callbackContext = new LifecycleContextImpl(
				context, fromStateType, toStateType);
		final String fromStateName = callbackContext.getFromStateName();
		final String toStateName = callbackContext.getToStateName();
		final LinkedList<CallbackObject> sortedCallbackObjects = getSortedCallbackObjects(
				fromStateName, toStateName);
		doCallbackObjects(callbackContext, sortedCallbackObjects, fromStateName, toStateName);
	}

	private void doCallbackObjects(
			final LifecycleContext<?, S> callbackContext,
			final LinkedList<CallbackObject> allCallbacks, final String fromStateName, final String toStateName) {
		for (CallbackObject cbo : allCallbacks) {
			if (cbo.getGeneralize() == CallbackObject.SPECIFIC) {
				invokeSpecificPostStateChangeCallbacks(cbo, callbackContext);
			} else if (cbo.getGeneralize() == CallbackObject.FROM) {
				getState(fromStateName).invokeFromPostStateChangeCallbacks(cbo,
						callbackContext);
			} else if (cbo.getGeneralize() == CallbackObject.TO) {
				getState(toStateName).invokeToPostStateChangeCallbacks(cbo,
						callbackContext);
			} else {
				invokeCommonPostStateChangeCallbacks(cbo, callbackContext);
			}
		}
	}

	private LinkedList<CallbackObject> getSortedCallbackObjects(
			final String fromState, final String toState) {
		LinkedList<CallbackObject> allCallbacks = new LinkedList<CallbackObject>();
		allCallbacks = addToList(allCallbacks, this.specificPostStateChangeCallbackObjects);
		allCallbacks = addToList(allCallbacks, getState(fromState).getPostFromCallbackObjects(fromState));
		allCallbacks = addToList(allCallbacks, getState(toState).getPostToCallbackObjects(toState));
		allCallbacks = addToList(allCallbacks, this.commonPostStateChangeCallbackObjects);
		Collections.sort(allCallbacks);
		return allCallbacks;
	}

	private LinkedList<CallbackObject> addToList(final LinkedList<CallbackObject> result, List<CallbackObject> others) {
		if (null == others) return result;
		for (CallbackObject cbo: others) {
			result.add(cbo);
		}
		return result;
	}
	private void invokeSpecificPostStateChangeCallbacks(CallbackObject cbo,
			LifecycleContext<?, S> callbackContext) {
		if (cbo.matches(callbackContext)) {
			cbo.doCallback(callbackContext);
		}
	}

	private void performPreStateChangeCallback(LifecycleInterceptContext context) {
		final S fromStateType = this.getStateRawTypeValue(context
				.getFromState());
		S toStateType = null;
		if (null != context.getToState()) {
			toStateType = this.getStateRawTypeValue(context.getToState());
		}
		final LifecycleContext<?, S> callbackContext = new LifecycleContextImpl(
				context, fromStateType, toStateType);
		final String fromState = callbackContext.getFromStateName();
		final String toState = callbackContext.getToStateName();
		if (null != toState) {
			invokeSpecificPreStateChangeCallbacks(callbackContext);
		}
		final StateObject<S> fromStateObject = getState(fromState);
		fromStateObject.invokeFromPreStateChangeCallbacks(callbackContext);
		if (null != toState) {
			final StateObject<S> toStateObject = getState(toState);
			toStateObject.invokeToPreStateChangeCallbacks(callbackContext);
		}
		invokeCommonPreStateChangeCallbacks(callbackContext);
	}

	private void requireFunctionNotNull(Object eventKey,
			final StateObject<S> state, final FunctionMetadata functionMetadata) {
		if (null == functionMetadata) {
			throw new IllegalArgumentException(
					"Invalid Key or Key not registered: " + eventKey
							+ " while searching function metadata from state: "
							+ state);
		}
	}

	private boolean scanFieldsRelation(Class<?> klass,
			final RelationConstraintMetadata relation) {
		for (Class<?> c = klass; null != c && Object.class != c; c = c.getSuperclass()) {
			for (Field field : c.getDeclaredFields()) {
				if (hasRelationOnField(relation, field))
					return true;
			}
		}
		return false;
	}

	private String transitToNextState(Object target, Object eventKey) {
		final String stateName = evaluateNextState(target, eventKey);
		this.stateAccessor.write(target, stateName);
		return stateName;
	}

	private void unlockTargetReactiveObject(LifecycleInterceptContext context) {
		if (isLockEnabled()) {
			final LifecycleLockStrategy lockStrategry = getLifecycleLockStrategy();
			if (null != lockStrategry) {
				lockStrategry.unlockWrite(context.getTarget());
			}
		}
	}

	private void validateInboundConstraintAfterMethodInvocation(
			LifecycleInterceptContext context,
			final StateMachineObject<?> stateMachine) {
		context.logStep5ValiatingInbound();
		if (!nextStateCanBeEvaluatedBeforeTranstion(stateMachine, context)) {
			validateInboundWhiles(context);
		}
	}

	private void validateInboundConstrantBeforeMethodInvocation(
			LifecycleInterceptContext context,
			final StateMachineObject<?> stateMachine) {
		context.logStep3ValidateInboundConstrantBeforeMethodInvocation();
		if (nextStateCanBeEvaluatedBeforeTranstion(stateMachine, context)) {
			validateInboundWhiles(context);
		}
	}

	private void validateInboundWhiles(LifecycleInterceptContext context) {
		final HashMap<Class<?>, Object> relationsInMethodParameters = evaluatorRelationsInMethodParameters(context);
		final Object target = context.getTarget();
		final StateMetadata state = getMetaType().getState(
				evaluateState(target));
		final String nextState = evaluateNextState(target,
				context.getEventKey());
		final StateMetadata nextStateMetadata = getMetaType().getState(
				nextState);
		for (final Entry<String, List<RelationConstraintMetadata>> relationMetadataEntry : mergeRelations(
				nextStateMetadata.getInboundWhiles()).entrySet()) {
			final Object relationTarget = getRelationInstance(target,
					relationsInMethodParameters, relationMetadataEntry);
			if (null != relationTarget) {
				getState(state.getDottedPath())
						.verifyInboundWhileAndLockRelatedObjects(
								context.getEventKey(),
								target,
								nextState,
								relationMetadataEntry.getValue().toArray(
										new RelationConstraintMetadata[0]),
								relationTarget, context);
			} else {
				validateRequiredRelationExist(
						state,
						relationMetadataEntry,
						LifecycleCommonErrors.INBOUND_WHILE_RELATION_TARGET_IS_NULL);
			}
		}
		context.setNextState(nextState);
	}

	private void validateRequiredRelationExist(
			final StateMetadata state,
			final Entry<String, List<RelationConstraintMetadata>> relationMetadataEntry,
			String errorCode) {
		for (final RelationConstraintMetadata item : relationMetadataEntry
				.getValue()) {
			if (!item.isNullable()) {
				throw new LifecycleException(getClass(),
						LifecycleCommonErrors.BUNDLE, errorCode,
						item.getPrimaryKey(),
						"nullable = " + item.isNullable(),
						state.getPrimaryKey());
			}
		}
	}

	private void validateStateValidWhiles(LifecycleInterceptContext context) {
		context.logStep1ValidateCurrentState();
		validateValidWhiles(context.getTarget(), context);
	}

	private void validateEvent(LifecycleInterceptContext context) {
		context.logStep2validateEvent();
		final EventMetadata event = validateEvent(context.getTarget(),
				context.getFromState(), context.getEventKey());
		context.setEventType(event.getType());
		context.setEventName(event.getDottedPath().getName());
	}

	private EventMetadata validateEvent(final Object target,
			final String fromState, final Object eventKey) {
		final StateMetadata stateMetadata = this.getMetaType().getState(
				fromState);
		if (!stateMetadata.isEventValid(eventKey)) {
			throw new LifecycleException(getClass(), "lifecycle_common",
					LifecycleCommonErrors.ILLEGAL_EVENT_ON_STATE, eventKey,
					fromState, target);
		} else {
			return stateMetadata.getEvent(eventKey);
		}
	}

	@Override
	public void validateValidWhiles(final Object target,
			final UnlockableStack stack) {
		final StateMetadata state = getMetaType().getState(
				evaluateState(target));
		final HashMap<String, List<RelationConstraintMetadata>> mergedRelations = mergeRelations(state
				.getValidWhiles());
		final StateObject<S> stateObject = getState(state.getDottedPath());
		for (final Entry<String, List<RelationConstraintMetadata>> relationMetadataEntry : mergedRelations
				.entrySet()) {
			final Object relationInstance = getRelationInstance(target,
					new HashMap<Class<?>, Object>(), relationMetadataEntry);
			if (null != relationInstance) {
				stateObject.verifyValidWhile(target, relationMetadataEntry
						.getValue().toArray(new RelationConstraintMetadata[0]),
						relationInstance, stack);
			} else {
				validateRequiredRelationExist(
						state,
						relationMetadataEntry,
						LifecycleCommonErrors.VALID_WHILE_RELATION_TARGET_IS_NULL);
			}
		}
	}

	private void verifyAllConditionBeCovered(Class<?> klass)
			throws VerificationException {
		for (ConditionMetadata conditionMetadata : getMetaType()
				.getAllCondtions()) {
			verifyConditionBeCovered(klass, conditionMetadata);
		}
	}

	private void verifyAllEventsCoverage(Class<?> klass,
			VerificationFailureSet failureSet) {
		for (EventMetadata eventMetadata : getMetaType().getAllEvents()) {
			verifyEventBeCovered(klass, eventMetadata, failureSet);
		}
	}

	private void verifyCallbackMethods(Class<?> klass)
			throws VerificationException {
		final VerificationFailureSet failureSet = new VerificationFailureSet();
		final CallbackMethodVerificationScanner scanner = new CallbackMethodVerificationScanner(
				this, failureSet);
		MethodScanner.scanMethodsOnClasses(klass, scanner);
		if (failureSet.size() > 0) {
			throw new VerificationException(failureSet);
		}
	}

	private void verifyConditionBeCovered(Class<?> klass,
			final ConditionMetadata conditionMetadata)
			throws VerificationException {
		final ScannerForVerifyConditionCoverage scanner = new ScannerForVerifyConditionCoverage(
				conditionMetadata);
		MethodScanner.scanMethodsOnClasses(klass, scanner);
		if (!scanner.isCovered()) {
			throw newVerificationException(getDottedPath(),
					SyntaxErrors.LM_CONDITION_NOT_COVERED, klass, getMetaType()
							.getDottedPath(), conditionMetadata.getDottedPath());
		}
	}

	private void verifyConditionReferenceValid(Class<?> klass)
			throws VerificationException {
		final VerificationFailureSet failureSet = new VerificationFailureSet();
		MethodScanner.scanMethodsOnClasses(klass,
				new ConditionProviderMethodScanner(this, klass, getMetaType(),
						failureSet));
		if (failureSet.size() > 0) {
			throw new VerificationException(failureSet);
		}
	}

	private void verifyConditions(Class<?> klass) throws VerificationException {
		verifyConditionReferenceValid(klass);
		verifyAllConditionBeCovered(klass);
	}

	private boolean verifyCustomizedStateIndicatorGetter(Class<?> klass)
			throws VerificationException {
		final Method specifiedGetter = findCustomizedStateIndicatorGetter(klass);
		if (null != specifiedGetter) {
			verifyStateIndicatorElement(klass, specifiedGetter,
					specifiedGetter.getReturnType());
			return true;
		} else {
			return false;
		}
	}

	private boolean verifyDefaultStateGetterMethod(Class<?> klass)
			throws VerificationException {
		final Method defaultGetter = findDefaultStateGetterMethod(klass);
		if (null != defaultGetter) {
			verifyStateIndicatorElement(klass, defaultGetter,
					defaultGetter.getReturnType());
			return true;
		} else {
			return false;
		}
	}

	@Override
	public void verifyMetaData(VerificationFailureSet verificationSet) {
		// TODO Auto-generated method stub
	}

	private void verifyRelationBeCovered(Class<?> klass,
			final RelationConstraintMetadata relation, final EventMetadata event)
			throws VerificationException {
		final EventMethodScanner scanner = new EventMethodScanner(event);
		MethodScanner.scanMethodsOnClasses(klass, scanner);
		final Method[] eventMethods = scanner.getEventMethods();
		NEXT_EVENT_METHOD: for (final Method method : eventMethods) {
			if (hasRelationOnMethodParameters(relation, method)) {
				continue NEXT_EVENT_METHOD;
			}
			// Continue to check in field and property method
			if (!klass.isInterface() && scanFieldsRelation(klass, relation)) {
				continue NEXT_EVENT_METHOD;
			}
			final RelationGetterScanner relationGetterScanner = new RelationGetterScanner(
					this, relation);
			MethodScanner.scanMethodsOnClasses(klass, relationGetterScanner);
			if (relationGetterScanner.isCovered()) {
				continue NEXT_EVENT_METHOD;
			}
			throw new VerificationException(newVerificationFailure(
					getDottedPath(), SyntaxErrors.LM_RELATION_NOT_BE_CONCRETED,
					method.getName(), klass.getName(), relation.getDottedPath()
							.getName(), relation.getParent().getDottedPath()));
		}
	}

	private void verifyRelationInstanceNotBeyondStateMachine(Class<?> klass)
			throws VerificationException {
		if (null == klass) {
			return;
		}
		verifyRelationInstanceOnFieldNotBeyondStateMachine(klass);
		verifyRelationInstanceOnMethodNotBeyondStateMachine(klass);
	}

	private void verifyRelationInstanceOnFieldNotBeyondStateMachine(
			Class<?> klass) throws VerificationException {
		if (null == klass || klass.isInterface() || Object.class == klass) {
			return;
		}
		for (Field field : klass.getDeclaredFields()) {
			Relation relation = field.getAnnotation(Relation.class);
			if (null == relation) {
				continue;
			}
			Class<?> relationClass = relation.value();
			if (Null.class != relationClass) {
				if (!getMetaType().hasRelation(relationClass)) {
					throw new VerificationException(
							newVerificationFailure(
									getDottedPath(),
									SyntaxErrors.LM_REFERENCE_INVALID_RELATION_INSTANCE,
									klass.getName(), relationClass.getName(),
									getMetaType().getDottedPath()
											.getAbsoluteName()));
				}
			} else {
				if (!getMetaType().hasRelation(
						StringUtil.toUppercaseFirstCharacter(field.getName()))) {
					throw new VerificationException(
							newVerificationFailure(
									getDottedPath(),
									SyntaxErrors.LM_REFERENCE_INVALID_RELATION_INSTANCE,
									klass.getName(), relationClass.getName(),
									getMetaType().getDottedPath()
											.getAbsoluteName()));
				}
			}
		}
		verifyRelationInstanceOnFieldNotBeyondStateMachine(klass
				.getSuperclass());
	}

	private void verifyRelationInstanceOnMethodNotBeyondStateMachine(
			Class<?> klass) throws VerificationException {
		if (Object.class == klass || null == klass) {
			return;
		}
		final VerificationFailureSet failureSet = new VerificationFailureSet();
		RelationIndicatorPropertyMethodScanner scanner = new RelationIndicatorPropertyMethodScanner(
				this, failureSet);
		MethodScanner.scanMethodsOnClasses(klass, scanner);
		if (failureSet.size() > 0) {
			throw new VerificationException(failureSet);
		}
	}

	private void verifyRelationInstancesDefinedCorrectly(Class<?> klass)
			throws VerificationException {
		verifyRelationInstanceNotBeyondStateMachine(klass);
		verifyRelationInstancesInClassLevelUnique(klass);
		verifyRelationInstancesInMethodLevelUnique(klass);
	}

	private void verifyRelationInstancesInClassLevelUnique(Class<?> klass)
			throws VerificationException {
		final Set<Class<?>> relations = new HashSet<Class<?>>();
		for (Field field : klass.getDeclaredFields()) {
			final Relation relation = field.getAnnotation(Relation.class);
			checkRelationInstanceWhetherExists(klass, relations, relation);
		}
		for (final Method method : klass.getDeclaredMethods()) {
			if (Relation.Utils.isRelationMethod(method)) {
				checkRelationInstanceWhetherExists(klass, relations,
						method.getAnnotation(Relation.class));
			}
		}
	}

	private void verifyRelationInstancesInMethodLevelUnique(final Class<?> klass)
			throws VerificationException {
		for (final Method method : klass.getDeclaredMethods()) {
			if (method.getParameterTypes().length <= 0) {
				continue;
			}
			final Set<Class<?>> methodRelations = new HashSet<Class<?>>();
			for (final Annotation[] annotations : method
					.getParameterAnnotations()) {
				for (final Annotation annotation : annotations) {
					if (annotation instanceof Relation) {
						checkRelationInstanceWhetherExists(klass,
								methodRelations, (Relation) annotation);
					}
				}
			}
		}
	}

	private void verifyRelations(Class<?> klass) throws VerificationException {
		verifyRelationInstancesDefinedCorrectly(klass);
		verifyRelationsAllBeCoveraged(klass);
	}

	private void verifyRelationsAllBeCoveraged(Class<?> klass)
			throws VerificationException {
		for (StateMetadata state : getMetaType().getAllStates()) {
			for (RelationConstraintMetadata relation : state.getValidWhiles()) {
				for (EventMetadata event : state.getPossibleLeavingEvents()) {
					verifyRelationBeCovered(klass, relation, event);
				}
			}
			for (RelationConstraintMetadata relation : state
					.getDeclaredInboundWhiles()) {
				for (EventMetadata event : getEventsToState(state)) {
					verifyRelationBeCovered(klass, relation, event);
				}
			}
		}
	}

	private void verifyStateIndicator(Class<?> klass)
			throws VerificationException {
		if (verifyStateIndicatorField(klass))
			return;
		if (verifyCustomizedStateIndicatorGetter(klass))
			return;
		if (verifyDefaultStateGetterMethod(klass))
			return;
		throw newVerificationException(
				getDottedPath(),
				SyntaxErrors.STATE_INDICATOR_CANNOT_FIND_DEFAULT_AND_SPECIFIED_STATE_INDICATOR,
				klass);
	}

	private void verifyStateIndicatorConverter(AnnotatedElement getter,
			Class<?> stateType) throws VerificationException {
		final Class<?> getterDeclaringClass;
		if (getter instanceof Method) {
			getterDeclaringClass = ((Method) getter).getDeclaringClass();
		} else if (getter instanceof Field) {
			getterDeclaringClass = ((Field) getter).getDeclaringClass();
		} else {
			throw new IllegalArgumentException();
		}
		final Converter converterMeta = getter.getAnnotation(Converter.class);
		if (null == converterMeta) {
			throw newVerificationException(getDottedPath(),
					SyntaxErrors.STATE_INDICATOR_CONVERTER_NOT_FOUND,
					getterDeclaringClass, stateType);
		}
		verifyStateIndicatorConverterDeclaredCorrectTypeParameter(stateType,
				getterDeclaringClass, converterMeta);
	}

	private void verifyStateIndicatorConverterDeclaredCorrectTypeParameter(
			Class<?> stateType, final Class<?> getterDeclaringClass,
			final Converter converterMeta) throws VerificationException {
		for (Type type : converterMeta.value().getGenericInterfaces()) {
			if (type instanceof ParameterizedType) {
				final ParameterizedType pType = (ParameterizedType) type;
				if (implementsStateConverter(pType)
						&& !matchedWithStateType(stateType, pType)) {
					throw newVerificationException(getDottedPath(),
							SyntaxErrors.STATE_INDICATOR_CONVERTER_INVALID,
							getterDeclaringClass, stateType,
							converterMeta.value(),
							pType.getActualTypeArguments()[0]);
				}
			}
		}
	}

	private boolean matchedWithStateType(Class<?> stateType,
			final ParameterizedType pType) {
		boolean matched = true;
		if (!stateType.isPrimitive()) {
			if (!stateType.equals(pType.getActualTypeArguments()[0])) {
				matched = false;
			}
		} else {
			if (!matchPrimitiveType(stateType, pType)) {
				matched = false;
			}
		}
		return matched;
	}

	private boolean implementsStateConverter(final ParameterizedType pType) {
		return pType.getRawType() instanceof Class
				&& StateConverter.class.isAssignableFrom((Class<?>) pType
						.getRawType());
	}

	private boolean matchPrimitiveType(Class<?> stateType,
			ParameterizedType pType) {
		boolean matched = true;
		if (Byte.TYPE.equals(stateType)) {
			if (!Byte.class.equals(pType.getActualTypeArguments()[0])) {
				matched = false;
			}
		} else if (Short.TYPE.equals(stateType)) {
			if (!Short.class.equals(pType.getActualTypeArguments()[0])) {
				matched = false;
			}
		} else if (Integer.TYPE.equals(stateType)) {
			if (!Integer.class.equals(pType.getActualTypeArguments()[0])) {
				matched = false;
			}
		} else if (Long.TYPE.equals(stateType)) {
			if (!Long.class.equals(pType.getActualTypeArguments()[0])) {
				matched = false;
			}
		} else if (Boolean.TYPE.equals(stateType)) {
			if (!Boolean.class.equals(pType.getActualTypeArguments()[0])) {
				matched = false;
			}
		} else if (Character.TYPE.equals(stateType)) {
			if (!Character.class.equals(pType.getActualTypeArguments()[0])) {
				matched = false;
			}
		} else if (Float.TYPE.equals(stateType)) {
			if (!Float.class.equals(pType.getActualTypeArguments()[0])) {
				matched = false;
			}
		} else if (Double.TYPE.equals(stateType)) {
			if (!Double.class.equals(pType.getActualTypeArguments()[0])) {
				matched = false;
			}
		} else {
			matched = false;
		}
		return matched;
	}

	private void verifyStateIndicatorElement(Class<?> klass,
			AnnotatedElement getter, Class<?> stateType)
			throws VerificationException {
		verifyStateIndicatorElementSetterVisibility(klass, getter, stateType);
		if (stateType.equals(java.lang.String.class)) {
			return;
		}
		verifyStateIndicatorConverter(getter, stateType);
	}

	private void verifyStateIndicatorElementSetterVisibility(
			final Class<?> klass, AnnotatedElement getter, Class<?> returnType)
			throws VerificationException {
		if (getter instanceof Method) {
			final String getterName = ((Method) getter).getName();
			final String setterName = convertSetterName(getterName, returnType);
			final Method setter = findMethod(klass, setterName, returnType);
			if (null == setter && !klass.isInterface()) {
				throw newVerificationException(getDottedPath(),
						SyntaxErrors.STATE_INDICATOR_SETTER_NOT_FOUND,
						((Method) getter).getDeclaringClass());
			} else {
				if (null != setter
						&& !Modifier.isPrivate((setter).getModifiers())) {
					throw newVerificationException(
							getDottedPath(),
							SyntaxErrors.STATE_INDICATOR_CANNOT_EXPOSE_STATE_INDICATOR_SETTER,
							setter);
				}
			}
		} else if (getter instanceof Field) {
			if (!Modifier.isPrivate(((Field) getter).getModifiers())) {
				throw newVerificationException(
						getDottedPath(),
						SyntaxErrors.STATE_INDICATOR_CANNOT_EXPOSE_STATE_INDICATOR_FIELD,
						getter);
			}
		} else {
			throw new IllegalArgumentException();
		}
	}

	private boolean verifyStateIndicatorField(Class<?> klass)
			throws VerificationException {
		Field field = findFieldWith(klass, StateIndicator.class);
		if (null != field) {
			verifyStateIndicatorElement(klass, field, field.getType());
			return true;
		}
		return false;
	}

	private void verifySyntax(Class<?> klass) throws VerificationException {
		verifyEventMethods(klass);
		verifyStateIndicator(klass);
		verifyRelations(klass);
		verifyConditions(klass);
		verifyCallbackMethods(klass);
	}

	private void verifyEventBeCovered(Class<?> klass,
			final EventMetadata eventMetadata,
			final VerificationFailureSet failureSet) {
		CoverageVerifier coverage = new CoverageVerifier(this, eventMetadata,
				failureSet);
		MethodScanner.scanMethodsOnClasses(klass, coverage);
		if (coverage.notCovered()) {
			failureSet.add(newVerificationFailure(eventMetadata.getDottedPath()
					.getAbsoluteName(),
					SyntaxErrors.LM_EVENT_NOT_CONCRETED_IN_LM, eventMetadata
							.getDottedPath().getName(), getMetaType()
							.getDottedPath().getAbsoluteName(), klass
							.getSimpleName()));
		}
	}

	private void verifyEventMethod(Method method,
			VerificationFailureSet failureSet) {
		final Event event = method.getAnnotation(Event.class);
		if (event == null) {
			return;
		}
		EventMetadata eventMetadata = null;
		if (Null.class == event.value()) {
			eventMetadata = verifyEventMethodDefaultStyle(method, failureSet,
					eventMetadata);
		} else {
			eventMetadata = verifyEventMethodWithEventClassKey(method,
					failureSet, event, eventMetadata);
		}
		if (null != eventMetadata) {
			eventMetadata.verifyEventMethod(method, failureSet);
		}
	}

	private EventMetadata verifyEventMethodDefaultStyle(Method method,
			VerificationFailureSet failureSet, EventMetadata eventMetadata) {
		if (!getMetaType().hasEvent(
				StringUtil.toUppercaseFirstCharacter(method.getName()))) {
			failureSet.add(newVerificationFailure(getMethodDottedPath(method),
					SyntaxErrors.LM_METHOD_NAME_INVALID, getMetaType()
							.getDottedPath(), method.getName(), method
							.getDeclaringClass().getName()));
		} else {
			eventMetadata = getMetaType().getEvent(
					StringUtil.toUppercaseFirstCharacter(method.getName()));
		}
		return eventMetadata;
	}

	private void verifyEventMethods(Class<?> klass)
			throws VerificationException {
		final VerificationFailureSet failureSet = new VerificationFailureSet();
		verifyEventMethodsValidity(klass, failureSet);
		verifyAllEventsCoverage(klass, failureSet);
		if (failureSet.size() > 0) {
			throw new VerificationException(failureSet);
		}
	}

	private void verifyEventMethodsValidity(final Class<?> klass,
			final VerificationFailureSet failureSet) {
		MethodScanner.scanMethodsOnClasses(klass, new MethodScanCallback() {

			@Override
			public boolean onMethodFound(Method method) {
				if (method.isBridge()) return false;
				verifyEventMethod(method, failureSet);
				return false;
			}
		});
	}

	private EventMetadata verifyEventMethodWithEventClassKey(Method method,
			VerificationFailureSet failureSet, final Event event,
			EventMetadata eventMetadata) {
		if (!getMetaType().hasEvent(event.value())) {
			failureSet.add(newVerificationFailure(getMethodDottedPath(method),
					SyntaxErrors.LM_EVENT_METHOD_WITH_INVALID_EVENT_REFERENCE,
					event, method.getName(), method.getDeclaringClass()
							.getName(), getMetaType().getDottedPath()));
		} else {
			eventMetadata = getMetaType().getEvent(event.value());
		}
		return eventMetadata;
	}

	@Override
	public Entry<StateAccessible<String>, String> getInitialState() {
		Iterator<Entry<StateAccessible<String>, String>> iterator = this.initialStates.entrySet().iterator();
		while (iterator.hasNext()) {
			Entry<StateAccessible<String>, String> next = iterator.next();
			return next;
		}
		throw new IllegalStateException("We should not be here! Check code");
	}

	
	@Override
	public void addCommonOnEventCallbackObject(EventCallbackObject item) {
        this.commonEventCallbackObjects.add(item);
	}

	@Override
	public EventObject getEvent(Object eventKey) {
		return eventObjectList.get(eventKey);
	}
}
