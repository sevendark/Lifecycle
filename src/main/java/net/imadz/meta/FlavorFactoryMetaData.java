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
package net.imadz.meta;

import net.imadz.meta.impl.KeyedEnumerationMap;

import java.util.Map;

/**
 * MetaData flavor
 */
public abstract class FlavorFactoryMetaData<OWNER extends MetaData, E extends Enum<E> & Keyed<Class<?>>> implements FlavorMetaData<OWNER>,
    FlavorFactory {

  private final KeySet keySet;
  private final Map<Class<?>, E> flavorKeyMap;

  @SuppressWarnings("unchecked")
  protected FlavorFactoryMetaData(E... keyEnumeration) {
    flavorKeyMap = KeyedEnumerationMap.valueOf(keyEnumeration);
    keySet = new KeySet(flavorKeyMap.keySet().toArray(new Object[flavorKeyMap.size()]));
  }

  protected FlavorFactoryMetaData(Class<E> keyEnumeration) {
    flavorKeyMap = KeyedEnumerationMap.valueOf(keyEnumeration);
    keySet = new KeySet(flavorKeyMap.keySet().toArray(new Object[flavorKeyMap.size()]));
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }

  @Override
  public FlavorFactoryMetaData<OWNER, E> filter(MetaData parent, MetaDataFilter filter, boolean lazyFilter) {
    return this;
  }

  @Override
  public final KeySet getKeySet() {
    return keySet;
  }

  protected abstract Object buildFlavor(E type, Object container) throws Exception;

  @Override
  public final <T> T getFlavor(Class<T> flavorInterface, Object container) throws FlavorNotSupportedException {
    E type = this.flavorKeyMap.get(flavorInterface);
    if (null != type) {
      try {
        Object value = buildFlavor(type, container);
        if (null != value) {
          return flavorInterface.cast(value);
        }
      } catch (Exception e) {
        e.printStackTrace();
        throw new FlavorNotSupportedException(this, flavorInterface);
      }
    }
    throw new FlavorNotSupportedException(this, flavorInterface);
  }
}
