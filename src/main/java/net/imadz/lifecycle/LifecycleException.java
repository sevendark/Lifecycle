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
package net.imadz.lifecycle;

import net.imadz.utils.MadzRuntimeException;

public class LifecycleException extends MadzRuntimeException {

  private static final long serialVersionUID = 7069144333982097517L;
  private static final String LIFECYCLE = "Lifecycle";

  public LifecycleException(Class<?> cls, String bundle, String errorCode, Object... messageVars) {
    super(cls, bundle, errorCode, messageVars);
  }

  public LifecycleException(Class<?> cls, String bundle, String errorCode, String[] messageVars, Throwable cause) {
    super(cls, bundle, errorCode, messageVars, cause);
  }

  public LifecycleException(Class<?> cls, String bundle, String errorCode, Throwable cause) {
    super(cls, bundle, errorCode, cause);
  }

  public LifecycleException(Class<?> cls, String bundle, String errorCode) {
    super(cls, bundle, errorCode);
  }

  @Override
  public String getCategory() {
    return LIFECYCLE;
  }
}
