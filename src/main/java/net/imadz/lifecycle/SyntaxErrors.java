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

public interface SyntaxErrors {

  public static final String SYNTAX_ERROR_BUNDLE = "syntax_error";
  public static final String REGISTERED_META_ERROR = "002-1000";
  // StateMachine
  /**
   * message argument {0} state machine class
   */
  public static final String STATEMACHINE_SUPER_MUST_BE_STATEMACHINE = "002-2100";
  /**
   * message argument {0} state machine class
   */
  public static final String STATEMACHINE_HAS_ONLY_ONE_SUPER_INTERFACE = "002-2101";
  /**
   * message argument {0} state machine class
   */
  public static final String STATEMACHINE_CLASS_WITHOUT_ANNOTATION = "002-2102";
  /**
   * message argument {0} state machine class
   */
  public static final String STATEMACHINE_WITHOUT_STATESET = "002-2103";
  /**
   * message argument {0} state machine class
   */
  public static final String STATEMACHINE_MULTIPLE_STATESET = "002-2104";
  /**
   * message argument {0} state machine class
   */
  public static final String STATEMACHINE_WITHOUT_EVENTSET = "002-2105";
  /**
   * message argument {0} state machine class
   */
  public static final String STATEMACHINE_MULTIPLE_EVENTSET = "002-2106";
  /**
   * message argument {0} state machine class
   */
  public static final String STATEMACHINE_WITHOUT_INNER_CLASSES_OR_INTERFACES = "002-2107";
  /**
   * message argument {0} state machine class
   */
  public static final String STATEMACHINE_MULTIPLE_CONDITIONSET = "002-2108";
  // StateSet
  /**
   * message argument {0} state set class
   */
  public static final String STATESET_WITHOUT_INITIAL_STATE = "002-2400";
  /**
   * message argument {0} state set class
   */
  public static final String STATESET_MULTIPLE_INITAL_STATES = "002-2401";
  /**
   * message argument {0} state set class
   */
  public static final String STATESET_WITHOUT_FINAL_STATE = "002-2402";
  /**
   * message argument {0} state set class
   */
  public static final String STATESET_WITHOUT_STATE = "002-2403";
  // EventSet
  /**
   * message argument {0} event set class
   */
  public static final String EVENTSET_WITHOUT_EVENT = "002-2501";
  /**
   * message argument {0} method object
   * message argument {1} eventKey, will be eventKey class when it's
   * available, otherwise it will be event method name with first
   * char upper case.
   * message argument {2} eventType
   */
  public static final String EVENT_TYPE_CORRUPT_RECOVER_REDO_REQUIRES_ZERO_PARAMETER = "002-2502";
  /**
   * message argument {0} Event class
   * message argument {1} condition class
   * message argument {2} juder class
   */
  public static final String EVENT_CONDITIONAL_CONDITION_NOT_MATCH_JUDGER = "002-2503";
  /**
   * message argument {0} Event class
   * message argument {1} Event class' super class or interface
   */
  public static final String EVENT_ILLEGAL_EXTENTION = "002-2504";
  /**
   * message argument {0} Event class
   * message argument {1} Event class' super class or interface
   * message argument {2} Super state machine class
   */
  public static final String EVENT_EXTENED_EVENT_CAN_NOT_FOUND_IN_SUPER_STATEMACHINE = "002-2505";
  // State's Transition
  /**
   * message argument {0} @Transition definition
   * message argument {1} State class
   * message argument {2} Event class
   */
  public static final String FUNCTION_INVALID_EVENT_REFERENCE = "002-2610";
  public static final String FUNCTION_CONDITIONAL_EVENT_WITHOUT_CONDITION = "002-2611";
  public static final String FUNCTION_EVENT_MUST_BE_NOT_ON_END_STATE = "002-2613";
  public static final String FUNCTION_WITH_EMPTY_STATE_CANDIDATES = "002-2614";
  public static final String STATE_NON_FINAL_WITHOUT_FUNCTIONS = "002-2615";
  /**
   * message argument {0} stateClass
   * message argument {1} transtionClass
   */
  public static final String STATE_DEFINED_MULTIPLE_FUNCTION_REFERRING_SAME_EVENT = "002-2616";
  /**
   * message argument {0} stateClass
   */
  public static final String STATE_OVERRIDES_WITHOUT_SUPER_CLASS = "002-2617";
  /**
   * message argument {0} stateClass
   * message argument {1} superStateClass
   */
  public static final String STATESET_WITHOUT_INITAL_STATE_AFTER_OVERRIDING_SUPER_INITIAL_STATE = "002-2618";
  /**
   * message argument {0} stateClass
   * message argument {1} superClass
   */
  public static final String STATE_SUPER_CLASS_IS_NOT_STATE_META_CLASS = "002-2619";
  public static final String FUNCTION_NEXT_STATESET_OF_FUNCTION_INVALID = "002-2700";
  // State's Shortcut
  public static final String SHORT_CUT_INVALID = "002-2800";
  public static final String COMPOSITE_STATEMACHINE_SHORTCUT_WITHOUT_END = "002-2801";
  public static final String COMPOSITE_STATEMACHINE_FINAL_STATE_WITHOUT_SHORTCUT = "002-2802";
  public static final String COMPOSITE_STATEMACHINE_SHORTCUT_STATE_INVALID = "002-2803";
  /**
   * message argument {0} composite state machine class
   */
  public static final String COMPOSITE_STATEMACHINE_CANNOT_EXTENDS_OWNING_STATEMACHINE = "002-2804";
  // State's Relation
  public static final String RELATION_INBOUNDWHILE_RELATION_NOT_DEFINED_IN_RELATIONSET = "002-2911";
  public static final String RELATION_ON_ATTRIBUTE_OF_INBOUNDWHILE_NOT_MATCHING_RELATION = "002-2912";
  public static final String RELATION_OTHERWISE_ATTRIBUTE_OF_INBOUNDWHILE_INVALID = "002-2913";
  public static final String RELATION_OTHERWISE_ATTRIBUTE_OF_VALIDWHILE_INVALID = "002-2914";
  public static final String RELATION_VALIDWHILE_RELATION_NOT_DEFINED_IN_RELATIONSET = "002-2921";
  public static final String RELATION_ON_ATTRIBUTE_OF_VALIDWHILE_NOT_MACHING_RELATION = "002-2922";
  public static final String RELATION_RELATED_TO_REFER_TO_NON_STATEMACHINE = "002-2931";
  // LifecycleMeta
  public static final String LM_EVENT_METHOD_WITH_INVALID_EVENT_REFERENCE = "002-3211";
  public static final String LM_EVENT_NOT_CONCRETED_IN_LM = "002-3212";
  public static final String LM_METHOD_NAME_INVALID = "002-3213";
  public static final String LM_REDO_CORRUPT_RECOVER_EVENT_HAS_ONLY_ONE_METHOD = "002-3214";
  public static final String LM_MUST_CONCRETE_ALL_RELATIONS = "002-3220";
  /**
   * message argument {0} @LifecycleMeta Class
   * message argument {1} invalid relation instance
   * message argument {2} StateMachine
   */
  public static final String LM_REFERENCE_INVALID_RELATION_INSTANCE = "002-3221";
  /**
   * message argument {0} @LifecycleMeta Class
   * message argument {1} relation instance
   */
  public static final String LM_RELATION_INSTANCE_MUST_BE_UNIQUE = "002-3222";
  /**
   * message argument {0} method
   * message argument {1} @LifecycleMeta Class
   * message argument {2} relation
   * message argument {3} state
   */
  public static final String LM_RELATION_NOT_BE_CONCRETED = "002-3223";
  /**
   * message argument {0} method
   */
  public static final String LM_RELATION_ON_METHOD_PARAMETER_MUST_SPECIFY_VALUE = "002-3224";
  /**
   * message argument {0} @LifecycleMeta class
   * message argument {1} @StateMachine dottedPath
   * message argument {2} Condition Class
   */
  public static final String LM_CONDITION_NOT_COVERED = "002-3230";
  /**
   * message argument {0} Method full qualified name
   * message argument {1} Invalid condition class
   */
  public static final String LM_CONDITION_REFERENCE_INVALID = "002-3231";
  /**
   * message argument {0} @LifecycleMeta class
   * message argument {1} Condition class
   */
  public static final String LM_CONDITION_MULTIPLE_METHODS_REFERENCE_SAME_CONDITION = "002-3232";
  /**
   * message argument {0} Method class
   * message argument {1} Condition class
   */
  public static final String LM_CONDITION_OBJECT_DOES_NOT_IMPLEMENT_CONDITION_INTERFACE = "002-3233";
  /**
   * message argument {0} class annotated with @LifecycleMeta
   */
  public static final String STATE_INDICATOR_CANNOT_FIND_DEFAULT_AND_SPECIFIED_STATE_INDICATOR = "002-3300";
  /**
   * message argument {0} setter method object
   */
  public static final String STATE_INDICATOR_CANNOT_EXPOSE_STATE_INDICATOR_SETTER = "002-3301";
  /**
   * message argument {0} setter field object
   */
  public static final String STATE_INDICATOR_CANNOT_EXPOSE_STATE_INDICATOR_FIELD = "002-3302";
  /**
   * message argument {0} class annotated with @LifecycleMeta
   */
  public static final String STATE_INDICATOR_SETTER_NOT_FOUND = "002-3303";
  /**
   * message argument {0} class annotated with @LifecycleMeta
   * message argument {1} state type
   */
  public static final String STATE_INDICATOR_CONVERTER_NOT_FOUND = "002-3304";
  /**
   * message argument {0} class annotated with @LifecycleMeta
   * message argument {1} actual state type
   * message argument {2} invalid converter
   * message argument {3} raw type that converter converts
   */
  public static final String STATE_INDICATOR_CONVERTER_INVALID = "002-3305";
  /**
   * message argument {0} class annotated with @LifecycleMeta
   */
  public static final String STATE_INDICATOR_MULTIPLE_STATE_INDICATOR_ERROR = "002-3306";
  // @ConditionSet
  /**
   * message argument {0} stateMachine class
   */
  public static final String CONDITIONSET_MULTIPLE = "002-3400";
  // @RelationSet
  public static final String RELATIONSET_MULTIPLE = "002-3500";
  /**
   * message argument {0} stateMachine class
   */
  public static final String RELATION_MULTIPLE_PARENT_RELATION = "002-3501";
  /**
   * message argument {0} child stateMachine class
   * message argument {1} super stateMachine class
   */
  public static final String RELATION_NEED_OVERRIDES_TO_OVERRIDE_SUPER_STATEMACHINE_PARENT_RELATION = "002-3502";
  /**
   * message argument {0} composite stateMachine dottedPath
   * message argument {1} parent relation class of composite stateMachine class
   * message argument {2} owning stateMachine class
   * message argument {3} parent relation class of owning stateMachine class
   */
  public static final String RELATION_COMPOSITE_STATE_MACHINE_CANNOT_OVERRIDE_OWNING_PARENT_RELATION = "002-3503";
  /**
   * message argument {0} Relation Class
   */
  public static final String RELATION_NO_RELATED_TO_DEFINED = "002-3504";
  /**
   * message argument {0} Lifecycle Lock class
   */
  public static final String LIFECYCLE_LOCK_SHOULD_HAVE_NO_ARGS_CONSTRUCTOR = "002-3600";
  /**
   * message argument {0} Lifecycle Event Handler Class
   */
  public static final String LIFECYCLE_EVENT_HANDLER_MUST_HAVE_NO_ARG_CONSTRUCTOR = "002-3601";
  /**
   * message argument {0} To state class
   * message argument {1} Call back Method
   * message argument {2} Event Dotted Path
   */
  public static final String PRE_STATE_CHANGE_TO_POST_EVALUATE_STATE_IS_INVALID = "002-3700";
  /**
   * message argument {0} From state class
   * message argument {1} Method class
   * message argument {2} State Machine class
   */
  public static final String PRE_STATE_CHANGE_FROM_STATE_IS_INVALID = "002-3701";
  /**
   * message argument {0} To state class
   * message argument {1} Method class
   * message argument {2} State Machine class
   */
  public static final String PRE_STATE_CHANGE_TO_STATE_IS_INVALID = "002-3702";
  /**
   * message argument {0} From state class
   * message argument {1} Method class
   * message argument {2} State Machine class
   */
  public static final String POST_STATE_CHANGE_FROM_STATE_IS_INVALID = "002-3703";
  /**
   * message argument {0} To state class
   * message argument {1} Method class
   * message argument {2} State Machine class
   */
  public static final String POST_STATE_CHANGE_TO_STATE_IS_INVALID = "002-3704";
  /**
   * message argument {0} relation
   * message argument {1} Method class
   * message argument {2} State Machine object class
   */
  public static final String PRE_STATE_CHANGE_RELATION_INVALID = "002-3705";
  /**
   * message argument {0} mappedby
   * message argument {1} Method class
   * message argument {2} Observerable Class
   */
  public static final String PRE_STATE_CHANGE_MAPPEDBY_INVALID = "002-3706";
  /**
   * message argument {0} relation
   * message argument {1} Method class
   * message argument {2} State Machine object class
   */
  public static final String POST_STATE_CHANGE_RELATION_INVALID = "002-3707";
  /**
   * message argument {0} mappedby
   * message argument {1} Method class
   * message argument {2} Observerable Class
   */
  public static final String POST_STATE_CHANGE_MAPPEDBY_INVALID = "002-3708";
  /**
   * message argument {0} observable name
   * message argument {1} observable class
   * message argument {2} Method class
   */
  public static final String POST_STATE_CHANGE_OBSERVABLE_NAME_MISMATCH_OBSERVABLE_CLASS = "002-3709";
  /**
   * message argument {0} observable class
   * message argument {1} Method class
   */
  public static final String POST_STATE_CHANGE_OBSERVABLE_CLASS_INVALID = "002-3710";
  /**
   * message argument {0} observable class
   * message argument {1} Method class
   */
  public static final String PRE_STATE_CHANGE_OBSERVABLE_CLASS_INVALID = "002-3711";
  /**
   * message argument {0} observable name
   * message argument {1} observable class
   * message argument {2} Method class
   */
  public static final String PRE_STATE_CHANGE_OBSERVABLE_NAME_MISMATCH_OBSERVABLE_CLASS = "002-3712";
  /**
   * message argument {0} mappedby
   * message argument {1} Method class
   * message argument {2} Observerable Class
   */
  public static final String ON_EVENT_MAPPEDBY_INVALID = "002-3713";
  /**
   * message argument {0} Event class
   * message argument {1} Method
   * message argument {2} StateMachine PrimaryKey
   */
  public static final String ON_EVENT_EVENT_IS_INVALID = "002-3714";
  /**
   * message argument {0} observable class
   * message argument {1} Method class
   */
  public static final String ON_EVENT_OBSERVABLE_CLASS_INVALID = "002-3715";
  /**
   * message argument {0} relation
   * message argument {1} Method class
   * message argument {2} State Machine object class
   */
  public static final String ON_EVENT_RELATION_INVALID = "002-3716";
  /**
   * message argument {0} observable name
   * message argument {1} observable class
   * message argument {2} Method class
   */
  public static final String ON_EVENT_OBSERVABLE_NAME_MISMATCH_OBSERVABLE_CLASS = "002-3717";
}
