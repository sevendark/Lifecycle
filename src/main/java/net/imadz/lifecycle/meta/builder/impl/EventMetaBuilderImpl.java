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

import net.imadz.common.Dumper;
import net.imadz.lifecycle.SyntaxErrors;
import net.imadz.lifecycle.annotations.action.Conditional;
import net.imadz.lifecycle.annotations.action.ConditionalEvent;
import net.imadz.lifecycle.annotations.action.Corrupt;
import net.imadz.lifecycle.annotations.action.Fail;
import net.imadz.lifecycle.annotations.action.Recover;
import net.imadz.lifecycle.annotations.action.Redo;
import net.imadz.lifecycle.annotations.action.Timeout;
import net.imadz.lifecycle.meta.builder.EventMetaBuilder;
import net.imadz.lifecycle.meta.type.EventMetadata;
import net.imadz.lifecycle.meta.type.StateMachineMetadata;
import net.imadz.util.StringUtil;
import net.imadz.verification.VerificationException;
import net.imadz.verification.VerificationFailureSet;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class EventMetaBuilderImpl extends InheritableAnnotationMetaBuilderBase<EventMetadata, StateMachineMetadata> implements EventMetaBuilder {

  private EventTypeEnum type = EventTypeEnum.Common;
  private boolean conditional;
  private Class<?> conditionClass;
  private Class<? extends ConditionalEvent<?>> judgerClass;
  private boolean postValidate;
  private long timeout;

  protected EventMetaBuilderImpl(StateMachineMetadata parent, String name) {
    super(parent, "EventSet." + name);
  }

  @Override
  public void verifyMetaData(VerificationFailureSet verificationSet) {
  }

  @Override
  public EventMetaBuilder build(Class<?> clazz, StateMachineMetadata parent) throws VerificationException {
    super.build(clazz, parent);
    configureSuper(clazz);
    configureCondition(clazz);
    configureType(clazz);
    configureTimeout(clazz);
    return this;
  }

  private void configureTimeout(Class<?> clazz) {
    final Timeout timeout = clazz.getAnnotation(Timeout.class);
    if (null != timeout) {
      this.timeout = timeout.value();
    }
  }

  private void configureType(Class<?> clazz) {
    if (null != clazz.getAnnotation(Corrupt.class)) {
      type = EventTypeEnum.Corrupt;
    } else if (null != clazz.getAnnotation(Redo.class)) {
      type = EventTypeEnum.Redo;
    } else if (null != clazz.getAnnotation(Recover.class)) {
      type = EventTypeEnum.Recover;
    } else if (null != clazz.getAnnotation(Fail.class)) {
      type = EventTypeEnum.Fail;
    } else {
      type = EventTypeEnum.Common;
    }
  }

  private void configureCondition(Class<?> clazz) throws VerificationException {
    Conditional conditionalAnno = clazz.getAnnotation(Conditional.class);
    if (null != conditionalAnno) {
      conditional = true;
      conditionClass = conditionalAnno.condition();
      judgerClass = conditionalAnno.judger();
      postValidate = conditionalAnno.postEval();
      verifyJudgerClass(clazz, judgerClass, conditionClass);
    } else {
      conditional = false;
    }
  }

  private void verifyJudgerClass(Class<?> clazz, Class<?> judgerClass, Class<?> conditionClass) throws VerificationException {
    for (Type type : judgerClass.getGenericInterfaces()) {
      if (!(type instanceof ParameterizedType)) {
        continue;
      }
      final ParameterizedType pType = (ParameterizedType) type;
      if (isConditionalEvent((Class<?>) pType.getRawType()) && !isConditionClassMatchingJudgerGenericType(conditionClass, pType)) {
        throw newVerificationException(getDottedPath(), SyntaxErrors.EVENT_CONDITIONAL_CONDITION_NOT_MATCH_JUDGER, clazz, conditionClass,
            judgerClass);
      }
    }
  }

  private boolean isConditionClassMatchingJudgerGenericType(Class<?> conditionClass, final ParameterizedType pType) {
    return conditionClass.isAssignableFrom((Class<?>) pType.getActualTypeArguments()[0]);
  }

  private boolean isConditionalEvent(final Class<?> rawType) {
    return ConditionalEvent.class.isAssignableFrom(rawType);
  }

  @Override
  public StateMachineMetadata getStateMachine() {
    return parent;
  }

  @Override
  public EventTypeEnum getType() {
    return type;
  }

  @Override
  public long getTimeout() {
    return this.timeout;
  }

  @Override
  public void dump(Dumper dumper) {
  }

  @Override
  public boolean isConditional() {
    return conditional;
  }

  @Override
  public Class<?> getConditionClass() {
    return conditionClass;
  }

  @Override
  public Class<? extends ConditionalEvent<?>> getJudgerClass() {
    return judgerClass;
  }

  @Override
  public boolean postValidate() {
    return postValidate;
  }

  @Override
  protected void verifySuper(Class<?> metaClass) throws VerificationException {
    if (!parent.hasSuper()) {
      throw newVerificationException(getDottedPath(), SyntaxErrors.EVENT_ILLEGAL_EXTENTION, metaClass, getSuperMetaClass(metaClass));
    } else {
      if (!parent.getSuper().hasEvent(getSuperMetaClass(metaClass))) {
        throw newVerificationException(getDottedPath(), SyntaxErrors.EVENT_EXTENED_EVENT_CAN_NOT_FOUND_IN_SUPER_STATEMACHINE, metaClass,
            getSuperMetaClass(metaClass), parent.getSuper().getPrimaryKey());
      }
    }
  }

  @Override
  protected EventMetadata findSuper(Class<?> metaClass) throws VerificationException {
    return parent.getSuper().getEvent(metaClass);
  }

  @Override
  protected boolean extendsSuperKeySet() {
    return true;
  }

  @Override
  public void verifyEventMethod(Method method, VerificationFailureSet failureSet) {
    if (method.getParameterTypes().length <= 0) {
      return;
    }
    if (EventTypeEnum.Corrupt == getType() || EventTypeEnum.Recover == getType() || EventTypeEnum.Redo == getType()) {
      failureSet.add(newVerificationFailure(getDottedPath(), SyntaxErrors.EVENT_TYPE_CORRUPT_RECOVER_REDO_REQUIRES_ZERO_PARAMETER, method,
          StringUtil.toUppercaseFirstCharacter(method.getName()), getType()));
    }
  }
}
