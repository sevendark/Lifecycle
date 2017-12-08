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

import net.imadz.lifecycle.annotations.Event;
import net.imadz.lifecycle.annotations.EventSet;
import net.imadz.lifecycle.annotations.LifecycleMeta;
import net.imadz.lifecycle.annotations.StateMachine;
import net.imadz.lifecycle.annotations.StateSet;
import net.imadz.lifecycle.annotations.Transition;
import net.imadz.lifecycle.annotations.Transitions;
import net.imadz.lifecycle.annotations.action.Condition;
import net.imadz.lifecycle.annotations.action.ConditionSet;
import net.imadz.lifecycle.annotations.action.Conditional;
import net.imadz.lifecycle.annotations.action.ConditionalEvent;
import net.imadz.lifecycle.annotations.relation.InboundWhile;
import net.imadz.lifecycle.annotations.relation.RelateTo;
import net.imadz.lifecycle.annotations.relation.Relation;
import net.imadz.lifecycle.annotations.relation.RelationSet;
import net.imadz.lifecycle.annotations.relation.ValidWhile;
import net.imadz.lifecycle.annotations.state.Final;
import net.imadz.lifecycle.annotations.state.Initial;
import net.imadz.lifecycle.annotations.state.LifecycleOverride;
import net.imadz.lifecycle.engine.CoreFuntionTestMetadata.CustomerLifecycleMeta.States.Draft;
import net.imadz.lifecycle.engine.CoreFuntionTestMetadata.InternetTVServiceLifecycle.Relations.TVProvider;
import net.imadz.lifecycle.engine.CoreFuntionTestMetadata.KeyBoardLifecycleMetadataPreValidateCondition.Conditions.TimesLeft;
import net.imadz.lifecycle.engine.CoreFuntionTestMetadata.KeyBoardLifecycleMetadataPreValidateCondition.Events.PressAnyKey;
import net.imadz.lifecycle.engine.CoreFuntionTestMetadata.KeyBoardLifecycleMetadataPreValidateCondition.Relations.PowerRelation;
import net.imadz.lifecycle.engine.CoreFuntionTestMetadata.PowerLifecycleMetadata.Conditions.PowerLeftCondition;
import net.imadz.lifecycle.engine.CoreFuntionTestMetadata.PowerLifecycleMetadata.Events.ReducePower;
import net.imadz.lifecycle.engine.CoreFuntionTestMetadata.PowerLifecycleMetadata.Events.ShutDown;
import net.imadz.lifecycle.engine.CoreFuntionTestMetadata.ServiceProviderLifecycle.States.ServiceAvailable;
import net.imadz.org.apache.bcel.Repository;
import net.imadz.org.apache.bcel.classfile.JavaClass;
import net.imadz.org.apache.bcel.util.BCELifier;
import net.imadz.verification.VerificationException;
import org.junit.BeforeClass;

import java.util.Date;

/**
 * <ol>
 * <li>Core Transitions</li>
 * <ol>
 * <li>Perform State Change</li>
 * <ol>
 * <li>State Validation</li>
 * <ol>
 * <li>Relation With ValidWhile</li>
 * </ol>
 * <li>Event Transition Validation</li>
 * <ol>
 * <li>check whether event is legal to current state</li>
 * </ol>
 * <li>Next State Evaluation and InboundWhile Validation</li>
 * <ol>
 * <li>pre-state-change-phase</li>
 * <li>post-state-change-phase</li>
 * </ol>
 * <li>Execute Event Method</li> <li>Locking</li>
 * <ol>
 * <li>Locking Strategy</li>
 * <ol>
 * <li>Core Java Lock: native synchronized or java.util.concurrent.ReentryLock</li>
 * <li>JPA Lock:&nbsp;</li>
 * <ol>
 * <li>EntityManager.lock with LockModeType</li>
 * <ol>
 * <li>Be careful with the impact of locking scope while working with different
 * database transaction isolation level.</li>
 * </ol>
 * </ol> <li>CustomizedLocking</li> </ol> </ol> <li>Set New State</li> </ol> <li>
 * Relational</li>
 * <ol>
 * <li>with Optimistic Lock Mode: Touch Related Object to increase&nbsp;related
 * object&nbsp;Optimistic Lock's&nbsp;version. (once object x become managed
 * entity, and then the related object is also managed, after touching those
 * related object, it is expecting the next database synchronization to write it
 * into database. Once there is concurrent modification which will directly lead
 * optimistic lock exception, and then the state change will fail. This is the
 * relational life cycle objects' validation strategy. )</li>
 * <ul>
 * <li>NOTE: Since JPA provides READ and WRITE&nbsp;OPTIMISTIC lock. READ can be
 * applied to check relation scenario, and WRITE can be applied to update state
 * scenario.&nbsp;</li>
 * </ul>
 * <li>Parent State Synchronization Event to update hierarchical business
 * objects.</li>
 * <ul>
 * <li><span style="font-size: 12px;">Example: For a service business, assuming
 * customer is the top level business object in CRM module/application, and
 * contracts, and service provisioning, and billing, and payment and etc are all
 * the hierarchical children business object. To suspend a customer's service,
 * with this Lifecycle Framework, there are ONLY two things to do:</span></li>
 * <ol>
 * <li><span style="font-size: 12px;">Suspend Customer ONLY, this will lead all
 * the children's business states to invalid states.</span></li>
 * <li><span style="font-size: 12px;">Synchronize the parent's State update with
 * Synchronization ONLY, because all the other events cannot happen, since
 * Lifecycle&nbsp;</span>Engine considers the children state is in invalid
 * state.<span style="font-size: 12px;">&nbsp;</span></li>
 * </ol>
 * </ul>
 * </ol>
 * </ol> <li><span style="font-size: 12px;">Recoverable Process</span></li>
 * <ol>
 * <ol>
 * <li>Corrupting invalid state before services started</li>
 * <li>Recover (Resume or Redo) Event after services are ready.&nbsp;</li>
 * <ol>
 * <li>These event methods will result in those corrupted recoverable
 * object into the service queue(pool, zone) first to ensure ordering</li>
 * </ol>
 * <li>RecoverableIterator</li>
 * <ol>
 * <li>Application can implement this interface and register the instance into
 * LifecycleModule.</li>
 * </ol>
 * </ol> </ol> <li>Callbacks VS Interceptors</li>
 * <ol>
 * <li>pre-state-change callback</li>
 * <li>post-state-change callback</li>
 * <li>context</li>
 * <ol>
 * <li>lifecycle object</li>
 * <li>event method</li>
 * <li>method arguments</li>
 * <li>from state</li>
 * <li>possible target states</li>
 * </ol>
 * </ol> <li>Lifecycle Events</li>
 * <ol>
 * <li>StateChangeEvent</li>
 * <ol>
 * <li>Object X is transiting from S1 to S2 with Event T</li>
 * </ol>
 * <li>EventEvent</li>
 * <ol>
 * <li>System Event</li>
 * <ol>
 * <li>Non-functional Corrupting Object X From S1 to S2</li>
 * <li>Non-functional&nbsp;Recovering Object X From S2 to S1</li>
 * <li>Non-functional&nbsp;Redoing Object X From S2 to S1</li>
 * </ol>
 * <li>Application Event&nbsp;</li>
 * <ol>
 * <li>Functional Transiting Object X From S1 to S2</li>
 * <li>Functional Transiting Object X From S1 to a Failed state with Fail
 * Event</li>
 * </ol>
 * </ol> </ol> <li>Versions</li> </ol>
 *
 * @author Barry
 */
public class CoreFuntionTestMetadata extends EngineTestBase {

  @BeforeClass
  public static void setup() throws VerificationException {
    registerMetaFromClass(CoreFuntionTestMetadata.class);
  }

  @StateMachine
  protected static interface CustomerLifecycleMeta {

    @StateSet
    static interface States {

      @Initial
      @Transition(event = CustomerLifecycleMeta.Events.Activate.class, value = {Active.class})
      static interface Draft {}

      @Transitions({@Transition(event = CustomerLifecycleMeta.Events.Suspend.class, value = Suspended.class),
          @Transition(event = CustomerLifecycleMeta.Events.Cancel.class, value = Canceled.class)})
      static interface Active {}

      @Transition(event = CustomerLifecycleMeta.Events.Resume.class, value = Active.class)
      static interface Suspended {}

      @Final
      static interface Canceled {}
    }

    @EventSet
    static interface Events {

      static interface Activate {}

      static interface Suspend {}

      static interface Resume {}

      static interface Cancel {}
    }
  }

  @LifecycleMeta(CustomerLifecycleMeta.class)
  public static class Customer extends ReactiveObject {

    protected Customer() {
      initialState(Draft.class.getSimpleName());
    }

    // @Event
    // public int activate() {
    // InterceptorController<Customer, Integer> a = new
    // InterceptorController<>();
    // InterceptContext<Customer, Integer> c = new
    // InterceptContext<>(Customer.class, this, "activate", new Class[0],
    // new Object[0]);
    // return a.exec(c, new Callable<Integer>() {
    //
    // @Override
    // public Integer call() throws Exception {
    // return activate$Impl();
    // }
    // }).intValue();
    // }
    //
    // public int activate$Impl() {
    // return 5;
    // }
    @Event
    public Customer activate() {
      return new Customer();
    }

    @Event
    public void suspend() {
    }

    @Event
    public void resume() {
    }

    @Event
    public void cancel() {
    }
  }

  public static void main(String[] args) throws Throwable {
    final JavaClass outerClass = Repository.lookupClass(Customer.class);
    BCELifier fier = new BCELifier(outerClass, System.out);
    fier.start();
  }

  @StateMachine
  protected static interface InternetServiceLifecycleMeta {

    @StateSet
    static interface States {

      @Initial
      @Transition(event = InternetServiceLifecycleMeta.Events.Start.class, value = {InternetServiceLifecycleMeta.States.InService.class})
      @ValidWhile(on = {CustomerLifecycleMeta.States.Active.class}, relation = InternetServiceLifecycleMeta.Relations.CustomerRelation.class)
      static interface New {}

      @Transition(event = InternetServiceLifecycleMeta.Events.End.class, value = {InternetServiceLifecycleMeta.States.Ended.class})
      // @InboundWhile(on = { CustomerLifecycleMeta.States.Active.class },
      // relation =
      // InternetServiceLifecycleMeta.Relations.CustomerRelation.class)
      static interface InService {}

      @Final
      static interface Ended {}
    }

    @EventSet
    static interface Events {

      static interface Start {}

      static interface End {}
    }

    @RelationSet
    static interface Relations {

      @RelateTo(value = CustomerLifecycleMeta.class)
      static interface CustomerRelation {}
    }
  }

  @StateMachine
  protected static interface ServiceProviderLifecycle {

    @StateSet
    static interface States {

      @Initial
      @Transition(event = Events.Shutdown.class, value = Closed.class)
      static interface ServiceAvailable {}

      @Final
      static interface Closed {}
    }

    @EventSet
    static interface Events {

      static interface Shutdown {}
    }
  }

  @StateMachine
  protected static interface InternetTVServiceLifecycle extends InternetServiceLifecycleMeta {

    @StateSet
    static interface States extends InternetServiceLifecycleMeta.States {

      @ValidWhile(relation = TVProvider.class, on = ServiceAvailable.class)
      static interface New extends InternetServiceLifecycleMeta.States.New {}
    }

    @RelationSet
    static interface Relations extends InternetServiceLifecycleMeta.Relations {

      @RelateTo(InternetTVProviderLifecycle.class)
      static interface TVProvider {}
    }
  }

  @StateMachine
  protected static interface InternetTVProviderLifecycle extends ServiceProviderLifecycle {}

  @LifecycleMeta(InternetTVServiceLifecycle.class)
  public static class InternetTVService extends BaseService<InternetTVServiceProvider> {

    public InternetTVService(Customer customer) {
      super(customer);
    }

    @Relation(InternetTVServiceLifecycle.Relations.TVProvider.class)
    public InternetTVServiceProvider getProvider() {
      return super.getProvider();
    }
  }

  @LifecycleMeta(InternetTVProviderLifecycle.class)
  public static class InternetTVServiceProvider extends BaseServiceProvider {}

  @StateMachine
  protected static interface VOIPServiceLifecycleMeta extends InternetServiceLifecycleMeta {

    @StateSet
    static interface States extends InternetServiceLifecycleMeta.States {

      @Initial
      @LifecycleOverride
      @ValidWhile(relation = VOIPServiceLifecycleMeta.Relations.VoipProvider.class, on = VOIPProviderLifecycleMeta.States.ServiceAvailable.class)
      @Transition(event = VOIPServiceLifecycleMeta.Events.Start.class, value = {VOIPServiceLifecycleMeta.States.InService.class})
      static interface New extends InternetServiceLifecycleMeta.States.New {}
    }

    @RelationSet
    static interface Relations extends InternetServiceLifecycleMeta.Relations {

      @RelateTo(VOIPProviderLifecycleMeta.class)
      static interface VoipProvider {}
    }
  }

  @StateMachine
  protected static interface VOIPProviderLifecycleMeta extends ServiceProviderLifecycle {}

  @LifecycleMeta(VOIPServiceLifecycleMeta.class)
  public static class VOIPService extends BaseService<VOIPProvider> {

    public VOIPService(Customer customer) {
      super(customer);
    }

    @Relation(VOIPServiceLifecycleMeta.Relations.VoipProvider.class)
    public VOIPProvider getProvider() {
      return super.getProvider();
    }
  }

  @LifecycleMeta(VOIPProviderLifecycleMeta.class)
  public static class VOIPProvider extends BaseServiceProvider {}

  @LifecycleMeta(InternetServiceLifecycleMeta.class)
  public class InternetServiceOrder extends ReactiveObject {

    private Date startDate;
    private Date endDate;
    @Relation(InternetServiceLifecycleMeta.Relations.CustomerRelation.class)
    private Customer customer;
    private String type;

    public InternetServiceOrder() {
      initialState(InternetServiceLifecycleMeta.States.New.class.getSimpleName());
    }

    public InternetServiceOrder(Date startDate, Date endDate, Customer customer, String type) {
      super();
      this.startDate = startDate;
      this.endDate = endDate;
      this.customer = customer;
      this.type = type;
      initialState(InternetServiceLifecycleMeta.States.New.class.getSimpleName());
    }

    @Event
    public void start() {
    }

    @Event
    public void end() {
    }

    public void setStartDate(Date startDate) {
      this.startDate = startDate;
    }

    public void setEndDate(Date endDate) {
      this.endDate = endDate;
    }

    public void setCustomer(Customer customer) {
      this.customer = customer;
    }

    public void setType(String type) {
      this.type = type;
    }

    public Date getStartDate() {
      return startDate;
    }

    public Date getEndDate() {
      return endDate;
    }

    public Customer getCustomer() {
      return customer;
    }

    public String getType() {
      return type;
    }
  }

  public CoreFuntionTestMetadata() {
    super();
  }

  @StateMachine
  protected static interface InternetServiceLifecycleMetaWithInboundWhile {

    @StateSet
    static interface States {

      @Initial
      @Transition(event = InternetServiceLifecycleMetaWithInboundWhile.Events.Start.class,
          value = {InternetServiceLifecycleMetaWithInboundWhile.States.InService.class})
      static interface New {}

      @Transition(event = InternetServiceLifecycleMetaWithInboundWhile.Events.End.class,
          value = {InternetServiceLifecycleMetaWithInboundWhile.States.Ended.class})
      @InboundWhile(on = {CustomerLifecycleMeta.States.Active.class},
          relation = InternetServiceLifecycleMetaWithInboundWhile.Relations.CustomerRelation.class)
      static interface InService {}

      @Final
      static interface Ended {}
    }

    @EventSet
    static interface Events {

      static interface Start {}

      static interface End {}
    }

    @RelationSet
    static interface Relations {

      @RelateTo(value = CustomerLifecycleMeta.class)
      static interface CustomerRelation {}
    }
  }

  @LifecycleMeta(InternetServiceLifecycleMetaWithInboundWhile.class)
  public class InternetServiceOrderWithInboundWhile extends ReactiveObject {

    private Date startDate;
    private Date endDate;
    @Relation(InternetServiceLifecycleMetaWithInboundWhile.Relations.CustomerRelation.class)
    private Customer customer;
    private String type;

    public InternetServiceOrderWithInboundWhile() {
      initialState(InternetServiceLifecycleMetaWithInboundWhile.States.New.class.getSimpleName());
    }

    public InternetServiceOrderWithInboundWhile(Date startDate, Date endDate, Customer customer, String type) {
      super();
      this.startDate = startDate;
      this.endDate = endDate;
      this.customer = customer;
      this.type = type;
      initialState(InternetServiceLifecycleMeta.States.New.class.getSimpleName());
    }

    @Event
    public void start() {
    }

    @Event
    public void end() {
    }

    public void setStartDate(Date startDate) {
      this.startDate = startDate;
    }

    public void setEndDate(Date endDate) {
      this.endDate = endDate;
    }

    public void setCustomer(Customer customer) {
      this.customer = customer;
    }

    public void setType(String type) {
      this.type = type;
    }

    public Date getStartDate() {
      return startDate;
    }

    public Date getEndDate() {
      return endDate;
    }

    public Customer getCustomer() {
      return customer;
    }

    public String getType() {
      return type;
    }
  }

  @StateMachine
  static interface PowerLifecycleMetadata {

    @StateSet
    static interface States {

      @Initial
      @Transitions(value = {@Transition(event = ShutDown.class, value = {PowerOff.class}),
          @Transition(event = ReducePower.class, value = {PowerOn.class, PowerOff.class})})
      static interface PowerOn {}

      @Final
      static interface PowerOff {}
    }

    @EventSet
    static interface Events {

      static interface ShutDown {}

      @Conditional(condition = PowerLeftCondition.class, judger = PowerLeftConditionImpl.class)
      static interface ReducePower {}
    }

    @ConditionSet
    static interface Conditions {

      static interface PowerLeftCondition {

        boolean powerLeft();

        boolean isShutDown();
      }
    }

    static class PowerLeftConditionImpl implements ConditionalEvent<PowerLeftCondition> {

      @Override
      public Class<?> doConditionJudge(PowerLeftCondition t) {
        if (t.isShutDown()) {
          return PowerLifecycleMetadata.States.PowerOff.class;
        }
        if (t.powerLeft()) {
          return PowerLifecycleMetadata.States.PowerOn.class;
        } else {
          return PowerLifecycleMetadata.States.PowerOff.class;
        }
      }
    }
  }

  @StateMachine
  static interface KeyBoardLifecycleMetadataPreValidateCondition {

    @StateSet
    static interface States {

      @Initial
      @Transition(event = PressAnyKey.class, value = {ReadingInput.class, Broken.class /*
                                                                                                  * ,
                                                                                                  * NotReading
                                                                                                  * .
                                                                                                  * class
                                                                                                  */})
      @InboundWhile(on = {PowerLifecycleMetadata.States.PowerOn.class}, relation = PowerRelation.class)
      static interface ReadingInput {}

      // @Transition(event = PressAnyKey.class, value = {
      // ReadingInput.class })
      // @ValidWhile(on = { PowerLifecycleMetadata.States.PowerOn.class },
      // relation = PowerRelation.class)
      // @InboundWhile(on = { PowerLifecycleMetadata.States.PowerOff.class
      // }, relation = PowerRelation.class)
      // static interface NotReading {}
      @InboundWhile(on = {PowerLifecycleMetadata.States.PowerOn.class}, relation = PowerRelation.class)
      @Final
      static interface Broken {}
    }

    @EventSet
    static interface Events {

      @Conditional(condition = TimesLeft.class, judger = ConditionJudgerImpl.class, postEval = false)
      static interface PressAnyKey {}
    }

    @RelationSet
    static interface Relations {

      @RelateTo(PowerLifecycleMetadata.class)
      static interface PowerRelation {}
    }

    @ConditionSet
    static interface Conditions {

      static interface TimesLeft {

        boolean timesLeft();

        boolean isPowerOff();
      }
    }

    public static class ConditionJudgerImpl implements ConditionalEvent<TimesLeft> {

      @Override
      public Class<?> doConditionJudge(TimesLeft t) {
        // if ( t.isPowerOff() ) {
        // return
        // KeyBoardLifecycleMetadataPreValidateCondition.States.NotReading.class;
        // } else
        if (t.timesLeft()) {
          return KeyBoardLifecycleMetadataPreValidateCondition.States.ReadingInput.class;
        } else {
          return KeyBoardLifecycleMetadataPreValidateCondition.States.Broken.class;
        }
      }
    }
  }

  @StateMachine
  static interface KeyBoardLifecycleMetadataPostValidateCondition extends KeyBoardLifecycleMetadataPreValidateCondition {

    @EventSet
    static interface Events extends KeyBoardLifecycleMetadataPreValidateCondition.Events {

      @LifecycleOverride
      @Conditional(condition = KeyBoardLifecycleMetadataPreValidateCondition.Conditions.TimesLeft.class, judger = ConditionJudgerImpl.class,
          postEval = true)
      static interface PressAnyKey extends KeyBoardLifecycleMetadataPreValidateCondition.Events.PressAnyKey {}
    }
  }

  @LifecycleMeta(PowerLifecycleMetadata.class)
  public static class PowerObject extends ReactiveObject implements PowerLeftCondition {

    private int powerLeft = 2;
    private boolean shutdown = false;

    @Event
    public void shutDown() {
      shutdown = true;
    }

    @Event
    public void reducePower() {
      powerLeft--;
    }

    public PowerObject() {
      initialState(PowerLifecycleMetadata.States.PowerOn.class.getSimpleName());
    }

    @Condition(PowerLifecycleMetadata.Conditions.PowerLeftCondition.class)
    public PowerLeftCondition getPowerLeftCondition() {
      return this;
    }

    @Override
    public boolean powerLeft() {
      return powerLeft > 0;
    }

    @Override
    public boolean isShutDown() {
      return shutdown;
    }
  }

  @LifecycleMeta(PowerLifecycleMetadata.class)
  public static class NonGetterConditionPowerObject extends ReactiveObject implements PowerLeftCondition {

    private int powerLeft = 2;
    private boolean shutdown = false;

    @Event
    public void shutDown() {
      shutdown = true;
    }

    @Event
    public void reducePower() {
      powerLeft--;
    }

    public NonGetterConditionPowerObject() {
      initialState(PowerLifecycleMetadata.States.PowerOn.class.getSimpleName());
    }

    @Condition(PowerLifecycleMetadata.Conditions.PowerLeftCondition.class)
    public PowerLeftCondition powerLeftCondition() {
      return this;
    }

    @Override
    public boolean powerLeft() {
      return powerLeft > 0;
    }

    @Override
    public boolean isShutDown() {
      return shutdown;
    }
  }

  @LifecycleMeta(KeyBoardLifecycleMetadataPostValidateCondition.class)
  public static class NonGetterConditionKeyBoardObjectPostValidateCondition extends ReactiveObject implements TimesLeft {

    private int times = 2;
    private NonGetterConditionPowerObject powerObject;

    public NonGetterConditionKeyBoardObjectPostValidateCondition(NonGetterConditionPowerObject powerObject) {
      super();
      this.powerObject = powerObject;
      initialState(KeyBoardLifecycleMetadataPostValidateCondition.States.ReadingInput.class.getSimpleName());
    }

    @Condition(KeyBoardLifecycleMetadataPostValidateCondition.Conditions.TimesLeft.class)
    public TimesLeft timeLeft() {
      return this;
    }

    @Relation(PowerRelation.class)
    public NonGetterConditionPowerObject getPowerRelation() {
      return this.powerObject;
    }

    @Event
    public void pressAnyKey() {
      times = times - 1;
      powerObject.reducePower();
    }

    @Override
    public boolean timesLeft() {
      return times > 0;
    }

    @Override
    public boolean isPowerOff() {
      return !powerObject.powerLeft();
    }
  }

  @LifecycleMeta(KeyBoardLifecycleMetadataPreValidateCondition.class)
  public static class KeyBoardObjectPreValidateCondition extends ReactiveObject implements TimesLeft {

    private int times = 2;
    @Relation(PowerRelation.class)
    private PowerObject powerObject;

    public KeyBoardObjectPreValidateCondition(PowerObject powerObject) {
      this.powerObject = powerObject;
      initialState(KeyBoardLifecycleMetadataPreValidateCondition.States.ReadingInput.class.getSimpleName());
    }

    @Condition(KeyBoardLifecycleMetadataPreValidateCondition.Conditions.TimesLeft.class)
    public TimesLeft getTimeLeft() {
      return this;
    }

    @Event
    public void pressAnyKey() {
      times = times - 1;
    }

    @Override
    public boolean timesLeft() {
      return times > 0;
    }

    @Override
    public boolean isPowerOff() {
      return !powerObject.powerLeft() || powerObject.shutdown;
    }
  }

  @LifecycleMeta(KeyBoardLifecycleMetadataPostValidateCondition.class)
  public static class KeyBoardObjectPostValidateCondition extends ReactiveObject implements TimesLeft {

    private int times = 2;
    @Relation(PowerRelation.class)
    private PowerObject powerObject;

    public KeyBoardObjectPostValidateCondition(PowerObject powerObject) {
      super();
      this.powerObject = powerObject;
      initialState(KeyBoardLifecycleMetadataPostValidateCondition.States.ReadingInput.class.getSimpleName());
    }

    @Condition(KeyBoardLifecycleMetadataPostValidateCondition.Conditions.TimesLeft.class)
    public TimesLeft getTimeLeft() {
      return this;
    }

    @Event
    public void pressAnyKey() {
      times = times - 1;
      powerObject.reducePower();
    }

    @Override
    public boolean timesLeft() {
      return times > 0;
    }

    @Override
    public boolean isPowerOff() {
      return !powerObject.powerLeft();
    }
  }

  @LifecycleMeta(InternetServiceLifecycleMeta.class)
  public static class BaseServiceWithRelationOnFields<T extends BaseServiceProvider> extends ReactiveObject {

    @Relation(InternetServiceLifecycleMeta.Relations.CustomerRelation.class)
    private Customer customer;

    public BaseServiceWithRelationOnFields(Customer customer) {
      initialState(InternetServiceLifecycleMeta.States.New.class.getSimpleName());
      this.customer = customer;
    }

    private T provider;

    public T getProvider() {
      return provider;
    }

    public void setProvider(T provider) {
      this.provider = provider;
    }

    public Customer getCustomer() {
      return customer;
    }

    public void setCustomer(Customer customer) {
      this.customer = customer;
    }

    @Event
    void start() {
    }

    @Event
    void end() {
    }
  }

  @LifecycleMeta(InternetTVServiceLifecycle.class)
  public static class InternetTVServiceWithRelationOnFields extends BaseServiceWithRelationOnFields<InternetTVServiceProvider> {

    public InternetTVServiceWithRelationOnFields(Customer customer, InternetTVServiceProvider provider) {
      super(customer);
      this.provider = provider;
    }

    @Relation(InternetTVServiceLifecycle.Relations.TVProvider.class)
    public InternetTVServiceProvider provider;
  }

  @StateMachine
  static interface MemberShipLifecycleMeta {

    @StateSet
    static interface States {

      @Initial
      @Transition(event = Events.Activate.class, value = {Active.class})
      static interface Draft {}

      @Transition(event = Events.Expire.class, value = {Expired.class})
      static interface Active {}

      @Final
      static interface Expired {}
    }

    @EventSet
    static interface Events {

      static interface Activate {}

      static interface Expire {}
    }
  }

  @StateMachine
  static interface OrderValidWhileNullableLifecycleMeta {

    @StateSet
    static interface States {

      @Initial
      @Transition(event = Events.Pay.class, value = {Paid.class})
      @ValidWhile(on = {MemberShipLifecycleMeta.States.Active.class}, relation = Relations.MemberShipRelation.class, nullable = true)
      static interface Draft {}

      @Final
      static interface Paid {}
    }

    @EventSet
    static interface Events {

      static interface Pay {}
    }

    @RelationSet
    static interface Relations {

      @RelateTo(MemberShipLifecycleMeta.class)
      static interface MemberShipRelation {}
    }
  }

  @StateMachine
  static interface OrderValidWhileNotNullableLifecycleMeta {

    @StateSet
    static interface States {

      @Initial
      @Transition(event = Events.Pay.class, value = {Paid.class})
      @ValidWhile(on = {MemberShipLifecycleMeta.States.Active.class}, relation = Relations.MemberShipRelation.class, nullable = false)
      static interface Draft {}

      @Final
      static interface Paid {}
    }

    @EventSet
    static interface Events {

      static interface Pay {}
    }

    @RelationSet
    static interface Relations {

      @RelateTo(MemberShipLifecycleMeta.class)
      static interface MemberShipRelation {}
    }
  }

  @LifecycleMeta(MemberShipLifecycleMeta.class)
  public static class MemberShip extends ReactiveObject {

    private int point;
    private Date activatedOn;
    private Date expiredOn;

    public MemberShip() {
      this.initialState(MemberShipLifecycleMeta.States.Draft.class.getSimpleName());
    }

    @Event(MemberShipLifecycleMeta.Events.Activate.class)
    public void active() {
      this.activatedOn = new Date();
      point = 0;
    }

    @Event
    public void expire() {
      this.expiredOn = new Date();
      point = 0;
    }

    public void setPoint(int point) {
      this.point = point;
    }

    public int getPoint() {
      return point;
    }

    public Date getActivatedOn() {
      return activatedOn;
    }

    public Date getExpiredOn() {
      return expiredOn;
    }
  }

  @LifecycleMeta(OrderValidWhileNullableLifecycleMeta.class)
  public static class OrderValidWhileNullable extends ReactiveObject {

    private double totalAmount = 400d;
    @Relation(OrderValidWhileNullableLifecycleMeta.Relations.MemberShipRelation.class)
    private MemberShip memberShip;

    public OrderValidWhileNullable(MemberShip memberShip) {
      this.memberShip = memberShip;
      this.initialState(OrderValidWhileNullableLifecycleMeta.States.Draft.class.getSimpleName());
    }

    @Event
    public void pay() {
      if (null != memberShip) {
        memberShip.setPoint((int) totalAmount);
      }
    }
  }

  @LifecycleMeta(OrderValidWhileNotNullableLifecycleMeta.class)
  public static class OrderValidWhileNotNullable extends ReactiveObject {

    private double totalAmount = 400d;
    @Relation(OrderValidWhileNotNullableLifecycleMeta.Relations.MemberShipRelation.class)
    private MemberShip memberShip;

    public OrderValidWhileNotNullable(MemberShip memberShip) {
      this.memberShip = memberShip;
      this.initialState(OrderValidWhileNotNullableLifecycleMeta.States.Draft.class.getSimpleName());
    }

    @Event
    public void pay() {
      if (null != memberShip) {
        memberShip.setPoint((int) totalAmount);
      }
    }
  }

  @StateMachine
  static interface OrderInboundWhileNullableLifecycleMeta {

    @StateSet
    static interface States {

      @Initial
      @Transition(event = Events.Pay.class, value = {Paid.class})
      static interface Draft {}

      @Final
      @InboundWhile(on = {MemberShipLifecycleMeta.States.Active.class}, relation = Relations.MemberShipRelation.class, nullable = true)
      static interface Paid {}
    }

    @EventSet
    static interface Events {

      static interface Pay {}
    }

    @RelationSet
    static interface Relations {

      @RelateTo(MemberShipLifecycleMeta.class)
      static interface MemberShipRelation {}
    }
  }

  @StateMachine
  static interface OrderInboundWhileNotNullableLifecycleMeta {

    @StateSet
    static interface States {

      @Initial
      @Transition(event = Events.Pay.class, value = {Paid.class})
      static interface Draft {}

      @Final
      @InboundWhile(on = {MemberShipLifecycleMeta.States.Active.class}, relation = Relations.MemberShipRelation.class, nullable = false)
      static interface Paid {}
    }

    @EventSet
    static interface Events {

      static interface Pay {}
    }

    @RelationSet
    static interface Relations {

      @RelateTo(MemberShipLifecycleMeta.class)
      static interface MemberShipRelation {}
    }
  }

  @LifecycleMeta(OrderInboundWhileNullableLifecycleMeta.class)
  public static class OrderInboundWhileNullable extends ReactiveObject {

    @Relation(OrderInboundWhileNullableLifecycleMeta.Relations.MemberShipRelation.class)
    private MemberShip memberShip;

    @Event
    public void pay() {
    }

    public OrderInboundWhileNullable(MemberShip memberShip) {
      this.memberShip = memberShip;
      this.initialState(OrderInboundWhileNullableLifecycleMeta.States.Draft.class.getSimpleName());
    }
  }

  @LifecycleMeta(OrderInboundWhileNotNullableLifecycleMeta.class)
  public static class OrderInboundWhileNotNullable extends ReactiveObject {

    @Relation(OrderInboundWhileNotNullableLifecycleMeta.Relations.MemberShipRelation.class)
    private MemberShip memberShip;

    @Event
    public void pay() {
    }

    public OrderInboundWhileNotNullable(MemberShip memberShip) {
      this.memberShip = memberShip;
      this.initialState(OrderInboundWhileNotNullableLifecycleMeta.States.Draft.class.getSimpleName());
    }
  }
}