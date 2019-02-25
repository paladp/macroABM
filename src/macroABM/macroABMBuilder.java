/*
 *	This class serves as the main class for the simulation. The build method is called by
 *	the simulation when it is ran and it returns the context with all the projections and
 *	agents to be used by the simulation.
 */
package macroABM;

import repast.simphony.context.Context;
import repast.simphony.context.space.continuous.ContinuousSpaceFactory;
import repast.simphony.context.space.continuous.ContinuousSpaceFactoryFinder;
import repast.simphony.context.space.graph.NetworkBuilder;
import repast.simphony.dataLoader.ContextBuilder;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.RandomCartesianAdder;
import repast.simphony.space.graph.Network;

public class macroABMBuilder implements ContextBuilder<Object> {

  // This function builds to context, adds projections to the context, and adds agents to the
  // context
  @Override
  public Context build(Context<Object> primaryContext) {
    primaryContext.setId("macroABM Context");
    ContinuousSpaceFactory spaceFactory =
        ContinuousSpaceFactoryFinder.createContinuousSpaceFactory(null);

    ContinuousSpace<Object> primarySpace =
        spaceFactory.createContinuousSpace(
            "primary space",
            primaryContext,
            new RandomCartesianAdder<Object>(),
            new repast.simphony.space.continuous.WrapAroundBorders(),
            50,
            50);

    NetworkBuilder<Object> consumptionBuilder =
        new NetworkBuilder<Object>("consumption network", primaryContext, false);
    Network<Object> mainConsumptionNetwork = consumptionBuilder.buildNetwork();

    NetworkBuilder<Object> bankBuilder =
        new NetworkBuilder<Object>("banking network", primaryContext, false);
    Network<Object> mainBankingNetwork = bankBuilder.buildNetwork();

    return primaryContext;
  }

  public static void main(String[] args) {
    HouseholdAgent testingAgent = new HouseholdAgent();
    System.out.println(testingAgent.getCash());
  }
}
