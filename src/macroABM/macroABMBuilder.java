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

    NetworkBuilder<Object> laborBuilder =
        new NetworkBuilder<Object>("labor network", primaryContext, false);
    Network<Object> mainLaborNetwork = laborBuilder.buildNetwork();

    EconomicAgent.setContext(primaryContext);
    EconomicAgent.setConsumptionNetwork(mainConsumptionNetwork);
    EconomicAgent.setLaborNetwork(mainLaborNetwork);
    EconomicAgent.setSpace(primarySpace);

    int numberOfHouseholds = 50;
    for (int currentHousehold = 0; currentHousehold < numberOfHouseholds; currentHousehold++) {
      primaryContext.add(new HouseholdAgent());
    }

    int numberOfFirms = 10;
    for (int currentFirm = 0; currentFirm < numberOfFirms; currentFirm++) {
      primaryContext.add(new ConsumptionFirmAgent());
    }

    return primaryContext;
  }

  public static void main(String[] args) {}
}
