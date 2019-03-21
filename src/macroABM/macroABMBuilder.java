/*
 *	This class serves as the main class for the simulation. The build method is called by
 *	the simulation when it is ran and it returns the context with all the projections and
 *	agents to be used by the simulation.
 */
package macroABM;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import repast.simphony.context.Context;
import repast.simphony.context.space.continuous.ContinuousSpaceFactory;
import repast.simphony.context.space.continuous.ContinuousSpaceFactoryFinder;
import repast.simphony.context.space.graph.NetworkBuilder;
import repast.simphony.dataLoader.ContextBuilder;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.RandomCartesianAdder;
import repast.simphony.space.graph.Network;
import repast.simphony.space.graph.RepastEdge;

public class macroABMBuilder implements ContextBuilder<Object> {

  BigDecimal techProductionCoeff = new BigDecimal("3.00");
  public Context<Object> context;
  public Network<Object> consumptionNetwork;
  public Network<Object> laborNetwork;
  public Network<Object> constraintNetwork;
  int numberOfFirms = 10;
  int numberOfHouseholds = 50;
  ArrayList<ConsumptionFirmAgent> consumptionFirms = new ArrayList<ConsumptionFirmAgent>();
  ArrayList<HouseholdAgent> households = new ArrayList<HouseholdAgent>();
  int percentChanceToFindNewPartner = 25;
  double percentThresholdForLowerPrices = 0.99;

  // This function builds to context, adds projections to the context, and adds agents to the
  // context
  @Override
  public Context build(Context<Object> primaryContext) {
    primaryContext.setId("macroABM");

    // Create the continuous space, consumption network, and labor network
    ContinuousSpaceFactory spaceFactory =
        ContinuousSpaceFactoryFinder.createContinuousSpaceFactory(null);
    ContinuousSpace<Object> primarySpace =
        spaceFactory.createContinuousSpace(
            "primarySpace",
            primaryContext,
            new RandomCartesianAdder<Object>(),
            new repast.simphony.space.continuous.WrapAroundBorders(),
            100,
            100);

    NetworkBuilder<Object> consumptionBuilder =
        new NetworkBuilder<Object>("consumptionNetwork", primaryContext, false);
    consumptionBuilder.buildNetwork();

    NetworkBuilder<Object> laborBuilder =
        new NetworkBuilder<Object>("laborNetwork", primaryContext, false);
    laborBuilder.buildNetwork();

    NetworkBuilder<Object> constraintBuilder =
        new NetworkBuilder<Object>("constraintNetwork", primaryContext, false);
    constraintBuilder.buildNetwork();

    // Save the created space and networks, and then save references to them in all the agents
    this.context = primaryContext;
    this.consumptionNetwork = (Network<Object>) this.context.getProjection("consumptionNetwork");
    this.laborNetwork = (Network<Object>) this.context.getProjection("laborNetwork");
    this.constraintNetwork = (Network<Object>) this.context.getProjection("constraintNetwork");
    EconomicAgent.setContext(primaryContext);
    EconomicAgent.setConsumptionNetwork(this.consumptionNetwork);
    EconomicAgent.setLaborNetwork(laborNetwork);
    EconomicAgent.setSpace(primarySpace);

    // Add the agents to the context
    for (int currentHousehold = 0; currentHousehold < numberOfHouseholds; currentHousehold++) {
      primaryContext.add(new HouseholdAgent());
    }

    for (int currentFirm = 0; currentFirm < numberOfFirms; currentFirm++) {
      primaryContext.add(new ConsumptionFirmAgent());
    }

    // Get an iterable that contains all of the agents in the context
    Iterable<Object> consumptionFirmsIterable = this.context.getObjects(ConsumptionFirmAgent.class);
    Iterable<Object> householdFirmsIterable = this.context.getObjects(HouseholdAgent.class);

    // Clear the arraylists before we build them again because they persist between sim resets
    this.consumptionFirms.clear();
    this.households.clear();

    // Get the agents from the iterable and add them to the arraylists so they are easier to work
    // with
    for (Object currentConsumptionFirm : consumptionFirmsIterable) {
      this.consumptionFirms.add((ConsumptionFirmAgent) currentConsumptionFirm);
    }

    for (Object currentHouseholdAgent : householdFirmsIterable) {
      this.households.add((HouseholdAgent) currentHouseholdAgent);
    }

    // Build the labor and consumption networks initial state
    buildInitialLaborNetwork();
    buildInitialConsumptionNetwork();

    findBetterTradingPartners();

    calcMarketShareByEmployees();
    return primaryContext;
  }

  public void consume() {
    // Go through every single household
    for (HouseholdAgent currentHousehold : this.households) {

      // Try one buyer first to see if all of their demand can be met with just one partner
      Iterable<Object> tradingPartnersIterable =
          this.consumptionNetwork.getAdjacent(currentHousehold);
      ArrayList<ConsumptionFirmAgent> tradingPartners = new ArrayList<ConsumptionFirmAgent>();
      for (Object currentPartner : tradingPartnersIterable) {
        tradingPartners.add((ConsumptionFirmAgent) currentPartner);
      }
      Collections.shuffle(tradingPartners);

      ConsumptionFirmAgent firstTradingPartner = tradingPartners.get(0);

      // Keep buying and stop once the firm runs out or this household has consumed all it needs to,
      // or the household has less money than the price of the good
      while ((currentHousehold.getConsumedToday() < currentHousehold.getConsumptionPerDay())
          && firstTradingPartner.canSell() == true
          && currentHousehold.getCash().intValue()
              > firstTradingPartner.getPrice().getAmountMajorInt()) {
        transaction buyOneGood =
            new transaction(
                currentHousehold,
                firstTradingPartner.getPrice(),
                1,
                firstTradingPartner,
                "consumption");
        currentHousehold.handleTransaction(buyOneGood);
        firstTradingPartner.handleTransaction(buyOneGood);
      }

      // Check if we ended because we could not consume all we needed, and could still afford to
      if (currentHousehold.getConsumedToday() < currentHousehold.getConsumptionPerDay()
          && currentHousehold.getCash().intValue()
              > firstTradingPartner.getPrice().getAmountMajorInt()) {

        // If we did, then we know we were demand constrained, so add the edge with the weight
        // representing the amount we were unsatisfied
        int amountUnsatisfied =
            currentHousehold.getConsumptionPerDay() - currentHousehold.getConsumedToday();
        if (this.constraintNetwork.isAdjacent(currentHousehold, firstTradingPartner) == true) {
          double previousConstraint =
              this.constraintNetwork.getEdge(currentHousehold, firstTradingPartner).getWeight();
          RepastEdge oldEdge =
              this.constraintNetwork.getEdge(currentHousehold, firstTradingPartner);
          this.constraintNetwork.removeEdge(oldEdge);
          this.constraintNetwork.addEdge(
              currentHousehold,
              firstTradingPartner,
              (double) amountUnsatisfied + previousConstraint);
        } else {
          this.constraintNetwork.addEdge(
              currentHousehold, firstTradingPartner, (double) amountUnsatisfied);
        }

        // Now, we go through with other trading partners with different bounds. First, until we hit
        // six firms visited, or until we have met 95 percent of the consumption goal
        int loweredConsumptionGoal = (int) (currentHousehold.getConsumptionPerDay() * 0.95d);
        int numberOfFirmsVisited = 0;
        for (int currentIndex = 1; currentIndex < tradingPartners.size(); currentIndex++) {
          while (currentHousehold.getConsumedToday() < loweredConsumptionGoal
              && numberOfFirmsVisited < 6
              && tradingPartners.get(currentIndex).canSell() == true
              && currentHousehold.getCash().intValue()
                  > tradingPartners.get(currentIndex).getPrice().getAmountMajorInt()) {
            transaction buyOneGood =
                new transaction(
                    currentHousehold,
                    firstTradingPartner.getPrice(),
                    1,
                    firstTradingPartner,
                    "consumption");
            currentHousehold.handleTransaction(buyOneGood);
            tradingPartners.get(currentIndex).handleTransaction(buyOneGood);
          }
          if (currentHousehold.getConsumedToday() < loweredConsumptionGoal
              && currentHousehold.getCash().intValue()
                  > tradingPartners.get(currentIndex).getPrice().getAmountMajorInt()) {
            amountUnsatisfied = loweredConsumptionGoal - currentHousehold.getConsumedToday();
            if (this.constraintNetwork.isAdjacent(
                    currentHousehold, tradingPartners.get(currentIndex))
                == true) {
              double previousConstraint =
                  this.constraintNetwork
                      .getEdge(currentHousehold, tradingPartners.get(currentIndex))
                      .getWeight();
              RepastEdge oldEdge =
                  this.constraintNetwork.getEdge(
                      currentHousehold, tradingPartners.get(currentIndex));
              this.constraintNetwork.removeEdge(oldEdge);
              this.constraintNetwork.addEdge(
                  currentHousehold,
                  tradingPartners.get(currentIndex),
                  (double) amountUnsatisfied + previousConstraint);
            } else {
              this.constraintNetwork.addEdge(
                  currentHousehold, tradingPartners.get(currentIndex), (double) amountUnsatisfied);
            }
            numberOfFirmsVisited++;
          }
        }
      }
    }
  }

  public void findBetterTradingPartners() {
    calcMarketShareByEmployees();
    for (HouseholdAgent currentHousehold : this.households) {
      // There is a certain percentage chance of trying to find a new partner, so check that first
      if (RandomHelper.nextIntFromTo(1, 100) <= this.percentChanceToFindNewPartner) {

        // Select a possible partner
        ConsumptionFirmAgent possibleNewPartner = findPossiblePartner(currentHousehold);

        // Select a random firm that the firm is currently trading with
        ConsumptionFirmAgent existingPartner =
            (ConsumptionFirmAgent) this.consumptionNetwork.getRandomAdjacent(currentHousehold);

        // Compare their prices
        Money priceOfNewPartner = possibleNewPartner.getPrice();
        Money priceOfOldPartner = existingPartner.getPrice();

        if (priceOfNewPartner
                .dividedBy(priceOfOldPartner.getAmount(), RoundingMode.HALF_DOWN)
                .getAmount()
                .doubleValue()
            <= this.percentThresholdForLowerPrices) {
          RepastEdge edgeToDelete = this.laborNetwork.getEdge(currentHousehold, existingPartner);
          this.laborNetwork.removeEdge(edgeToDelete);
          this.laborNetwork.addEdge(currentHousehold, possibleNewPartner);
        }
      }
    }
  }

  public void replaceConstrainedTradingPartners() {
    // First get all of the households that were constrained in the past month
    ArrayList<HouseholdAgent> constrainedHouseholds = new ArrayList<HouseholdAgent>();
    for (HouseholdAgent currentHousehold : this.households) {
      if (this.constraintNetwork.getDegree(currentHousehold) >= 1) {
        constrainedHouseholds.add(currentHousehold);
      }
    }
  }

  public ConsumptionFirmAgent findPossiblePartner(HouseholdAgent householdToUse) {
    // Collect the firms that this household is not trading with
    ArrayList<ConsumptionFirmAgent> firmsNotTradingWith = new ArrayList<ConsumptionFirmAgent>();
    for (ConsumptionFirmAgent currentFirm : this.consumptionFirms) {
      if (this.consumptionNetwork.isAdjacent(householdToUse, currentFirm) == false) {
        firmsNotTradingWith.add(currentFirm);
      }
    }

    // Sum up their market shares
    double sumOfShares = 0;
    for (ConsumptionFirmAgent currentFirm : firmsNotTradingWith) {
      sumOfShares = sumOfShares + currentFirm.getMarketShareByEmployees();
    }

    System.out.println("Market share for the firms not sharing a connection are: " + sumOfShares);

    // Generate a random number
    double probability = RandomHelper.nextDoubleFromTo(0, sumOfShares);
    System.out.println("Probability is: " + probability);
    double cumulativeProbability = 0.0;
    for (ConsumptionFirmAgent currentFirm : firmsNotTradingWith) {
      cumulativeProbability = cumulativeProbability + currentFirm.getMarketShareByEmployees();
      System.out.println("Cumulative probability is: " + cumulativeProbability);
      if (cumulativeProbability >= probability) {
        return currentFirm;
      }
    }

    return null;
  }

  public void calcMarketShareByEmployees() {
    // First, count the number of people employed in the economy
    int totalNumWorkers = 0;
    for (ConsumptionFirmAgent currentFirm : this.consumptionFirms) {
      totalNumWorkers = totalNumWorkers + this.laborNetwork.getDegree(currentFirm);
    }
    // Next, for each firm, get the amount of workers they have and divide that by the total.
    for (ConsumptionFirmAgent currentFirm : this.consumptionFirms) {
      double thisFirmsShare =
          ((double) this.laborNetwork.getDegree(currentFirm) / totalNumWorkers) * 100;
      currentFirm.setMarketShareByEmployees(thisFirmsShare);
    }
  }

  public void buildInitialConsumptionNetwork() {

    // Build a new arraylist that represents the potential partners that each household can pick
    // from. Initially, set it the contain all firms
    ArrayList<ConsumptionFirmAgent> potentialPartners = new ArrayList<ConsumptionFirmAgent>();
    for (ConsumptionFirmAgent curAgent : this.consumptionFirms) {
      potentialPartners.add(curAgent);
    }

    // This determines how many partners each household selects, should be a factor of the number of
    // firms. This is the amount of firms removed from the pool of potential partners when a
    // household finishes selecting their partners. A round of assignment is then done when all
    // partners have been removed, which takes a number of households equal to the number of firms
    // divided by the numberOfTradingPartnersPerHousehold
    int numberOfTradingPartnersPerHousehold = 5;

    // For every household, first make an arraylist that holds their trading partners then check if
    // the potential partners arraylist is empty. If it is then repopulate it with all of the firms
    // in the simulation. This is because if all the agents have been removed, then that means they
    // have all gone through one round of assignment
    for (HouseholdAgent currentHousehold : this.households) {
      ArrayList<ConsumptionFirmAgent> tradingPartners = new ArrayList<ConsumptionFirmAgent>();

      // Repopulate if empty
      if (potentialPartners.isEmpty()) {
        for (ConsumptionFirmAgent curAgent : this.consumptionFirms) {
          potentialPartners.add(curAgent);
        }
      }

      // Shuffler to ensure random access
      Collections.shuffle(potentialPartners);

      // Builds the trading partners by going through the firms and finding one that is not already
      // connected to the current household, when it does, it adds it to the trading partners
      // arraylist. It does this numberOfTradingPartnersPerHousehold times
      for (int currentFirm = 1, currentIndex = 0;
          currentFirm <= numberOfTradingPartnersPerHousehold;
          currentIndex++) {
        if (this.consumptionNetwork.isAdjacent(
                currentHousehold, potentialPartners.get(currentIndex))
            == false) {
          tradingPartners.add(potentialPartners.get(currentIndex));
          currentFirm++;
        }
      }

      // Now that we have the partners, go ahead and create the edges that determine who the
      // household can buy from
      for (ConsumptionFirmAgent currentTradingPartner : tradingPartners) {
        this.consumptionNetwork.addEdge(currentHousehold, currentTradingPartner);
      }

      // Remove the current households partners from the pool of potential partners for the next
      // household
      potentialPartners.removeAll(tradingPartners);
    }
  }

  public void buildInitialLaborNetwork() {

    // We want to assign a number of workers to each firm such that all firms have an equal amount
    // of workers and all households have jobs to start
    int workersPerFirm = this.numberOfHouseholds / this.numberOfFirms;

    // For every firm, iterate through the households and check if the current household has a job,
    // if it does not, connect it with an edge and increment the amount of workers for the firm
    for (ConsumptionFirmAgent currentFirm : this.consumptionFirms) {
      Collections.shuffle(this.households);
      for (int currentHousehold = 1, currentIndex = 0;
          currentHousehold <= workersPerFirm;
          currentIndex++) {
        if (this.laborNetwork.getDegree(this.households.get(currentIndex)) == 0) {
          this.laborNetwork.addEdge(currentFirm, this.households.get(currentIndex));
          currentFirm.increaseAmountOfWorkers(1);
          currentHousehold++;
        }
      }
    }
  }

  public void payHouseholds() {
    for (ConsumptionFirmAgent currentFirm : this.consumptionFirms) {

      // Get all the employees of this firm and save them to an array list
      Iterable<Object> currFirmsEmployeesIterable = this.laborNetwork.getAdjacent(currentFirm);
      ArrayList<HouseholdAgent> currFirmsEmployees = new ArrayList<HouseholdAgent>();
      for (Object currEmployee : currFirmsEmployeesIterable) {
        currFirmsEmployees.add((HouseholdAgent) currEmployee);
      }

      // First, pay every single household
      for (HouseholdAgent currentEmployee : currFirmsEmployees) {
        transaction wagePayment =
            new transaction(currentFirm, currentFirm.getOfferedWage(), 0, currentEmployee, "wage");

        currentEmployee.handleTransaction(wagePayment);
        currentFirm.handleTransaction(wagePayment);
      }

      // Next distribute the remaining amount of money, as long as each household gets atleast one
      // dollar, and the payoff is regular
      if (currentFirm
              .getDividends()
              .dividedBy(this.numberOfHouseholds, RoundingMode.HALF_DOWN)
              .isGreaterThan(Money.of(CurrencyUnit.of("USD"), 1.00d))
          && currentFirm.getPayOffDecision().equals("regular")) {

        Money individualDividend =
            currentFirm.getDividends().dividedBy(this.numberOfHouseholds, RoundingMode.HALF_DOWN);

        for (HouseholdAgent currentHousehold : this.households) {
          transaction dividendPayment =
              new transaction(currentFirm, individualDividend, 0, currentHousehold, "wage");
        }
      }
    }
  }

  public static void main(String[] args) {}
}
