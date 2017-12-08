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
package net.imadz.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class LazySetterImpl<T> implements Setter<T> {

  private final Method getter;
  private volatile Method setterMethod;

  public LazySetterImpl(Method getter) {
    this.getter = getter;
  }

  @Override
  public void invoke(Object reactiveObject, T state) {
    initSetter(reactiveObject);
    try {
      setterMethod.setAccessible(true);
      setterMethod.invoke(reactiveObject, state);
    } catch (Exception e) {
      if (e instanceof IllegalAccessException
          | e instanceof IllegalArgumentException
          | e instanceof InvocationTargetException) {
        throw new IllegalStateException(e);
      }
    } finally {
      setterMethod.setAccessible(false);
    }
  }

  private void initSetter(Object reactiveObject) {
    if (null == setterMethod || (setterMethod.getDeclaringClass() != reactiveObject.getClass())) {
      synchronized (this) {
        if (null == setterMethod || (setterMethod.getDeclaringClass() != reactiveObject.getClass())) {
          setterMethod = findSetter(reactiveObject);
        }
      }
    }
  }

  private Method findSetter(Object reactiveObject) {
    final String setterName = "set" + getter.getName().substring(3);
    for (Class<?> rawClass = reactiveObject.getClass(); null != rawClass && rawClass != Object.class; rawClass = rawClass.getSuperclass()) {
      try {
        return rawClass.getDeclaredMethod(setterName, getter.getReturnType());
      } catch (NoSuchMethodException e) {
        continue;
      } catch (SecurityException e) {
        continue;
      }
    }
    throw new IllegalStateException("state setter method: " + setterName + " Cannot be found through class: " + reactiveObject.getClass());
  }
}