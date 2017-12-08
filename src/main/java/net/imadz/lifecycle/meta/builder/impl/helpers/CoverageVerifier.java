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
import net.imadz.lifecycle.annotations.Event;
import net.imadz.lifecycle.meta.builder.impl.StateMachineObjectBuilderImpl;
import net.imadz.lifecycle.meta.type.EventMetadata;
import net.imadz.lifecycle.meta.type.EventMetadata.EventTypeEnum;
import net.imadz.util.MethodScanCallback;
import net.imadz.util.StringUtil;
import net.imadz.utils.Null;
import net.imadz.verification.VerificationFailureSet;

import java.lang.reflect.Method;
import java.util.HashSet;

public final class CoverageVerifier implements MethodScanCallback {

  private final StateMachineObjectBuilderImpl<?> stateMachineObjectBuilderImpl;
  private final EventMetadata eventMetadata;
  HashSet<Class<?>> declaringClass = new HashSet<Class<?>>();
  private final VerificationFailureSet failureSet;

  public CoverageVerifier(StateMachineObjectBuilderImpl<?> stateMachineObjectBuilderImpl, final EventMetadata eventMetadata,
      final VerificationFailureSet failureSet) {
    this.stateMachineObjectBuilderImpl = stateMachineObjectBuilderImpl;
    this.eventMetadata = eventMetadata;
    this.failureSet = failureSet;
  }

  public boolean notCovered() {
    return declaringClass.size() == 0;
  }

  @Override
  public boolean onMethodFound(Method method) {
    if (method.isBridge()) {
      return false;
    }
    if (!match(eventMetadata, method)) {
      return false;
    }
    if (!declaringClass.contains(method.getDeclaringClass())) {
      declaringClass.add(method.getDeclaringClass());
      return false;
    }
    final EventTypeEnum type = eventMetadata.getType();
    if (type.isUniqueEvent()) {
      failureSet.add(this.stateMachineObjectBuilderImpl.newVerificationFailure(eventMetadata.getDottedPath(),
          SyntaxErrors.LM_REDO_CORRUPT_RECOVER_EVENT_HAS_ONLY_ONE_METHOD, eventMetadata.getDottedPath().getName(), "@" + type.name(),
          this.stateMachineObjectBuilderImpl.getMetaType().getDottedPath(), this.stateMachineObjectBuilderImpl.getDottedPath().getAbsoluteName()));
    }
    return false;
  }

  private boolean match(EventMetadata eventMetadata, Method eventMethod) {
    Event event = eventMethod.getAnnotation(Event.class);
    if (null == event) {
      return false;
    }
    final String eventName = eventMetadata.getDottedPath().getName();
    if (Null.class == event.value()) {
      return eventName.equals(StringUtil.toUppercaseFirstCharacter(eventMethod.getName()));
    } else {
      return eventName.equals(event.value().getSimpleName());
    }
  }
}