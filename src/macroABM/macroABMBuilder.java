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
import java.util.Iterator;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import repast.simphony.context.Context;
import repast.simphony.context.space.continuous.ContinuousSpaceFactory;
import repast.simphony.context.space.continuous.ContinuousSpaceFactoryFinder;
import repast.simphony.context.space.graph.NetworkBuilder;
import repast.simphony.dataLoader.ContextBuilder;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ISchedule;
import repast.simphony.engine.schedule.ScheduleParameters;
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
  int numberOfHouseholds = 100;
  ArrayList<ConsumptionFirmAgent> consumptionFirms = new ArrayList<ConsumptionFirmAgent>();
  ArrayList<HouseholdAgent> households = new ArrayList<HouseholdAgent>();
  int percentChanceToFindNewPartner = 25;
  double percentThresholdForLowerPrices = 0.99;
  int percentChanceToReplaceConstrainedPartner = 25;
  static int consumeRuns = 0;
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
            500,
            500);

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

    // Scheduling
    RunEnvironment currentEnv = RunEnvironment.getInstance();
    ISchedule schedule = currentEnv.getCurrentSchedule();
    ScheduleParameters params = ScheduleParameters.createRepeating(1, 21, 100);
    schedule.schedule(params, this, "fireWorkers");
    params = ScheduleParameters.createRepeating(1, 21, 97);
    schedule.schedule(params, this, "findBetterTradingPartners");
    params = ScheduleParameters.createRepeating(1, 21, 96);
    schedule.schedule(params, this, "replaceConstrainedTradingPartners");
    params = ScheduleParameters.createRepeating(1, 21, 95);
    schedule.schedule(params, this, "changeEmployment");
    params = ScheduleParameters.createRepeating(1, 1, 93);
    schedule.schedule(params, this, "endOfDayCleanUp");
    params = ScheduleParameters.createRepeating(1, 1, 92);
    schedule.schedule(params, this, "consume");
    params = ScheduleParameters.createRepeating(21, 21, 89);
    schedule.schedule(params, this, "payHouseholds");
    params = ScheduleParameters.createRepeating(21, 21, 87);
    schedule.schedule(params, this, "endOfMonthCleanup");
    params = ScheduleParameters.createRepeating(1, 1, 86);
    schedule.schedule(params, this, "testForIncorrectValues");
    params = ScheduleParameters.createRepeating(21, 21, 85);
    schedule.schedule(params, this, "calcUnemploymentRate");
    params = ScheduleParameters.createOneTime(21000, 84);
    schedule.schedule(params, this, "calcAverageWage");

    return primaryContext;
  }

  public void calcAverageWage() {
    Money runningSum = Money.of(CurrencyUnit.of("USD"), 0.00);
    for (ConsumptionFirmAgent currentFirm : this.consumptionFirms) {
      runningSum = runningSum.plus(currentFirm.getOfferedWage());
    }

    Money averageWage =
        runningSum.dividedBy(new BigDecimal(this.numberOfFirms), RoundingMode.HALF_DOWN);
    Money newMinimumWage = averageWage.multipliedBy(new BigDecimal(0.90), RoundingMode.HALF_DOWN);
    ConsumptionFirmAgent.setMinWage(newMinimumWage);
    System.out.println("Minimum wage set at: " + newMinimumWage.toString());
  }

  public int calcUnemploymentRate() {
    int numberEmployed = 0;
    for (HouseholdAgent currentHousehold : this.households) {
      if (this.laborNetwork.getDegree(currentHousehold) == 1) {
        numberEmployed++;
      }
    }
    System.out.println("Employment is: " + numberEmployed);
    return numberEmployed;
  }

  public int testOutput() {
    return RandomHelper.nextIntFromTo(1, 10);
  }

  public void testForIncorrectValues() {
    System.out.println("Finding errors");
    for (HouseholdAgent currentHousehold : this.households) {
      if (this.laborNetwork.getDegree(currentHousehold) > 1) {
        System.out.println("Too many employers for " + currentHousehold);
        System.out.println("Currently has " + this.laborNetwork.getDegree(currentHousehold));
        System.exit(0);
      }
      if (this.consumptionNetwork.getDegree(currentHousehold) > 7) {
        System.out.println("Too many trading partners");
        System.out.println("Currently has " + this.constraintNetwork.getDegree(currentHousehold));
        System.exit(0);
      }
      if (currentHousehold.getCash().isLessThan(Money.of(CurrencyUnit.of("USD"), -100.00))) {
        System.out.println(
            currentHousehold + " has negative cash: " + currentHousehold.getCash().toString());
        System.exit(0);
      }
    }
    for (ConsumptionFirmAgent currentFirm : this.consumptionFirms) {
      if (currentFirm.getInventory() < 0) {
        System.out.println(currentFirm + " has negative inventory");
        System.exit(0);
      }
      if (currentFirm.getCash().isLessThan(Money.of(CurrencyUnit.of("USD"), -100.00))) {
        System.out.println(currentFirm + " has negative cash: " + currentFirm.getCash().toString());
        System.exit(0);
      }
      if (currentFirm.getAmountOfWorkers().intValue() < 0) {
        System.out.println(
            currentFirm + " has negative amount of workers " + currentFirm.getAmountOfWorkers());
        System.out.println(currentFirm + " has degree " + this.laborNetwork.getDegree(currentFirm));
        System.exit(0);
      }
      if (currentFirm.getAmountOfWorkers().intValue() != this.laborNetwork.getDegree(currentFirm)) {
        System.out.println(currentFirm + " has mismatch in worker amount");
        System.out.println(currentFirm.getAmountOfWorkers());
        System.out.println("Actual amount is " + this.laborNetwork.getDegree(currentFirm));
        System.exit(0);
      }
    }
  }

  public void endOfDayCleanUp() {
    for (HouseholdAgent currentHousehold : this.households) {
      currentHousehold.resetDailyConsumption();
    }
  }

  public void consume() {
    // Go through every single household
    //    System.out.println("IN CONSUME");
    //    System.out.println("CONSUME HAS RAN: " + this.consumeRuns);
    consumeRuns++;
    for (HouseholdAgent currentHousehold : this.households) {
      //      System.out.println(
      //          currentHousehold
      //              + " has "
      //              + this.consumptionNetwork.getDegree(currentHousehold)
      //              + " trading partners");
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

      //      System.out.println(
      //          this
      //              + " has consumed "
      //              + currentHousehold.getConsumedToday()
      //              + " out of "
      //              + currentHousehold.getConsumptionPerDay());
      //      System.out.println(firstTradingPartner + " can sell? " +
      // firstTradingPartner.canSell());
      //      System.out.println(
      //          "Can this household afford a good? "
      //              + (currentHousehold.getCash().isGreaterThan(firstTradingPartner.getPrice())));

      while ((currentHousehold.getConsumedToday() < currentHousehold.getConsumptionPerDay())
          && firstTradingPartner.canSell() == true
          && currentHousehold.getCash().isGreaterThan(firstTradingPartner.getPrice())) {
        transaction buyOneGood =
            new transaction(
                currentHousehold,
                firstTradingPartner.getPrice(),
                1,
                firstTradingPartner,
                "consumption");
        currentHousehold.handleTransaction(buyOneGood);
        firstTradingPartner.handleTransaction(buyOneGood);
        //        System.out.println(
        //            currentHousehold
        //                + " bought one good from "
        //                + firstTradingPartner
        //                + ". Partner now has "
        //                + firstTradingPartner.getInventory()
        //                + " inventories");
        //        System.out.println(
        //            currentHousehold
        //                + " has consumed "
        //                + currentHousehold.getConsumedToday()
        //                + " out of "
        //                + currentHousehold.getConsumptionPerDay());
      }

      // Check if we ended because we could not consume all we needed, and could still afford to
      if (currentHousehold.getConsumedToday() < currentHousehold.getConsumptionPerDay()
          && currentHousehold.getCash().isGreaterThan(firstTradingPartner.getPrice())) {

        // If we did, then we know we were demand constrained, so add the edge with the weight
        // representing the amount we were unsatisfied
        int amountUnsatisfied =
            currentHousehold.getConsumptionPerDay() - currentHousehold.getConsumedToday();

        //        System.out.println(currentHousehold + " not satisfied by " + amountUnsatisfied);

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

        //        System.out.println(
        //            "Constraint network worked?"
        //                + this.constraintNetwork.isAdjacent(currentHousehold,
        // firstTradingPartner));

        // Now, we go through with other trading partners with different bounds. First, until we hit
        // six firms visited, or until we have met 95 percent of the consumption goal
        int loweredConsumptionGoal = (int) (currentHousehold.getConsumptionPerDay() * 0.95d);
        int numberOfFirmsVisited = 0;
        for (int currentIndex = 1; currentIndex < tradingPartners.size(); currentIndex++) {
          //          System.out.println("Trying index" + currentIndex);
          while (currentHousehold.getConsumedToday() < loweredConsumptionGoal
              && numberOfFirmsVisited < 6
              && tradingPartners.get(currentIndex).canSell() == true
              && currentHousehold.getCash().isGreaterThan(firstTradingPartner.getPrice())) {
            transaction buyOneGood =
                new transaction(
                    currentHousehold,
                    firstTradingPartner.getPrice(),
                    1,
                    tradingPartners.get(currentIndex),
                    "consumption");
            currentHousehold.handleTransaction(buyOneGood);
            tradingPartners.get(currentIndex).handleTransaction(buyOneGood);
            //            System.out.println(
            //                currentHousehold
            //                    + " bought one good from "
            //                    + tradingPartners.get(currentIndex)
            //                    + ". Partner now has "
            //                    + tradingPartners.get(currentIndex).getInventory()
            //                    + " inventories");
            //            System.out.println(
            //                currentHousehold
            //                    + " has consumed "
            //                    + currentHousehold.getConsumedToday()
            //                    + " out of "
            //                    + loweredConsumptionGoal);
            //            System.out.println(
            //                currentHousehold + " has visited " + numberOfFirmsVisited + " firms");
          }
          if (currentHousehold.getConsumedToday() < loweredConsumptionGoal
              && currentHousehold.getCash().isGreaterThan(firstTradingPartner.getPrice())) {
            amountUnsatisfied = loweredConsumptionGoal - currentHousehold.getConsumedToday();
            //            System.out.println(currentHousehold + " not satisfied by " +
            // amountUnsatisfied);
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
            //            System.out.println(
            //                "Constraint network worked?"
            //                    + this.constraintNetwork.isAdjacent(
            //                        currentHousehold, tradingPartners.get(currentIndex)));
            numberOfFirmsVisited++;
          }
        }
      }
    }
  }

  public void fireWorkers() {

    // Collect all the firms that want to fire a worker
    ArrayList<ConsumptionFirmAgent> firmsThatAreFiring = new ArrayList<ConsumptionFirmAgent>();

    for (ConsumptionFirmAgent currentFirm : this.consumptionFirms) {
      if (currentFirm.getFiringDecision() == true
          && currentFirm.getAmountOfWorkers().intValue() > 0) {
        firmsThatAreFiring.add(currentFirm);
      }
    }

    System.out.println(
        "There are: " + firmsThatAreFiring.size() + " firms that want to fire workers");

    for (ConsumptionFirmAgent currentFirm : firmsThatAreFiring) {
      // Select a random firm from those that are adjacent in the labor network
      HouseholdAgent householdToBeFired =
          (HouseholdAgent) this.laborNetwork.getRandomAdjacent(currentFirm);
      RepastEdge edgeToDelete = this.laborNetwork.getEdge(currentFirm, householdToBeFired);
      this.laborNetwork.removeEdge(edgeToDelete);
      currentFirm.decreaseAmountOfWorkers(1);
      System.out.println(
          currentFirm
              + " just fired "
              + householdToBeFired
              + " and now has "
              + this.laborNetwork.getDegree(currentFirm)
              + " connections and "
              + currentFirm.getAmountOfWorkers()
              + " workers");
      System.out.println(
          householdToBeFired
              + " now has degree "
              + this.laborNetwork.getDegree(householdToBeFired));
    }
  }

  public void endOfMonthCleanup() {

    // Firm hiring decisions have not been updated yet, so set the values for next months variables
    for (ConsumptionFirmAgent currentFirm : this.consumptionFirms) {
      if (currentFirm.getHiringDecision() == true) {
        currentFirm.setWasPositionOfferedLastPeriod(true);
      } else {
        currentFirm.setWasPositionFilledLastPeriod(false);
      }
    }
  }

  public void findBetterTradingPartners() {
    calcMarketShareByEmployees();
    for (HouseholdAgent currentHousehold : this.households) {
      // There is a certain percentage chance of trying to find a new partner, so check that first
      if (RandomHelper.nextIntFromTo(1, 100) <= this.percentChanceToFindNewPartner
          && this.consumptionNetwork.getDegree(currentHousehold) < 7) {

        // Select a possible partner
        ConsumptionFirmAgent possibleNewPartner = findPossiblePartner(currentHousehold);
        System.out.println(
            "Possible new partner is: "
                + possibleNewPartner
                + "which has MS of "
                + possibleNewPartner.getMarketShareByEmployees());

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
          RepastEdge edgeToDelete =
              this.consumptionNetwork.getEdge(currentHousehold, existingPartner);
          System.out.println(
              "For "
                  + this
                  + " old partner had price "
                  + existingPartner.getPrice().toString()
                  + " new partner has price "
                  + possibleNewPartner.getPrice().toString());
          this.consumptionNetwork.removeEdge(edgeToDelete);
          this.consumptionNetwork.addEdge(currentHousehold, possibleNewPartner);
          System.out.println(
              "Confirming that "
                  + this
                  + " is no longer adjacent to old partner: "
                  + !this.laborNetwork.isAdjacent(currentHousehold, existingPartner));
        }
      }
    }
  }

  public void changeEmployment() {
    // This will be done for every household
    for (HouseholdAgent currentHousehold : this.households) {
      // If the household is currently unemployed...
      if (this.laborNetwork.getDegree(currentHousehold) == 0) {
        // This search can only happen a number of times, or until a position is found
        System.out.println(currentHousehold + " is looking for a job");
        for (int currentSearchIteration = 1;
            currentSearchIteration <= 5;
            currentSearchIteration++) {
          // Select a random firm
          ConsumptionFirmAgent potentialEmployer =
              this.consumptionFirms.get(
                  RandomHelper.nextIntFromTo(0, this.consumptionFirms.size() - 1));
          // Check if the firm is hiring and if the wage offered is higher than our reservationWage
          System.out.println(
              potentialEmployer + " is paying " + potentialEmployer.getOfferedWage().toString());
          System.out.println(
              currentHousehold
                  + " wants atleast "
                  + currentHousehold.getReservationWage().toString());
          if (potentialEmployer.getHiringDecision() == true
              && (potentialEmployer
                      .getOfferedWage()
                      .isGreaterThan(currentHousehold.getReservationWage())
                  || potentialEmployer
                      .getOfferedWage()
                      .isEqual(currentHousehold.getReservationWage()))) {
            this.laborNetwork.addEdge(potentialEmployer, currentHousehold);
            potentialEmployer.increaseAmountOfWorkers(1);
            potentialEmployer.setWasPositionFilledLastPeriod(true);
            System.out.println(currentHousehold + " found a job with " + potentialEmployer);
            break;
          }
        }
      }

      // If the household is currently employed...
      else if (this.laborNetwork.getDegree(currentHousehold) == 1) {
        ConsumptionFirmAgent employer = getEmployer(currentHousehold);
        System.out.println("Employer was: " + employer);
        System.out.println("Reservation wage is: " + currentHousehold.getReservationWage());
        // and the reservation wage is less than the employers offered wage rate...
        if (currentHousehold.getReservationWage().isLessThan(employer.getOfferedWage())) {
          ConsumptionFirmAgent possibleNewEmployer =
              this.consumptionFirms.get(
                  RandomHelper.nextIntFromTo(0, this.consumptionFirms.size() - 1));
          if (this.laborNetwork.isAdjacent(currentHousehold, possibleNewEmployer) == false
              && possibleNewEmployer.getOfferedWage().isGreaterThan(employer.getOfferedWage())) {
            RepastEdge edgeToDelete = this.laborNetwork.getEdge(employer, currentHousehold);
            this.laborNetwork.removeEdge(edgeToDelete);
            employer.decreaseAmountOfWorkers(1);
            System.out.println(
                currentHousehold
                    + " is still connected to old employer?"
                    + this.laborNetwork.isAdjacent(employer, currentHousehold));
            this.laborNetwork.addEdge(possibleNewEmployer, currentHousehold);
            possibleNewEmployer.increaseAmountOfWorkers(1);
            possibleNewEmployer.setWasPositionFilledLastPeriod(true);
          }
        }

        // and the reservation wage is equal to or less than the employers offered wage rate...
        else if (currentHousehold.getReservationWage().isEqual(employer.getOfferedWage()) == true
            || currentHousehold.getReservationWage().isLessThan(employer.getOfferedWage())
                == true) {
          if (RandomHelper.nextIntFromTo(1, 100) <= 10) {
            System.out.println(
                "Trying to find new partner even though reservation wage is high enough");
            ConsumptionFirmAgent possibleNewEmployer =
                this.consumptionFirms.get(
                    RandomHelper.nextIntFromTo(0, this.consumptionFirms.size() - 1));
            if (this.laborNetwork.isAdjacent(possibleNewEmployer, currentHousehold) == false
                && possibleNewEmployer.getOfferedWage().isGreaterThan(employer.getOfferedWage())) {
              System.out.println("New employer found");
              RepastEdge edgeToDelete = this.laborNetwork.getEdge(employer, currentHousehold);
              this.laborNetwork.removeEdge(edgeToDelete);
              employer.decreaseAmountOfWorkers(1);
              System.out.println(
                  currentHousehold
                      + " is still connected to old employer?"
                      + this.laborNetwork.isAdjacent(employer, currentHousehold));
              this.laborNetwork.addEdge(possibleNewEmployer, currentHousehold);
              possibleNewEmployer.increaseAmountOfWorkers(1);
              possibleNewEmployer.setWasPositionFilledLastPeriod(true);
            }
          }
        }
      }
    }
  }

  public ConsumptionFirmAgent getEmployer(HouseholdAgent employee) {
    Iterator<Object> employerIterator = this.laborNetwork.getAdjacent(employee).iterator();
    return (ConsumptionFirmAgent) employerIterator.next();
  }

  public void replaceConstrainedTradingPartners() {
    // First get all of the households that were constrained in the past month
    ArrayList<HouseholdAgent> constrainedHouseholds = new ArrayList<HouseholdAgent>();

    System.out.println("Going in there is a total degree of " + this.constraintNetwork.getDegree());

    for (HouseholdAgent currentHousehold : this.households) {
      if (this.constraintNetwork.getDegree(currentHousehold) >= 1) {
        constrainedHouseholds.add(currentHousehold);
      }
    }

    System.out.println("There were " + constrainedHouseholds.size() + " constrained households");

    for (HouseholdAgent currentHousehold : constrainedHouseholds) {
      // For every household, only do the replacement with a given probability
      if (RandomHelper.nextIntFromTo(1, 100) <= this.percentChanceToReplaceConstrainedPartner
          && this.consumptionNetwork.getDegree(currentHousehold) < 7) {

        // Select a random partner from the constrained partners based on the size of the constraint
        ConsumptionFirmAgent constrainedPartner = findRandomConstrainedPartner(currentHousehold);
        System.out.println("The selected constraining partner was " + constrainedPartner);

        ConsumptionFirmAgent replacementPartner;
        // Select a random firm that this is not already connected with
        do {
          replacementPartner =
              this.consumptionFirms.get(
                  RandomHelper.nextIntFromTo(0, this.consumptionFirms.size() - 1));
        } while (this.consumptionNetwork.isAdjacent(currentHousehold, replacementPartner) == true);
        System.out.println("The selected random partner was " + replacementPartner);

        // Delete the edge with the last partner
        RepastEdge edgeToDelete =
            this.consumptionNetwork.getEdge(currentHousehold, constrainedPartner);
        this.consumptionNetwork.removeEdge(edgeToDelete);

        // Add a new edge
        this.consumptionNetwork.addEdge(currentHousehold, replacementPartner);
      }
    }
    this.constraintNetwork.removeEdges();
  }

  public ConsumptionFirmAgent findRandomConstrainedPartner(HouseholdAgent householdToUse) {
    // Get all of the constrained partners into an arrayList
    Iterable<Object> constrainedPartnersIterable =
        this.constraintNetwork.getAdjacent(householdToUse);
    ArrayList<ConsumptionFirmAgent> constrainedPartners = new ArrayList<ConsumptionFirmAgent>();
    for (Object partner : constrainedPartnersIterable) {
      constrainedPartners.add((ConsumptionFirmAgent) partner);
    }

    // Next, find the total amount the firms constrained demand by
    double totalConstraint = 0;
    for (ConsumptionFirmAgent currentPartner : constrainedPartners) {
      totalConstraint =
          totalConstraint
              + this.constraintNetwork.getEdge(householdToUse, currentPartner).getWeight();
    }

    // Select a random number from 1 to the total constraint
    double probability = RandomHelper.nextDoubleFromTo(1, totalConstraint);
    double cumulativeProbability = 0;

    for (ConsumptionFirmAgent currentFirm : constrainedPartners) {
      cumulativeProbability =
          cumulativeProbability
              + this.constraintNetwork.getEdge(householdToUse, currentFirm).getWeight();
      if (cumulativeProbability >= probability) {
        return currentFirm;
      }
    }
    return null;
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

    System.out.println(
        "Market share for the firms not sharing a connection with "
            + householdToUse
            + "are: "
            + sumOfShares);

    // Generate a random number
    double probability = RandomHelper.nextDoubleFromTo(0, sumOfShares);
    double cumulativeProbability = 0.0;
    for (ConsumptionFirmAgent currentFirm : firmsNotTradingWith) {
      cumulativeProbability = cumulativeProbability + currentFirm.getMarketShareByEmployees();
      if (cumulativeProbability >= probability) {
        return currentFirm;
      }
    }

    return this.consumptionFirms.get(
        RandomHelper.nextIntFromTo(0, this.consumptionFirms.size() - 1));
  }

  public void calcMarketShareByEmployees() {
    // First, count the number of people employed in the economy
    int totalNumWorkers = 0;
    for (ConsumptionFirmAgent currentFirm : this.consumptionFirms) {
      totalNumWorkers = totalNumWorkers + this.laborNetwork.getDegree(currentFirm);
    }
    // Next, for each firm, get the amount of workers they have and divide that by the total.
    if (totalNumWorkers > 0) {
      for (ConsumptionFirmAgent currentFirm : this.consumptionFirms) {
        double thisFirmsShare =
            ((double) this.laborNetwork.getDegree(currentFirm) / totalNumWorkers) * 100;
        currentFirm.setMarketShareByEmployees(thisFirmsShare);
      }
    } else {
      for (ConsumptionFirmAgent currentFirm : this.consumptionFirms) {
        currentFirm.setMarketShareByEmployees(0);
      }
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

      System.out.println(currentFirm + " employs " + currFirmsEmployees.size());

      // First, pay every single household
      for (HouseholdAgent currentEmployee : currFirmsEmployees) {
        transaction wagePayment =
            new transaction(currentFirm, currentFirm.getOfferedWage(), 0, currentEmployee, "wage");

        currentEmployee.handleTransaction(wagePayment);
        currentFirm.handleTransaction(wagePayment);
      }

      System.out.println(
          "After paying wage " + currentFirm + " has " + currentFirm.getCash().toString());

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
          currentHousehold.handleTransaction(dividendPayment);
          currentFirm.handleTransaction(dividendPayment);
        }

        System.out.println(
            "After paying dividends " + currentFirm + " has " + currentFirm.getCash().toString());
      }
      if (currentFirm.getPayOffDecision().equals("noworkers")) {
        System.out.println(currentFirm + "has no workers");
      } else if (currentFirm.getPayOffDecision().equals("firingworkers")) {

        int amountToFire = currentFirm.getNumWorkersToFireForMinWage();
        ArrayList<HouseholdAgent> householdsToFire = new ArrayList<HouseholdAgent>();
        for (int count = 0; count < amountToFire; count++) {
          HouseholdAgent currentHouseholdToFire =
              (HouseholdAgent) this.laborNetwork.getRandomAdjacent(currentFirm);
          householdsToFire.add(currentHouseholdToFire);
        }

        for (HouseholdAgent currentHousehold : householdsToFire) {
          RepastEdge edgeToDelete = this.laborNetwork.getEdge(currentFirm, currentHousehold);
          this.laborNetwork.removeEdge(edgeToDelete);
          currentFirm.decreaseAmountOfWorkers(1);
        }
      }
    }
  }

  public static void main(String[] args) {}
}
