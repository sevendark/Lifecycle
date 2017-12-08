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
package net.imadz.lifecycle.meta.type;

import net.imadz.lifecycle.meta.MetaType;
import net.imadz.lifecycle.meta.Recoverable;
import net.imadz.lifecycle.meta.object.StateMachineObject;
import net.imadz.verification.VerificationException;

public interface StateMachineMetadata extends Recoverable, MetaType<StateMachineMetadata> {

  /* ///////////////////////////////////////////////////////////// */
    /* // State Machine Relation with other State Machine Methods // */
    /* ///////////////////////////////////////////////////////////// */
  boolean hasParent();

  StateMachineMetadata getParent();

  boolean hasRelation(Object relationKey);

  /**
   * @param relationKey
   * @return RelationMetadata in current StateMachine
   */
  RelationMetadata getDeclaredRelationMetadata(Object relationKey);

  /**
   * @param relationKey
   * @return RelationMetadata in StateMachine hierarchy.
   */
  RelationMetadata getRelationMetadata(Object relationKey);

  /* //////////////////////////////////////////////////// */
    /* /////////////// State Related Methods ////////////// */
    /* //////////////////////////////////////////////////// */
  StateMetadata[] getDeclaredStateSet();

  StateMetadata getDeclaredState(Object stateKey);

  /**
   * @return all states in current StateMachine, current StateMachine's
   * composite StateMachine, super StateMachine, super StateMachine's
   * composite StateMachine.
   */
  StateMetadata[] getAllStates();

  /**
   * @param stateKey
   * @return state in allStates by specified stateKey.
   */
  StateMetadata getState(Object stateKey);

  StateMetadata getInitialState();

  StateMetadata[] getFinalStates();

  /* //////////////////////////////////////////////////// */
    /* ///////////// Transtion Related Methods //////////// */
    /* //////////////////////////////////////////////////// */
  EventMetadata[] getDeclaredEventSet();

  // EventMetadata[] getSuperEventSet();
  EventMetadata getDeclaredEvent(Object eventKey);

  /**
   * @return events in current StateMachine, current StateMachine's
   * CompositeStateMachine, super StateMachines, super
   * StateMachines'composite
   * StateMachines.
   */
  EventMetadata[] getAllEvents();

  /**
   * @param eventKey
   * @return event in allEventSet by specified eventKey
   */
  EventMetadata getEvent(Object eventKey);

  EventMetadata getStateSynchronizationEvent();

  /**
   * @param clazz defined with @LifecycleMeta, and with @Event
   *              , @StateIndicator, @Relation.
   * @return a concrete instance of StateMachineMetadata, whose abstract
   * part is concreted by the clazz param.
   * @throws VerificationException
   */
  StateMachineObject<?> newInstance(Class<?> clazz) throws VerificationException;

  /* //////////////////////////////////////////////////// */
    /* //////// Methods For Composite State Machine /////// */
    /* //////////////////////////////////////////////////// */
  boolean isComposite();

  /**
   * @return a state machine template, in whose state defining this state
   * machine
   */
  StateMachineMetadata getOwningStateMachine();

  StateMetadata getOwningState();

  // StateMetadata[] getShortcutStateSet();
  StateMachineMetadata[] getCompositeStateMachines();

    /* //////////////////////////////////////////////////// */
    /* //////// Methods For Conditions /////// */
    /* //////////////////////////////////////////////////// */

  /**
   * @return condition metadata defined in current state machine and current
   * composite state machines.
   */
  ConditionMetadata[] getDeclaredConditions();

  /**
   * @return all condition metadata defined in current state machine, current
   * composite state machines, super state machine, super state
   * machine's composite state machines.
   */
  ConditionMetadata[] getAllCondtions();

  /**
   * @param conditionKey
   * @return condition metadata in the state machine's inheritance hierarchy,
   * including composite state machines.
   */
  ConditionMetadata getCondtion(Object conditionKey);

  /**
   * @param conditionKey
   * @return true if the condition can by found in the state machine's
   * inheritance hierarchy and composite state machines.
   */
  boolean hasCondition(Object conditionKey);

  LifecycleMetaRegistry getRegistry();

  boolean hasEvent(Object eventKey);
}
