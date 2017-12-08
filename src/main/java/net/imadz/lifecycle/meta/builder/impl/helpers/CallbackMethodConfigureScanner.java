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
package net.imadz.lifecycle.meta.builder.impl.helpers;

import net.imadz.lifecycle.SyntaxErrors;
import net.imadz.lifecycle.annotations.LifecycleMeta;
import net.imadz.lifecycle.annotations.callback.AnyEvent;
import net.imadz.lifecycle.annotations.callback.AnyState;
import net.imadz.lifecycle.annotations.callback.CallbackConsts;
import net.imadz.lifecycle.annotations.callback.Callbacks;
import net.imadz.lifecycle.annotations.callback.OnEvent;
import net.imadz.lifecycle.annotations.callback.PostStateChange;
import net.imadz.lifecycle.annotations.callback.PreStateChange;
import net.imadz.lifecycle.annotations.relation.Relation;
import net.imadz.lifecycle.meta.builder.impl.CallbackObject;
import net.imadz.lifecycle.meta.builder.impl.EventCallbackObject;
import net.imadz.lifecycle.meta.builder.impl.RelationalCallbackObject;
import net.imadz.lifecycle.meta.builder.impl.RelationalEventCallbackObject;
import net.imadz.lifecycle.meta.builder.impl.StateMachineObjectBuilderImpl;
import net.imadz.lifecycle.meta.object.StateMachineObject;
import net.imadz.lifecycle.meta.type.EventMetadata;
import net.imadz.lifecycle.meta.type.StateMachineMetadata;
import net.imadz.util.FieldEvaluator;
import net.imadz.util.MethodScanCallback;
import net.imadz.util.MethodScanner;
import net.imadz.util.PropertyEvaluator;
import net.imadz.util.Readable;
import net.imadz.util.StringUtil;
import net.imadz.utils.Null;
import net.imadz.verification.VerificationException;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashSet;

public final class CallbackMethodConfigureScanner {

  private final StateMachineObjectBuilderImpl<?> stateMachineObjectBuilderImpl;
  private final Class<?> klass;
  private final HashSet<String> lifecycleOverridenCallbackDefinitionSet = new HashSet<String>();

  public CallbackMethodConfigureScanner(
      StateMachineObjectBuilderImpl<?> stateMachineObjectBuilderImpl,
      Class<?> klass) {
    this.stateMachineObjectBuilderImpl = stateMachineObjectBuilderImpl;
    this.klass = klass;
  }

  public void scanMethod() throws VerificationException {
    if (klass.isInterface()
        && null == klass.getAnnotation(LifecycleMeta.class)) {
      return;
    }
    if (klass.isInterface()) {
      MethodScanner.scanMethodsOnClasses(klass, new MethodScanCallback() {

        @Override
        public boolean onMethodFound(Method method) {
          if (method.isBridge()) {
            return false;
          }
          try {
            //FIXME: Set inheritance level for methods in interfaces
            return CallbackMethodConfigureScanner.this.onMethodFound(method, 0);
          } catch (VerificationException e) {
            return false;
          }
        }

      });
    } else {
      int inheritanceLevel = 0; // set current level default to 0, minus 1 to
      // super class
      for (Class<?> clazz = klass; null != clazz && clazz != null && clazz != Object.class; clazz = clazz
          .getSuperclass()) {
        inheritanceLevel -= 1;
        for (Method method : clazz.getDeclaredMethods()) {
          onMethodFound(method, inheritanceLevel);
        }
      }
    }
  }

  public boolean onMethodFound(Method method, int inheritanceLevel)
      throws VerificationException {
    if (!isCallbackMethod(method)) {
      return false;
    }
    if (lifecycleOverridenCallbackDefinitionSet.contains(method.getName())) {
      return false;
    }
    final MethodWrapper mw = new MethodWrapper(method, inheritanceLevel);

    configureCallbacks(mw, method.getAnnotation(Callbacks.class));
    configurePreStateChange(mw, method.getAnnotation(PreStateChange.class));
    configurePostStateChange(mw,
        method.getAnnotation(PostStateChange.class));
    configureOnEvent(method,
        method.getAnnotation(OnEvent.class));
    if (stateMachineObjectBuilderImpl
        .hasLifecycleOverrideAnnotation(method)) {
      lifecycleOverridenCallbackDefinitionSet.add(method.getName());
    }
    return false;
  }

  private void configureCallbacks(final MethodWrapper methodWrapper,
      final Callbacks callbacks) throws VerificationException {
    if (null == callbacks) {
      return;
    }
    for (final PreStateChange item : callbacks.preStateChange()) {
      configurePreStateChange(methodWrapper, item);
    }
    for (final PostStateChange item : callbacks.postStateChange()) {
      configurePostStateChange(methodWrapper, item);
    }
    for (final OnEvent item : callbacks.onEvent()) {
      configureOnEvent(methodWrapper.getMethod(), item);
    }
  }

  private void configurePreStateChange(final MethodWrapper methodWrapper,
      final PreStateChange preStateChange) throws VerificationException {
    if (null == preStateChange) {
      return;
    }
    final Class<?> from = preStateChange.from();
    final Class<?> to = preStateChange.to();
    final String observableName = preStateChange.observableName();
    final String mappedBy = preStateChange.mappedBy();
    final Class<?> observableClass = preStateChange.observableClass();
    final Method method = methodWrapper.getMethod();
    if (isRelationalCallback(observableName, observableClass)) {
      final Class<?> actualObservableClass = evaluateActualObservableClassOfPreStateChange(
          method, observableName, observableClass);
      final StateMachineObject<?> callBackEventSourceContainer = this.stateMachineObjectBuilderImpl
          .getRegistry()
          .loadStateMachineObject(actualObservableClass);
      if (AnyState.class != to) {
        verifyPreToState(method, to,
            callBackEventSourceContainer.getMetaType());
      }
      if (AnyState.class != from) {
        verifyFromState(method, from,
            callBackEventSourceContainer.getMetaType(),
            SyntaxErrors.PRE_STATE_CHANGE_FROM_STATE_IS_INVALID);
      }
      validateMappedby(method, mappedBy,
          actualObservableClass,
          SyntaxErrors.PRE_STATE_CHANGE_MAPPEDBY_INVALID);
      final Readable<?> accessor = evaluateAccessor(mappedBy,
          actualObservableClass);
      configurePreStateChangeRelationalCallbackObjects(methodWrapper,
          from, to, callBackEventSourceContainer, accessor);
    } else {
      configurePreStateChangeNonRelationalCallbackObjects(methodWrapper,
          from, to);
    }
  }

  private boolean isMappedByInvalid(final String mappedBy,
      final Class<?> actualObservableClass) {
    return null == mappedBy
        || CallbackConsts.NULL_STR.equalsIgnoreCase(mappedBy)
        || null == convertRelationKey(actualObservableClass, mappedBy);
  }

  private Class<?> evaluateActualObservableClassOfPreStateChange(
      final Method method, final String observableName,
      final Class<?> observableClass) throws VerificationException {
    Class<?> actualObservableClass = null;
    if (!CallbackConsts.NULL_STR.equals(observableName)
        && Null.class != observableClass) {
      verifyObservableClass(method, observableClass,
          SyntaxErrors.PRE_STATE_CHANGE_OBSERVABLE_CLASS_INVALID);
      final Class<?> observableClassViaObservaleName = verifyObservableName(
          method, observableName,
          SyntaxErrors.PRE_STATE_CHANGE_RELATION_INVALID);
      if (!observableClass
          .isAssignableFrom(observableClassViaObservaleName)) {
        throw this.stateMachineObjectBuilderImpl
            .newVerificationException(
                this.stateMachineObjectBuilderImpl
                    .getDottedPath(),
                SyntaxErrors.PRE_STATE_CHANGE_OBSERVABLE_NAME_MISMATCH_OBSERVABLE_CLASS,
                observableName, observableClass, method);
      }
      actualObservableClass = observableClass;
    } else if (CallbackConsts.NULL_STR.equals(observableName)
        && Null.class != observableClass) {
      verifyObservableClass(method, observableClass,
          SyntaxErrors.PRE_STATE_CHANGE_OBSERVABLE_CLASS_INVALID);
      actualObservableClass = observableClass;
    } else if (!CallbackConsts.NULL_STR.equals(observableName)
        && Null.class == observableClass) {
      actualObservableClass = verifyObservableName(method,
          observableName,
          SyntaxErrors.PRE_STATE_CHANGE_RELATION_INVALID);
    }
    return actualObservableClass;
  }

  private boolean isRelationalCallback(final String observableName,
      final Class<?> observableClass) {
    return !CallbackConsts.NULL_STR.equals(observableName)
        || Null.class != observableClass;
  }

  private void configurePostStateChange(final MethodWrapper methodWrapper,
      final PostStateChange postStateChange) throws VerificationException {
    if (null == postStateChange) {
      return;
    }
    final Class<?> from = postStateChange.from();
    final Class<?> to = postStateChange.to();
    final String observableName = postStateChange.observableName();
    final String mappedBy = postStateChange.mappedBy();
    final Class<?> observableClass = postStateChange.observableClass();
    final int priority = postStateChange.priority();
    methodWrapper.setPriority(priority);
    if (isRelationalCallback(observableName, observableClass)) {
      final Class<?> actualObservableClass = evaluateActualObservableClassOfPostStateChange(
          methodWrapper.getMethod(), observableName, observableClass);
      validateMappedby(methodWrapper.getMethod(), mappedBy,
          actualObservableClass,
          SyntaxErrors.POST_STATE_CHANGE_MAPPEDBY_INVALID);
      final StateMachineObject<?> callBackEventSourceContainer = this.stateMachineObjectBuilderImpl
          .getRegistry()
          .loadStateMachineObject(actualObservableClass);
      if (AnyState.class != from) {
        verifyFromState(methodWrapper.getMethod(), from,
            callBackEventSourceContainer.getMetaType(),
            SyntaxErrors.POST_STATE_CHANGE_FROM_STATE_IS_INVALID);
      }
      if (AnyState.class != to) {
        verifyPostToState(methodWrapper.getMethod(), to,
            callBackEventSourceContainer.getMetaType());
      }
      final Readable<?> accessor = evaluateAccessor(mappedBy,
          actualObservableClass);
      configurePostStateChangeCallbackObjectsWithRelational(
          methodWrapper, from, to, callBackEventSourceContainer,
          accessor);
    } else {
      configurePostStateChangeNonRelationalCallbackObjects(methodWrapper,
          from, to);
    }
  }


  private void configureOnEvent(final Method method, final OnEvent onEvent) throws VerificationException {
    if (null == onEvent) {
      return;
    }
    final Class<?> eventClass = onEvent.value();
    final String observableName = onEvent.observableName();
    final String mappedBy = onEvent.mappedBy();
    final Class<?> observableClass = onEvent.observableClass();
    if (isRelationalCallback(observableName, observableClass)) {
      final Class<?> actualObservableClass = evaluateActualObservableClassOfOnEvent(
          method, observableName, observableClass);
      validateMappedby(method, mappedBy, actualObservableClass,
          SyntaxErrors.ON_EVENT_MAPPEDBY_INVALID);
      final StateMachineObject<?> callBackEventSourceContainer = this.stateMachineObjectBuilderImpl
          .getRegistry()
          .loadStateMachineObject(actualObservableClass);
      if (AnyEvent.class != eventClass) {
        verifyEvent(method, eventClass,
            callBackEventSourceContainer.getMetaType(),
            SyntaxErrors.ON_EVENT_EVENT_IS_INVALID);
      }
      final Readable<?> accessor = evaluateAccessor(mappedBy,
          actualObservableClass);
      configureOnEventRelationalCallbackObjects(method, eventClass, callBackEventSourceContainer, accessor);
    } else {
      configureOnEventNonRelationalCallbackObjects(method, eventClass);
    }
  }


  private void configureOnEventNonRelationalCallbackObjects(Method method,
      Class<?> eventClass) {
    final EventCallbackObject item = new EventCallbackObject(eventClass, method);
    if (AnyEvent.class != eventClass) {
      this.stateMachineObjectBuilderImpl.getEvent(eventClass).addSpecificOnEventCallbackObject(item);
    } else {
      this.stateMachineObjectBuilderImpl.addCommonOnEventCallbackObject(item);
    }
  }

  private void configureOnEventRelationalCallbackObjects(Method method,
      Class<?> eventClass,
      StateMachineObject<?> callBackEventSourceContainer,
      Readable<?> accessor) {
    final RelationalEventCallbackObject item = new RelationalEventCallbackObject(eventClass, accessor, method);
    if (AnyEvent.class != eventClass) {
      callBackEventSourceContainer.getEvent(eventClass).addSpecificOnEventCallbackObject(item);
    } else {
      callBackEventSourceContainer.addCommonOnEventCallbackObject(item);
    }
  }

  private void verifyEvent(Method method, Class<?> eventClass,
      StateMachineMetadata metaType, String onEventEventIsInvalid) throws VerificationException {
    if (null == metaType.getEvent(eventClass)) {
      throw this.stateMachineObjectBuilderImpl.newVerificationException(
          metaType.getDottedPath(),
          SyntaxErrors.ON_EVENT_EVENT_IS_INVALID, eventClass,
          method, metaType.getPrimaryKey());
    }
  }

  private Class<?> evaluateActualObservableClassOfOnEvent(Method method,
      String observableName, Class<?> observableClass) throws VerificationException {
    Class<?> actualObservableClass = null;
    if (!CallbackConsts.NULL_STR.equals(observableName)
        && Null.class != observableClass) {
      verifyObservableClass(method, observableClass,
          SyntaxErrors.ON_EVENT_OBSERVABLE_CLASS_INVALID);
      final Class<?> observableClassViaObservaleName = verifyObservableName(
          method, observableName,
          SyntaxErrors.ON_EVENT_RELATION_INVALID);
      if (!observableClass
          .isAssignableFrom(observableClassViaObservaleName)) {
        throw this.stateMachineObjectBuilderImpl
            .newVerificationException(
                this.stateMachineObjectBuilderImpl
                    .getDottedPath(),
                SyntaxErrors.ON_EVENT_OBSERVABLE_NAME_MISMATCH_OBSERVABLE_CLASS,
                observableName, observableClass, method);
      }
      actualObservableClass = observableClass;
    } else if (CallbackConsts.NULL_STR.equals(observableName)
        && Null.class != observableClass) {
      verifyObservableClass(method, observableClass,
          SyntaxErrors.ON_EVENT_OBSERVABLE_CLASS_INVALID);
      actualObservableClass = observableClass;
    } else if (!CallbackConsts.NULL_STR.equals(observableName)
        && Null.class == observableClass) {
      actualObservableClass = verifyObservableName(method,
          observableName,
          SyntaxErrors.ON_EVENT_RELATION_INVALID);
    }
    return actualObservableClass;
  }

  private void validateMappedby(final Method method, final String mappedBy,
      final Class<?> actualObservableClass, final String errorCode)
      throws VerificationException {
    final StateMachineObject<?> callBackEventSourceContainer = this.stateMachineObjectBuilderImpl
        .getRegistry().loadStateMachineObject(actualObservableClass);
    if (isMappedByInvalid(mappedBy, actualObservableClass)) {
      throw this.stateMachineObjectBuilderImpl.newVerificationException(
          callBackEventSourceContainer.getDottedPath(), errorCode,
          mappedBy, method, actualObservableClass);
    }
  }

  private Class<?> evaluateActualObservableClassOfPostStateChange(
      Method method, final String observableName,
      final Class<?> observableClass) throws VerificationException {
    Class<?> actualObservableClass = null;
    if (!CallbackConsts.NULL_STR.equals(observableName)
        && Null.class != observableClass) {
      verifyObservableClass(method, observableClass,
          SyntaxErrors.POST_STATE_CHANGE_OBSERVABLE_CLASS_INVALID);
      final Class<?> observableClassViaObservaleName = verifyObservableName(
          method, observableName,
          SyntaxErrors.POST_STATE_CHANGE_RELATION_INVALID);
      if (!observableClass
          .isAssignableFrom(observableClassViaObservaleName)) {
        throw this.stateMachineObjectBuilderImpl
            .newVerificationException(
                this.stateMachineObjectBuilderImpl
                    .getDottedPath(),
                SyntaxErrors.POST_STATE_CHANGE_OBSERVABLE_NAME_MISMATCH_OBSERVABLE_CLASS,
                observableName, observableClass, method);
      }
      actualObservableClass = observableClass;
    } else if (CallbackConsts.NULL_STR.equals(observableName)
        && Null.class != observableClass) {
      verifyObservableClass(method, observableClass,
          SyntaxErrors.POST_STATE_CHANGE_OBSERVABLE_CLASS_INVALID);
      actualObservableClass = observableClass;
    } else if (!CallbackConsts.NULL_STR.equals(observableName)
        && Null.class == observableClass) {
      actualObservableClass = verifyObservableName(method,
          observableName,
          SyntaxErrors.POST_STATE_CHANGE_RELATION_INVALID);
    }
    return actualObservableClass;
  }

  private Class<?> verifyObservableName(Method method,
      final String observableName, String errorCode)
      throws VerificationException {
    Class<?> actualObservableClass;
    actualObservableClass = evaluateObservableClass(klass, observableName);
    if (null == actualObservableClass) {
      throw this.stateMachineObjectBuilderImpl.newVerificationException(
          this.stateMachineObjectBuilderImpl.getDottedPath(),
          errorCode, observableName, method, klass);
    }
    return actualObservableClass;
  }

  private void verifyObservableClass(Method method,
      final Class<?> observableClass, String errorCode)
      throws VerificationException {
    try {
      this.stateMachineObjectBuilderImpl.getRegistry()
          .loadStateMachineObject(observableClass);
    } catch (VerificationException e) {
      if (e.getVerificationFailureSet().iterator().next().getErrorCode()
          .equals(SyntaxErrors.REGISTERED_META_ERROR)) {
        throw this.stateMachineObjectBuilderImpl
            .newVerificationException(
                this.stateMachineObjectBuilderImpl
                    .getDottedPath(), errorCode,
                observableClass, method);
      }
    }
  }

  private void verifyPostToState(final Method method, Class<?> to,
      final StateMachineMetadata metaType) throws VerificationException {
    if (null == metaType.getState(to)) {
      throw this.stateMachineObjectBuilderImpl.newVerificationException(
          metaType.getDottedPath(),
          SyntaxErrors.POST_STATE_CHANGE_TO_STATE_IS_INVALID, to,
          method, metaType.getPrimaryKey());
    }
  }

  private String convertRelationKey(final Class<?> klass,
      final String mappedBy) {
    final Field observerField = evaluateObserverField(klass, mappedBy);
    if (isRightRelationField(observerField)) {
      return evaluateRelationKeyFromRelationField(mappedBy, observerField);
    }
    final Method observerMethod = evaluateObserverMethod(klass, mappedBy);
    if (null == observerMethod) {
      return null;
    }
    final Relation relation = observerMethod.getAnnotation(Relation.class);
    if (null == relation) {
      return null;
    }
    if (Null.class != relation.value()) {
      return relation.value().getName();
    } else {
      return StringUtil.toUppercaseFirstCharacter(mappedBy);
    }
  }

  private Method evaluateObserverMethod(Class<?> klass, String mappedBy) {
    Method observerMethod = null;
    for (Class<?> clazz = klass; null != clazz && clazz != Object.class; clazz = clazz
        .getSuperclass()) {
      try {
        observerMethod = clazz.getDeclaredMethod("get"
            + StringUtil.toUppercaseFirstCharacter(mappedBy));
        break;
      } catch (NoSuchMethodException e) {
        continue;
      } catch (SecurityException e) {
        continue;
      }
    }
    return observerMethod;
  }

  private String evaluateRelationKeyFromRelationField(String mappedBy,
      Field observerField) {
    final Relation relation = observerField.getAnnotation(Relation.class);
    if (Null.class != relation.value()) {
      return relation.value().getName();
    } else {
      return StringUtil.toUppercaseFirstCharacter(mappedBy);
    }
  }

  private boolean isRightRelationField(Field observerField) {
    return null != observerField
        && null != observerField.getAnnotation(Relation.class);
  }

  private Field evaluateObserverField(Class<?> klass, String mappedBy) {
    Field observerField = null;
    for (Class<?> clazz = klass; clazz != Object.class && null != clazz; clazz = clazz
        .getSuperclass()) {
      try {
        observerField = clazz.getDeclaredField(mappedBy);
        break;
      } catch (NoSuchFieldException e) {
        continue;
      } catch (SecurityException e) {
        continue;
      }
    }
    return observerField;
  }

  private void verifyPreToState(Method method, Class<?> to,
      StateMachineMetadata stateMachineMetadata)
      throws VerificationException {
    if (null == stateMachineMetadata.getState(to)) {
      throw this.stateMachineObjectBuilderImpl.newVerificationException(
          stateMachineMetadata.getDottedPath(),
          SyntaxErrors.PRE_STATE_CHANGE_TO_STATE_IS_INVALID, to,
          method, stateMachineMetadata.getPrimaryKey());
    }
    verifyPreToStatePostEvaluate(method, to, stateMachineMetadata);
  }

  private void verifyPreToStatePostEvaluate(Method method,
      Class<?> toStateClass, StateMachineMetadata stateMachineMetadata)
      throws VerificationException {
    for (final EventMetadata event : stateMachineMetadata.getState(
        toStateClass).getPossibleReachingEvents()) {
      if (event.isConditional() && event.postValidate()) {
        throw this.stateMachineObjectBuilderImpl
            .newVerificationException(
                stateMachineMetadata.getDottedPath(),
                SyntaxErrors.PRE_STATE_CHANGE_TO_POST_EVALUATE_STATE_IS_INVALID,
                toStateClass, method, event.getDottedPath());
      }
    }
  }

  private void verifyFromState(Method method, Class<?> stateClass,
      StateMachineMetadata metaType, String errorCode)
      throws VerificationException {
    if (null == metaType.getState(stateClass)) {
      throw this.stateMachineObjectBuilderImpl.newVerificationException(
          metaType.getDottedPath(), errorCode, stateClass, method,
          metaType.getPrimaryKey());
    }
  }

  private Readable<?> evaluateAccessor(String mappedBy,
      Class<?> observableClass) {
    final Field observerField = findObserverField(mappedBy, observableClass);
    if (null != observerField) {
      return new FieldEvaluator(observerField);
    }
    final Method observerMethod = findObserverProperty(mappedBy,
        observableClass);
    if (null != observerMethod) {
      return new PropertyEvaluator(observerMethod);
    }
    return null;
  }

  private Method findObserverProperty(String mappedBy,
      Class<?> observableClass) {
    Method getter = null;
    for (Class<?> klass = observableClass; null != klass && klass != Object.class; klass = klass
        .getSuperclass()) {
      try {
        getter = klass.getDeclaredMethod("get"
            + StringUtil.toUppercaseFirstCharacter(mappedBy));
        break;
      } catch (NoSuchMethodException e) {
        continue;
      } catch (SecurityException e) {
        continue;
      }
    }
    return getter;
  }

  private Field findObserverField(String mappedBy, Class<?> observableClass) {
    Field observerField = null;
    for (Class<?> klass = observableClass; null != klass && klass != Object.class; klass = klass
        .getSuperclass()) {
      try {
        observerField = klass.getDeclaredField(mappedBy);
        break;
      } catch (NoSuchFieldException e) {
        continue;
      } catch (SecurityException e) {
        continue;
      }
    }
    return observerField;
  }

  private void configurePreStateChangeRelationalCallbackObjects(
      final MethodWrapper method, final Class<?> from, final Class<?> to,
      final StateMachineObject<?> callBackEventSourceContainer,
      final Readable<?> accessor) {
    RelationalCallbackObject item = null;
    if (focusFromAndToState(from, to)) {
      method.setGeneralize(CallbackObject.SPECIFIC);
      item = new RelationalCallbackObject(from.getSimpleName(),
          to.getSimpleName(), method, accessor);
      callBackEventSourceContainer
          .addSpecificPreStateChangeCallbackObject(item);
    } else if (focusToState(from, to)) {
      method.setGeneralize(CallbackObject.TO);
      item = new RelationalCallbackObject(AnyState.class.getSimpleName(),
          to.getSimpleName(), method, accessor);
      callBackEventSourceContainer.getState(to).addPreToCallbackObject(
          to, item);
    } else if (focusFromState(from, to)) {
      method.setGeneralize(CallbackObject.TO);
      item = new RelationalCallbackObject(from.getSimpleName(),
          AnyState.class.getSimpleName(), method, accessor);
      callBackEventSourceContainer.getState(from)
          .addPreFromCallbackObject(from, item);
    } else {
      method.setGeneralize(CallbackObject.COMMON);
      item = new RelationalCallbackObject(AnyState.class.getSimpleName(),
          AnyState.class.getSimpleName(), method, accessor);
      callBackEventSourceContainer
          .addCommonPreStateChangeCallbackObject(item);
    }
  }

  private boolean focusFromAndToState(final Class<?> from, final Class<?> to) {
    return AnyState.class != from && AnyState.class != to;
  }

  private boolean focusFromState(final Class<?> from, final Class<?> to) {
    return AnyState.class != from && AnyState.class == to;
  }

  private boolean focusToState(final Class<?> from, final Class<?> to) {
    return AnyState.class == from && AnyState.class != to;
  }

  private void configurePostStateChangeCallbackObjectsWithRelational(
      MethodWrapper method, Class<?> from, Class<?> to,
      final StateMachineObject<?> callBackEventSourceContainer,
      final Readable<?> accessor) {
    RelationalCallbackObject item = null;
    if (focusFromAndToState(from, to)) {
      method.setGeneralize(CallbackObject.SPECIFIC);
      item = new RelationalCallbackObject(from.getSimpleName(),
          to.getSimpleName(), method, accessor);
      callBackEventSourceContainer
          .addSpecificPostStateChangeCallbackObject(item);
    } else if (focusToState(from, to)) {
      method.setGeneralize(CallbackObject.TO);
      item = new RelationalCallbackObject(AnyState.class.getSimpleName(),
          to.getSimpleName(), method, accessor);
      callBackEventSourceContainer.getState(to).addPostToCallbackObject(
          to, item);
    } else if (focusFromState(from, to)) {
      method.setGeneralize(CallbackObject.FROM);
      item = new RelationalCallbackObject(from.getSimpleName(),
          AnyState.class.getSimpleName(), method, accessor);
      callBackEventSourceContainer.getState(from)
          .addPostFromCallbackObject(from, item);
    } else {
      method.setGeneralize(CallbackObject.COMMON);
      item = new RelationalCallbackObject(AnyState.class.getSimpleName(),
          AnyState.class.getSimpleName(), method, accessor);
      callBackEventSourceContainer
          .addCommonPostStateChangeCallbackObject(item);
    }
  }

  private Class<?> evaluateObservableClass(Class<?> klass,
      String observableName) {
    final Field observableField = findObservableField(klass, observableName);
    if (null != observableField) {
      final Type genericType = observableField.getGenericType();
      if (genericType instanceof ParameterizedType) {
        ParameterizedType pType = (ParameterizedType) genericType;
        return (Class<?>) pType.getActualTypeArguments()[0];
      }
      return observableField.getType();
    }
    return evaluateObservableClassFromProperties(klass, observableName);
  }

  private Class<?> evaluateObservableClassFromProperties(Class<?> klass,
      String observableName) {
    final Method observableMethod = findObservableMethod(klass,
        observableName);
    if (null == observableMethod) {
      return null;
    }
    final Class<?> relatedClass = observableMethod.getReturnType();
    if (Iterable.class.isAssignableFrom(relatedClass)) {
      return null;
    }
    if (relatedClass.isArray()) {
      return relatedClass.getComponentType();
    }
    return relatedClass;
  }

  private Method findObservableMethod(Class<?> klass, String observableName) {
    Method relatedMethod = null;
    for (Class<?> clazz = klass; null != clazz && clazz != Object.class; clazz = clazz
        .getSuperclass()) {
      try {
        relatedMethod = clazz.getDeclaredMethod("get"
            + StringUtil.toUppercaseFirstCharacter(observableName));
        break;
      } catch (NoSuchMethodException e) {
        continue;
      } catch (SecurityException e) {
        continue;
      }
    }
    return relatedMethod;
  }

  private Field findObservableField(Class<?> klass, String observableName) {
    Field declaredField = null;
    for (Class<?> clazz = klass; null != clazz && clazz != Object.class; clazz = clazz
        .getSuperclass()) {
      try {
        declaredField = clazz.getDeclaredField(observableName);
        break;
      } catch (NoSuchFieldException e) {
        continue;
      } catch (SecurityException e) {
        continue;
      }
    }
    return declaredField;
  }

  private void configurePreStateChangeNonRelationalCallbackObjects(
      final MethodWrapper methodWrapper, final Class<?> from, final Class<?> to) {
    CallbackObject item = null;
    if (focusFromAndToState(from, to)) {
      methodWrapper.setGeneralize(CallbackObject.SPECIFIC);
      item = new CallbackObject(from.getSimpleName(), to.getSimpleName(),
          methodWrapper);
      this.stateMachineObjectBuilderImpl
          .addSpecificPreStateChangeCallbackObject(item);
    } else if (focusToState(from, to)) {
      methodWrapper.setGeneralize(CallbackObject.TO);
      item = new CallbackObject(AnyState.class.getSimpleName(),
          to.getSimpleName(), methodWrapper);
      this.stateMachineObjectBuilderImpl.getState(to)
          .addPreToCallbackObject(to, item);
    } else if (focusFromState(from, to)) {
      methodWrapper.setGeneralize(CallbackObject.FROM);
      item = new CallbackObject(from.getSimpleName(),
          AnyState.class.getSimpleName(), methodWrapper);
      this.stateMachineObjectBuilderImpl.getState(from)
          .addPreFromCallbackObject(from, item);
    } else {
      methodWrapper.setGeneralize(CallbackObject.COMMON);
      item = new CallbackObject(AnyState.class.getSimpleName(),
          AnyState.class.getSimpleName(), methodWrapper);
      this.stateMachineObjectBuilderImpl
          .addCommonPreStateChangeCallbackObject(item);
    }
  }

  private void configurePostStateChangeNonRelationalCallbackObjects(
      final MethodWrapper methodWrapper, Class<?> from, Class<?> to) {
    CallbackObject item = null;
    if (focusFromAndToState(from, to)) {
      methodWrapper.setGeneralize(CallbackObject.SPECIFIC);
      item = new CallbackObject(from.getSimpleName(), to.getSimpleName(),
          methodWrapper);
      this.stateMachineObjectBuilderImpl
          .addSpecificPostStateChangeCallbackObject(item);
    } else if (focusToState(from, to)) {
      methodWrapper.setGeneralize(CallbackObject.TO);
      item = new CallbackObject(AnyState.class.getSimpleName(),
          to.getSimpleName(), methodWrapper);
      this.stateMachineObjectBuilderImpl.getState(to)
          .addPostToCallbackObject(to, item);
    } else if (focusFromState(from, to)) {
      methodWrapper.setGeneralize(CallbackObject.FROM);
      item = new CallbackObject(from.getSimpleName(),
          AnyState.class.getSimpleName(), methodWrapper);
      this.stateMachineObjectBuilderImpl.getState(from)
          .addPostFromCallbackObject(from, item);
    } else {
      methodWrapper.setGeneralize(CallbackObject.COMMON);
      item = new CallbackObject(AnyState.class.getSimpleName(),
          AnyState.class.getSimpleName(), methodWrapper);
      this.stateMachineObjectBuilderImpl
          .addCommonPostStateChangeCallbackObject(item);
    }
  }

  boolean hasAnnotation(AnnotatedElement element,
      Class<? extends Annotation> annotationClass) {
    return null != element.getAnnotation(annotationClass);
  }

  private boolean isCallbackMethod(Method method) {
    if (hasAnnotation(method, Callbacks.class)
        || hasAnnotation(method, PreStateChange.class)
        || hasAnnotation(method, PostStateChange.class)
        || hasAnnotation(method, OnEvent.class)) {
      return true;
    }
    return false;
  }
}