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
package net.imadz.lifecycle.engine;

import net.imadz.lifecycle.StateConverter;
import net.imadz.lifecycle.annotations.Event;
import net.imadz.lifecycle.annotations.EventSet;
import net.imadz.lifecycle.annotations.LifecycleMeta;
import net.imadz.lifecycle.annotations.StateIndicator;
import net.imadz.lifecycle.annotations.StateMachine;
import net.imadz.lifecycle.annotations.StateSet;
import net.imadz.lifecycle.annotations.Transition;
import net.imadz.lifecycle.annotations.state.Converter;
import net.imadz.lifecycle.annotations.state.Final;
import net.imadz.lifecycle.annotations.state.Initial;
import net.imadz.lifecycle.engine.StateSetterTestMetadata.SetterTestStateMachine.Events.Do;

public class StateSetterTestMetadata extends EngineTestBase {

  @StateMachine
  static interface SetterTestStateMachine {

    @StateSet
    static interface States {

      @Initial
      @Transition(event = Events.Do.class, value = Done.class)
      static interface New {}

      @Final
      static interface Done {}
    }

    @EventSet
    static interface Events {

      static interface Do {}
    }
  }

  @LifecycleMeta(SetterTestStateMachine.class)
  public static interface LazySetterBusinessInterface {

    @StateIndicator
    String getState();

    @Event(Do.class)
    void doIt();
  }

  @net.imadz.lifecycle.annotations.ReactiveObject
  public static class LazySetterBusinessImpl implements LazySetterBusinessInterface {

    private String state = SetterTestStateMachine.States.New.class.getSimpleName();

    @Override
    public String getState() {
      return state;
    }

    @SuppressWarnings("unused")
    private void setState(String state) {
      this.state = state;
    }

    @Override
    @Event(Do.class)
    public void doIt() {
    }
  }

  @LifecycleMeta(SetterTestStateMachine.class)
  public static class EagerSetterBusinessImpl {

    private String state = SetterTestStateMachine.States.New.class.getSimpleName();

    public String getState() {
      return state;
    }

    @SuppressWarnings("unused")
    private void setState(String state) {
      this.state = state;
    }

    @Event(Do.class)
    public void doIt() {
    }
  }

  @StateMachine
  public static interface BooleanTypeStateMachine {

    @StateSet
    static interface States {

      @Initial
      @Transition(event = Events.Close.class, value = Closed.class)
      static interface Opened {}

      @Final
      static interface Closed {}
    }

    @EventSet
    static interface Events {

      static interface Close {}
    }
  }

  @LifecycleMeta(BooleanTypeStateMachine.class)
  public static class BooleanTypeObject {

    private boolean closed;

    @StateIndicator
    @Converter(BooleanTypeConverter.class)
    public boolean isClosed() {
      return closed;
    }

    @SuppressWarnings("unused")
    private void setClosed(boolean closed) {
      this.closed = closed;
    }

    @Event
    public void close() {
    }
  }

  public static class BooleanTypeConverter implements StateConverter<Boolean> {

    @Override
    public String toState(Boolean t) {
      if (t) {
        return BooleanTypeStateMachine.States.Closed.class.getSimpleName();
      } else {
        return BooleanTypeStateMachine.States.Opened.class.getSimpleName();
      }
    }

    @Override
    public Boolean fromState(String state) {
      if (BooleanTypeStateMachine.States.Opened.class.getSimpleName().equals(state)) {
        return false;
      } else {
        return true;
      }
    }
  }
}
