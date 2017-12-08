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
package net.imadz.lifecycle.syntax.relation;

import net.imadz.lifecycle.SyntaxErrors;
import net.imadz.lifecycle.annotations.CompositeState;
import net.imadz.lifecycle.annotations.EventSet;
import net.imadz.lifecycle.annotations.StateMachine;
import net.imadz.lifecycle.annotations.StateSet;
import net.imadz.lifecycle.annotations.Transition;
import net.imadz.lifecycle.annotations.relation.ErrorMessage;
import net.imadz.lifecycle.annotations.relation.InboundWhile;
import net.imadz.lifecycle.annotations.relation.Parent;
import net.imadz.lifecycle.annotations.relation.RelateTo;
import net.imadz.lifecycle.annotations.relation.RelationSet;
import net.imadz.lifecycle.annotations.relation.ValidWhile;
import net.imadz.lifecycle.annotations.state.Final;
import net.imadz.lifecycle.annotations.state.Initial;
import net.imadz.lifecycle.annotations.state.LifecycleOverride;
import net.imadz.lifecycle.annotations.state.ShortCut;
import net.imadz.lifecycle.syntax.BaseMetaDataTest;
import net.imadz.lifecycle.syntax.relation.RelationSyntaxMetadata.POwningStateMachine.Events.OwningX;
import net.imadz.lifecycle.syntax.relation.RelationSyntaxMetadata.POwningStateMachine.Events.OwningY;

public class RelationSyntaxMetadata extends BaseMetaDataTest {

  @StateMachine
  static interface InvalidRelationReferenceSM {

    @StateSet
    static interface States {

      @Initial
      @Transition(event = InvalidRelationReferenceSM.Events.X.class, value = B.class)
      static interface A {}

      @Final
      static interface B {}
    }

    @EventSet
    static interface Events {

      static interface X {}
    }
  }

  @StateMachine
  static interface RelatedSM {

    @StateSet
    static interface States {

      @Initial
      @Transition(event = RelatedSM.Events.RX.class, value = RB.class)
      static interface RA {}

      @Final
      static interface RB {}
    }

    @EventSet
    static interface Events {

      static interface RX {}
    }
  }

  @StateMachine
  static interface PStandalone {

    @StateSet
    static interface States {

      @Initial
      @Transition(event = PStandalone.Events.PX.class, value = PB.class)
      @InboundWhile(on = {RelatedSM.States.RB.class}, relation = PStandalone.Relations.PR.class, otherwise = {@ErrorMessage(
          bundle = SyntaxErrors.SYNTAX_ERROR_BUNDLE, code = SyntaxErrors.RELATION_OTHERWISE_ATTRIBUTE_OF_INBOUNDWHILE_INVALID,
          states = {RelatedSM.States.RA.class})})
      @ValidWhile(on = {RelatedSM.States.RB.class}, relation = PStandalone.Relations.PR.class, otherwise = {@ErrorMessage(
          bundle = SyntaxErrors.SYNTAX_ERROR_BUNDLE, code = SyntaxErrors.RELATION_OTHERWISE_ATTRIBUTE_OF_INBOUNDWHILE_INVALID,
          states = {RelatedSM.States.RA.class})})
      static interface PA {}

      @Final
      static interface PB {}
    }

    @EventSet
    static interface Events {

      static interface PX {}
    }

    @RelationSet
    static interface Relations {

      @RelateTo(RelatedSM.class)
      static interface PR {}
    }
  }

  @StateMachine
  static interface NStandalone {

    static String error = SyntaxErrors.RELATION_INBOUNDWHILE_RELATION_NOT_DEFINED_IN_RELATIONSET;

    @StateSet
    static interface States {

      @Initial
      @Transition(event = NStandalone.Events.NX.class, value = NB.class)
      @InboundWhile(on = {RelatedSM.States.RB.class}, relation = PStandalone.Relations.PR.class)
      @ValidWhile(on = {RelatedSM.States.RB.class}, relation = PStandalone.Relations.PR.class)
      static interface NA {}

      @Final
      static interface NB {}
    }

    @EventSet
    static interface Events {

      static interface NX {}
    }

    @RelationSet
    static interface Relations {

      @RelateTo(RelatedSM.class)
      static interface NR {}
    }
  }

  @StateMachine
  static interface NStandalone2 {

    static String error = SyntaxErrors.RELATION_ON_ATTRIBUTE_OF_INBOUNDWHILE_NOT_MATCHING_RELATION;

    @StateSet
    static interface States {

      @Initial
      @Transition(event = NStandalone2.Events.NX.class, value = NStandalone2.States.NB.class)
      @InboundWhile(on = {InvalidRelationReferenceSM.States.B.class}, relation = NStandalone2.Relations.NR.class)
      static interface NA {}

      @Final
      static interface NB {}
    }

    @EventSet
    static interface Events {

      static interface NX {}
    }

    @RelationSet
    static interface Relations {

      @RelateTo(RelatedSM.class)
      static interface NR {}
    }
  }

  @StateMachine
  static interface NStandalone3 {

    static String error = SyntaxErrors.RELATION_RELATED_TO_REFER_TO_NON_STATEMACHINE;

    @StateSet
    static interface States {

      @Initial
      @Transition(event = NStandalone3.Events.NX.class, value = NStandalone3.States.NB.class)
      @InboundWhile(on = {InvalidRelationReferenceSM.States.B.class}, relation = NStandalone2.Relations.NR.class)
      static interface NA {}

      @Final
      static interface NB {}
    }

    @EventSet
    static interface Events {

      static interface NX {}
    }

    @RelationSet
    static interface Relations {

      @RelateTo(NStandalone3.Events.NX.class)
      static interface NR {}
    }
  }

  @StateMachine
  static interface NStandalone4 {

    static String error = SyntaxErrors.RELATIONSET_MULTIPLE;

    @StateSet
    static interface States {

      @Initial
      @Transition(event = NStandalone4.Events.NX.class, value = NStandalone4.States.NB.class)
      @InboundWhile(on = {InvalidRelationReferenceSM.States.B.class}, relation = NStandalone2.Relations.NR.class)
      static interface NA {}

      @Final
      static interface NB {}
    }

    @EventSet
    static interface Events {

      static interface NX {}
    }

    @RelationSet
    static interface Relations {

      @RelateTo(NStandalone4.Events.NX.class)
      static interface NR {}
    }

    @RelationSet
    static interface Relations2 {

      @RelateTo(NStandalone4.Events.NX.class)
      static interface NR {}
    }
  }

  @StateMachine
  static interface Super {

    @StateSet
    static interface States {

      @Initial
      @Transition(event = Super.Events.SX.class, value = SB.class)
      @InboundWhile(relation = Super.Relations.SR.class, on = {RelatedSM.States.RB.class}, otherwise = {@ErrorMessage(
          bundle = SyntaxErrors.SYNTAX_ERROR_BUNDLE, code = SyntaxErrors.RELATION_OTHERWISE_ATTRIBUTE_OF_INBOUNDWHILE_INVALID,
          states = {RelatedSM.States.RA.class})})
      @ValidWhile(relation = Super.Relations.SR.class, on = {RelatedSM.States.RB.class}, otherwise = {@ErrorMessage(
          bundle = SyntaxErrors.SYNTAX_ERROR_BUNDLE, code = SyntaxErrors.RELATION_OTHERWISE_ATTRIBUTE_OF_INBOUNDWHILE_INVALID,
          states = {RelatedSM.States.RA.class})})
      static interface SA {}

      @Final
      static interface SB {}
    }

    @EventSet
    static interface Events {

      static interface SX {}
    }

    @RelationSet
    static interface Relations {

      @RelateTo(RelatedSM.class)
      static interface SR {}
    }
  }

  @StateMachine
  static interface PChild extends Super {

    @StateSet
    static interface States extends Super.States {

      @Transition(event = PChild.Events.PCX.class, value = CC.class)
      static interface CA extends Super.States.SA {}

      @Transition(event = PChild.Events.PCX.class, value = SB.class)
      @InboundWhile(relation = Super.Relations.SR.class, on = {RelatedSM.States.RB.class})
      static interface CC {}
    }

    @EventSet
    static interface Events extends Super.Events {

      static interface PCX {}

      ;
    }
  }

  @StateMachine
  static interface NChild extends Super {

    static String error = SyntaxErrors.RELATION_INBOUNDWHILE_RELATION_NOT_DEFINED_IN_RELATIONSET;

    @StateSet
    static interface States extends Super.States {

      @Transition(event = NChild.Events.NCX.class, value = NCC.class)
      static interface NCA extends Super.States.SA {}

      @Transition(event = NChild.Events.NCX.class, value = SB.class)
      @InboundWhile(relation = PStandalone.Relations.PR.class, on = {InvalidRelationReferenceSM.States.B.class})
      static interface NCC {}
    }

    @EventSet
    static interface Events extends Super.Events {

      static interface NCX {}

      ;
    }
  }

  @StateMachine
  static interface NChild2 extends Super {

    static String error = SyntaxErrors.RELATION_ON_ATTRIBUTE_OF_INBOUNDWHILE_NOT_MATCHING_RELATION;

    @StateSet
    static interface States extends Super.States {

      @Transition(event = NChild2.Events.NC2X.class, value = NC2C.class)
      static interface NCA extends Super.States.SA {}

      @Transition(event = NChild2.Events.NC2X.class, value = SB.class)
      @InboundWhile(relation = Super.Relations.SR.class, on = {InvalidRelationReferenceSM.States.B.class})
      static interface NC2C {}
    }

    @EventSet
    static interface Events extends Super.Events {

      static interface NC2X {}

      ;
    }
  }

  @StateMachine
  static interface NChild3 extends Super {

    static String error = SyntaxErrors.RELATION_VALIDWHILE_RELATION_NOT_DEFINED_IN_RELATIONSET;

    @StateSet
    static interface States extends Super.States {

      @Transition(event = NChild3.Events.NC3X.class, value = NC3C.class)
      static interface NC3A extends Super.States.SA {}

      @Transition(event = NChild3.Events.NC3X.class, value = SB.class)
      @ValidWhile(relation = PStandalone.Relations.PR.class, on = {InvalidRelationReferenceSM.States.B.class})
      static interface NC3C {}
    }

    @EventSet
    static interface Events extends Super.Events {

      static interface NC3X {}

      ;
    }
  }

  @StateMachine
  static interface NChild4 extends Super {

    static String error = SyntaxErrors.RELATION_ON_ATTRIBUTE_OF_VALIDWHILE_NOT_MACHING_RELATION;

    @StateSet
    static interface States extends Super.States {

      @Transition(event = NChild4.Events.NC4X.class, value = NC4C.class)
      static interface NC4A extends Super.States.SA {}

      @Transition(event = NChild4.Events.NC4X.class, value = SB.class)
      @ValidWhile(relation = Super.Relations.SR.class, on = {InvalidRelationReferenceSM.States.B.class})
      static interface NC4C {}
    }

    @EventSet
    static interface Events extends Super.Events {

      static interface NC4X {}

      ;
    }
  }

  @StateMachine
  static interface NStandalone5 {

    static String error = SyntaxErrors.RELATION_OTHERWISE_ATTRIBUTE_OF_INBOUNDWHILE_INVALID;

    @StateSet
    static interface States {

      @Initial
      @Transition(event = NStandalone5.Events.N5X.class, value = N5B.class)
      @InboundWhile(relation = NStandalone5.Relations.N5R.class, on = {RelatedSM.States.RB.class}, otherwise = {@ErrorMessage(
          bundle = SyntaxErrors.SYNTAX_ERROR_BUNDLE, code = SyntaxErrors.RELATION_OTHERWISE_ATTRIBUTE_OF_INBOUNDWHILE_INVALID,
          states = {InvalidRelationReferenceSM.States.A.class})})
      @ValidWhile(relation = NStandalone5.Relations.N5R.class, on = {RelatedSM.States.RB.class}, otherwise = {@ErrorMessage(
          bundle = SyntaxErrors.SYNTAX_ERROR_BUNDLE, code = SyntaxErrors.RELATION_OTHERWISE_ATTRIBUTE_OF_VALIDWHILE_INVALID,
          states = {InvalidRelationReferenceSM.States.A.class})})
      static interface N5A {}

      @Final
      static interface N5B {}
    }

    @EventSet
    static interface Events {

      static interface N5X {}
    }

    @RelationSet
    static interface Relations {

      @RelateTo(RelatedSM.class)
      static interface N5R {}
    }
  }

  @StateMachine
  static interface PStandaloneParent {

    @StateSet
    static interface States {

      @Initial
      @Transition(event = PStandaloneParent.Events.PPX.class, value = PStandaloneParent.States.PPB.class)
      @InboundWhile(on = {RelatedSM.States.RB.class}, relation = PStandaloneParent.Relations.PPR.class, otherwise = {@ErrorMessage(
          bundle = SyntaxErrors.SYNTAX_ERROR_BUNDLE, code = SyntaxErrors.RELATION_OTHERWISE_ATTRIBUTE_OF_INBOUNDWHILE_INVALID,
          states = {RelatedSM.States.RA.class})})
      @ValidWhile(on = {RelatedSM.States.RB.class}, relation = PStandaloneParent.Relations.PPR.class, otherwise = {@ErrorMessage(
          bundle = SyntaxErrors.SYNTAX_ERROR_BUNDLE, code = SyntaxErrors.RELATION_OTHERWISE_ATTRIBUTE_OF_VALIDWHILE_INVALID,
          states = {RelatedSM.States.RA.class})})
      static interface PPA {}

      @Final
      static interface PPB {}
    }

    @EventSet
    static interface Events {

      static interface PPX {}
    }

    @RelationSet
    static interface Relations {

      @Parent
      @RelateTo(RelatedSM.class)
      static interface PPR {}
    }
  }

  @StateMachine
  static interface POwningStateMachine {

    @StateSet
    static interface States {

      @Initial
      @Transition(event = OwningX.class, value = OwningB.class)
      static interface OwningA {}

      @CompositeState
      @Transition(event = OwningY.class, value = OwningC.class)
      static interface OwningB {

        @StateSet
        static interface CStates {

          @Initial
          @Transition(event = OwningB.CEvents.CompositeX.class, value = OwningB.CStates.CompositeB.class)
          @InboundWhile(on = {RelatedSM.States.RB.class}, relation = OwningB.CRelations.PCS1R.class, otherwise = {@ErrorMessage(
              bundle = SyntaxErrors.SYNTAX_ERROR_BUNDLE, code = SyntaxErrors.RELATION_OTHERWISE_ATTRIBUTE_OF_INBOUNDWHILE_INVALID,
              states = {RelatedSM.States.RA.class})})
          static interface CompositeA {}

          @Transition(event = OwningB.CEvents.CompositeX.class, value = OwningB.CStates.CompositeC.class)
          @InboundWhile(on = {RelatedSM.States.RB.class}, relation = OwningB.CRelations.PCS1R.class, otherwise = {@ErrorMessage(
              bundle = SyntaxErrors.SYNTAX_ERROR_BUNDLE, code = SyntaxErrors.RELATION_OTHERWISE_ATTRIBUTE_OF_INBOUNDWHILE_INVALID,
              states = {RelatedSM.States.RA.class})})
          static interface CompositeB {}

          @Final
          @ShortCut(OwningC.class)
          static interface CompositeC {}
        }

        @EventSet
        static interface CEvents {

          static interface CompositeX {}
        }

        @RelationSet
        static interface CRelations {

          @Parent
          @RelateTo(RelatedSM.class)
          static interface PCS1R {}
        }
      }

      @Final
      static interface OwningC {}
    }

    @EventSet
    static interface Events {

      static interface OwningX {}

      static interface OwningY {}
    }
  }

  @StateMachine
  static interface PParentRelationSuper {

    @StateSet
    static interface States {

      @Initial
      @Transition(event = PParentRelationSuper.Events.PPX.class, value = PParentRelationSuper.States.PPB.class)
      @InboundWhile(on = {RelatedSM.States.RB.class}, relation = PParentRelationSuper.Relations.PPR.class, otherwise = {@ErrorMessage(
          bundle = SyntaxErrors.SYNTAX_ERROR_BUNDLE, code = SyntaxErrors.RELATION_OTHERWISE_ATTRIBUTE_OF_INBOUNDWHILE_INVALID,
          states = {RelatedSM.States.RA.class})})
      @ValidWhile(on = {RelatedSM.States.RB.class}, relation = PParentRelationSuper.Relations.PPR.class, otherwise = {@ErrorMessage(
          bundle = SyntaxErrors.SYNTAX_ERROR_BUNDLE, code = SyntaxErrors.RELATION_OTHERWISE_ATTRIBUTE_OF_INBOUNDWHILE_INVALID,
          states = {RelatedSM.States.RA.class})})
      static interface PPA {}

      @Final
      static interface PPB {}
    }

    @EventSet
    static interface Events {

      static interface PPX {}
    }

    @RelationSet
    static interface Relations {

      @Parent
      @RelateTo(RelatedSM.class)
      static interface PPR {}
    }
  }

  @StateMachine
  static interface PParentRelationChild extends PParentRelationSuper {

    @RelationSet
    static interface Relations {

      @Parent
      @RelateTo(RelatedSM.class)
      @LifecycleOverride
      static interface PCR {}
    }
  }

  @StateMachine
  static interface NStandaloneParent {

    @StateSet
    static interface States {

      @Initial
      @Transition(event = NStandaloneParent.Events.PPX.class, value = NStandaloneParent.States.PPB.class)
      @InboundWhile(on = {RelatedSM.States.RB.class}, relation = NStandaloneParent.Relations.PPR.class, otherwise = {@ErrorMessage(
          bundle = SyntaxErrors.SYNTAX_ERROR_BUNDLE, code = SyntaxErrors.RELATION_OTHERWISE_ATTRIBUTE_OF_INBOUNDWHILE_INVALID,
          states = {RelatedSM.States.RA.class})})
      @ValidWhile(on = {RelatedSM.States.RB.class}, relation = NStandaloneParent.Relations.PPR.class, otherwise = {@ErrorMessage(
          bundle = SyntaxErrors.SYNTAX_ERROR_BUNDLE, code = SyntaxErrors.RELATION_OTHERWISE_ATTRIBUTE_OF_INBOUNDWHILE_INVALID,
          states = {RelatedSM.States.RA.class})})
      static interface PPA {}

      @Final
      static interface PPB {}
    }

    @EventSet
    static interface Events {

      static interface PPX {}
    }

    @RelationSet
    static interface Relations {

      @Parent
      @RelateTo(RelatedSM.class)
      static interface PPR {}

      @Parent
      @RelateTo(RelatedSM.class)
      static interface PPR2 {}
    }
  }

  @StateMachine
  static interface NParentRelationChild extends PParentRelationSuper {

    @RelationSet
    static interface Relations {

      @Parent
      @RelateTo(RelatedSM.class)
      // Forget to @Overrides
      static interface PCR {}
    }
  }

  @StateMachine
  static interface NOwningStateMachine {

    @StateSet
    static interface States {

      @Initial
      @Transition(event = NOwningStateMachine.Events.NOwningX.class, value = NOwningB.class)
      static interface NOwningA {}

      @CompositeState
      @Transition(event = NOwningStateMachine.Events.NOwningY.class, value = NOwningC.class)
      static interface NOwningB {

        @StateSet
        static interface NCStates {

          @Initial
          @Transition(event = NOwningB.CEvents.NCompositeX.class, value = NOwningB.NCStates.NCompositeB.class)
          @InboundWhile(on = {RelatedSM.States.RB.class}, relation = NOwningB.CRelations.NCR.class, otherwise = {@ErrorMessage(
              bundle = SyntaxErrors.SYNTAX_ERROR_BUNDLE, code = SyntaxErrors.RELATION_OTHERWISE_ATTRIBUTE_OF_INBOUNDWHILE_INVALID,
              states = {RelatedSM.States.RA.class})})
          static interface NCompositeA {}

          @Transition(event = NOwningB.CEvents.NCompositeX.class, value = NOwningB.NCStates.NCompositeC.class)
          @InboundWhile(on = {RelatedSM.States.RB.class}, relation = NOwningB.CRelations.NCR.class, otherwise = {@ErrorMessage(
              bundle = SyntaxErrors.SYNTAX_ERROR_BUNDLE, code = SyntaxErrors.RELATION_OTHERWISE_ATTRIBUTE_OF_INBOUNDWHILE_INVALID,
              states = {RelatedSM.States.RA.class})})
          static interface NCompositeB {}

          @Final
          @ShortCut(NOwningC.class)
          static interface NCompositeC {}
        }

        @EventSet
        static interface CEvents {

          static interface NCompositeX {}
        }

        @RelationSet
        static interface CRelations {

          @Parent
          @LifecycleOverride
          // It's illegal whether overrides or not
          @RelateTo(RelatedSM.class)
          static interface NCR {}
        }
      }

      @Final
      static interface NOwningC {}
    }

    @EventSet
    static interface Events {

      static interface NOwningX {}

      static interface NOwningY {}
    }

    @RelationSet
    static interface Relations {

      @Parent
      @RelateTo(RelatedSM.class)
      static interface NR {}
    }
  }

  @StateMachine
  static interface N2OwningStateMachine {

    @StateSet
    static interface States {

      @Initial
      @Transition(event = N2OwningStateMachine.Events.N2OwningX.class, value = N2OwningB.class)
      static interface N2OwningA {}

      @CompositeState
      @Transition(event = N2OwningStateMachine.Events.N2OwningY.class, value = N2OwningC.class)
      static interface N2OwningB {

        @StateSet
        static interface N2CStates {

          @Initial
          @Transition(event = N2OwningB.CEvents.N2CompositeX.class, value = N2OwningB.N2CStates.N2CompositeB.class)
          @InboundWhile(on = {RelatedSM.States.RB.class}, relation = N2OwningB.CRelations.N2CR.class, otherwise = {@ErrorMessage(
              bundle = SyntaxErrors.SYNTAX_ERROR_BUNDLE, code = SyntaxErrors.RELATION_OTHERWISE_ATTRIBUTE_OF_INBOUNDWHILE_INVALID,
              states = {RelatedSM.States.RA.class})})
          static interface N2CompositeA {}

          @Transition(event = N2OwningB.CEvents.N2CompositeX.class, value = N2OwningB.N2CStates.N2CompositeC.class)
          @InboundWhile(on = {RelatedSM.States.RB.class}, relation = N2OwningB.CRelations.N2CR.class, otherwise = {@ErrorMessage(
              bundle = SyntaxErrors.SYNTAX_ERROR_BUNDLE, code = SyntaxErrors.RELATION_OTHERWISE_ATTRIBUTE_OF_INBOUNDWHILE_INVALID,
              states = {RelatedSM.States.RA.class})})
          static interface N2CompositeB {}

          @Final
          @ShortCut(N2OwningC.class)
          static interface N2CompositeC {}
        }

        @EventSet
        static interface CEvents {

          static interface N2CompositeX {}
        }

        @RelationSet
        static interface CRelations {

          @Parent
          // @Overrides // It's illegal whether overrides or not
          @RelateTo(RelatedSM.class)
          static interface N2CR {}
        }
      }

      @Final
      static interface N2OwningC {}
    }

    @EventSet
    static interface Events {

      static interface N2OwningX {}

      static interface N2OwningY {}
    }

    @RelationSet
    static interface Relations {

      @Parent
      @RelateTo(RelatedSM.class)
      static interface N2R {}
    }
  }

  @StateMachine
  static interface NoRelateTo {

    @StateSet
    static interface States {

      @Initial
      @Transition(event = NoRelateTo.Events.Action.class, value = Finished.class)
      @ValidWhile(relation = NoRelateTo.Relations.Relative.class, on = RelatedSM.States.RA.class)
      static interface Created {}

      @Final
      static interface Finished {}
    }

    @EventSet
    static interface Events {

      static interface Action {}
    }

    @RelationSet
    static interface Relations {

      static interface Relative {}
    }
  }
}
