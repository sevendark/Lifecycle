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
import net.imadz.lifecycle.annotations.callback.AnyEvent;
import net.imadz.lifecycle.annotations.callback.AnyState;
import net.imadz.lifecycle.annotations.callback.CallbackConsts;
import net.imadz.lifecycle.annotations.callback.Callbacks;
import net.imadz.lifecycle.annotations.callback.OnEvent;
import net.imadz.lifecycle.annotations.callback.PostStateChange;
import net.imadz.lifecycle.annotations.callback.PreStateChange;
import net.imadz.lifecycle.meta.builder.impl.StateMachineObjectBuilderImpl;
import net.imadz.lifecycle.meta.type.EventMetadata;
import net.imadz.lifecycle.meta.type.StateMachineMetadata;
import net.imadz.lifecycle.meta.type.StateMetadata;
import net.imadz.util.MethodScanCallback;
import net.imadz.utils.Null;
import net.imadz.verification.VerificationFailureSet;

import java.lang.reflect.Method;

public final class CallbackMethodVerificationScanner implements MethodScanCallback {

  private final StateMachineObjectBuilderImpl<?> stateMachineObjectBuilderImpl;
  private VerificationFailureSet failureSet;

  public CallbackMethodVerificationScanner(StateMachineObjectBuilderImpl<?> stateMachineObjectBuilderImpl, final VerificationFailureSet failureSet) {
    this.stateMachineObjectBuilderImpl = stateMachineObjectBuilderImpl;
    this.failureSet = failureSet;
  }

  @Override
  public boolean onMethodFound(Method method) {
    if (method.isBridge()) {
      return false;
    }
    verifyPreStateChange(method, failureSet, method.getAnnotation(PreStateChange.class));
    verifyPostStateChange(method, failureSet, method.getAnnotation(PostStateChange.class));
    verifyOnEvent(method, failureSet, method.getAnnotation(OnEvent.class));
    verifyCallbacks(method);
    return false;
  }

  private void verifyCallbacks(Method method) {
    final Callbacks callbacks = method.getAnnotation(Callbacks.class);
    if (null != callbacks) {
      for (PreStateChange item : callbacks.preStateChange()) {
        verifyPreStateChange(method, failureSet, item);
      }
      for (PostStateChange item : callbacks.postStateChange()) {
        verifyPostStateChange(method, failureSet, item);
      }
      for (OnEvent item : callbacks.onEvent()) {
        verifyOnEvent(method, failureSet, item);
      }
    }
  }

  private void verifyOnEvent(final Method method,
      final VerificationFailureSet failureSet, final OnEvent onEvent) {
    if (null == onEvent) {
      return;
    }
    if (isRelationalCallback(onEvent.observableName(), onEvent.observableClass())) {
      return;
    }
    verifyEventWithoutRelation(method, failureSet, onEvent.value(), SyntaxErrors.ON_EVENT_EVENT_IS_INVALID);
  }

  private void verifyEventWithoutRelation(Method method,
      VerificationFailureSet failureSet, Class<?> eventClass,
      String errorCode) {
    if (AnyEvent.class != eventClass) {
      if (eventMetadataNotFound(eventClass)) {
        failureSet.add(this.stateMachineObjectBuilderImpl.newVerificationException(method.getDeclaringClass().getName() + "." + eventClass + "."
            + errorCode, errorCode, eventClass, method, this.stateMachineObjectBuilderImpl.getMetaType().getPrimaryKey()));
      }
    }
  }

  private boolean eventMetadataNotFound(Class<?> eventClass) {
    return null == this.stateMachineObjectBuilderImpl.getMetaType().getEvent(eventClass);
  }

  private void verifyPostStateChange(final Method method, final VerificationFailureSet failureSet, final PostStateChange postStateChange) {
    if (null == postStateChange) {
      return;
    }
    if (isRelationalCallback(postStateChange.observableName(), postStateChange.observableClass())) {
      return;
    }
    verifyStateWithoutRelation(method, failureSet, postStateChange.from(), SyntaxErrors.POST_STATE_CHANGE_FROM_STATE_IS_INVALID);
    verifyStateWithoutRelation(method, failureSet, postStateChange.to(), SyntaxErrors.POST_STATE_CHANGE_TO_STATE_IS_INVALID);
  }

  private void verifyPreStateChange(final Method method, final VerificationFailureSet failureSet, final PreStateChange preStateChange) {
    if (null == preStateChange) {
      return;
    }
    if (isRelationalCallback(preStateChange.observableName(), preStateChange.observableClass())) {
      return;
    }
    verifyStateWithoutRelation(method, failureSet, preStateChange.from(), SyntaxErrors.PRE_STATE_CHANGE_FROM_STATE_IS_INVALID);
    verifyStateWithoutRelation(method, failureSet, preStateChange.to(), SyntaxErrors.PRE_STATE_CHANGE_TO_STATE_IS_INVALID);
    if (AnyState.class == preStateChange.to()) {
      return;
    }
    verifyPreToStatePostEvaluate(method, failureSet, preStateChange.to(), (StateMachineMetadata) this.stateMachineObjectBuilderImpl.getMetaType());
  }

  private boolean isRelationalCallback(String relation, Class<?> observableClass) {
    return !CallbackConsts.NULL_STR.equals(relation) || Null.class != observableClass;
  }

  private void verifyPreToStatePostEvaluate(final Method method, final VerificationFailureSet failureSet, final Class<?> toStateClass,
      final StateMachineMetadata stateMachineMetadata) {
    final StateMetadata toState = ((StateMachineMetadata) this.stateMachineObjectBuilderImpl.getMetaType()).getState(toStateClass);
    if (null == toState) {
      return;
    }
    for (final EventMetadata event : stateMachineMetadata.getState(toStateClass).getPossibleReachingEvents()) {
      if (event.isConditional() && event.postValidate()) {
        failureSet.add(this.stateMachineObjectBuilderImpl.newVerificationFailure(this.stateMachineObjectBuilderImpl.getDottedPath(),
            SyntaxErrors.PRE_STATE_CHANGE_TO_POST_EVALUATE_STATE_IS_INVALID, toStateClass, method, event.getDottedPath()));
      }
    }
  }

  private void verifyStateWithoutRelation(final Method method, final VerificationFailureSet failureSet, final Class<?> stateClass, final String
      errorCode) {
    if (AnyState.class != stateClass) {
      if (stateMetadataNotFound(stateClass)) {
        failureSet.add(this.stateMachineObjectBuilderImpl.newVerificationException(method.getDeclaringClass().getName() + "." + stateClass + "."
            + errorCode, errorCode, stateClass, method, this.stateMachineObjectBuilderImpl.getMetaType().getPrimaryKey()));
      }
    }
  }

  private boolean stateMetadataNotFound(final Class<?> stateClass) {
    return null == this.stateMachineObjectBuilderImpl.getMetaType().getState(stateClass);
  }
}