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
package net.imadz.lifecycle.demo.inheritance.meta;

import net.imadz.lifecycle.annotations.CompositeState;
import net.imadz.lifecycle.annotations.EventSet;
import net.imadz.lifecycle.annotations.StateMachine;
import net.imadz.lifecycle.annotations.StateSet;
import net.imadz.lifecycle.annotations.Transition;
import net.imadz.lifecycle.annotations.Transitions;
import net.imadz.lifecycle.annotations.state.Final;
import net.imadz.lifecycle.annotations.state.Initial;
import net.imadz.lifecycle.annotations.state.LifecycleOverride;
import net.imadz.lifecycle.annotations.state.ShortCut;
import net.imadz.lifecycle.demo.inheritance.meta.PlantScheduleOrderLifecycleMeta.Events.Finish;
import net.imadz.lifecycle.demo.inheritance.meta.VehicleScheduleOrderLifecycleMeta.States.Ongoing.SubEvents.DoConstruct;
import net.imadz.lifecycle.demo.inheritance.meta.VehicleScheduleOrderLifecycleMeta.States.Ongoing.SubEvents.DoTransport;

@StateMachine
public interface VehicleScheduleOrderLifecycleMeta extends OrderLifecycleMeta {

  @StateSet
  public static class States extends OrderLifecycleMeta.States {

    @LifecycleOverride
    @CompositeState
    public static class Ongoing extends OrderLifecycleMeta.States.Ongoing {

      @StateSet
      public static class SubStates {

        @Initial
        @Transition(event = DoTransport.class, value = OnPassage.class)
        public static class Loading {}

        @Transitions({@Transition(event = DoConstruct.class, value = Constructing.class)})
        public static class OnPassage {}

        @Transition(event = Finish.class, value = Exit.class)
        public static class Constructing {}

        @Final
        @ShortCut(Finished.class)
        public static class Exit {}
      }

      @EventSet
      public static class SubEvents extends OrderLifecycleMeta.Events {

        public static class DoTransport {}

        public static class DoConstruct {}
      }
    }
  }
}
