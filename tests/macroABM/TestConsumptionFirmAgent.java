package macroABM;

import static org.junit.Assert.assertEquals;
import java.math.BigDecimal;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;

@RunWith(JUnitParamsRunner.class)
public class TestConsumptionFirmAgent {

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {}

  @AfterClass
  public static void tearDownAfterClass() throws Exception {}

  @Before
  public void setUp() throws Exception {}

  @After
  public void tearDown() throws Exception {}

  @Test
  @Parameters(method = "paramsTestUpdateWage")
  public void testUpdateWage(
      ConsumptionFirmAgent testAgent,
      boolean testWasPositionOfferedLastPeriod,
      boolean testWasPositionFilledLastPeriod,
      int testMonthsAllPositionsFilled,
      boolean expectedOutput) {
    Money previousWage = testAgent.getOfferedWage();
    testAgent.updateWage(
        testWasPositionOfferedLastPeriod,
        testWasPositionFilledLastPeriod,
        testMonthsAllPositionsFilled);
    Money newWage = testAgent.getOfferedWage();
    boolean didWageIncrease = newWage.isGreaterThan(previousWage);
    assertEquals(didWageIncrease, expectedOutput);
  }

  private Object[] paramsTestUpdateWage() {
    return new Object[] {
      new Object[] {new ConsumptionFirmAgent(), true, true, 10, false},
      new Object[] {new ConsumptionFirmAgent(), true, false, 10, true}
    };
  }

  @Test
  @Parameters(method = "paramsTestUpdateHiringDecision")
  public void testUpdateHiringDecision(
      ConsumptionFirmAgent testAgent,
      BigDecimal testFloor,
      BigDecimal testCeil,
      boolean expectedHiring,
      boolean expectedFiring) {

    testAgent.updateHiringDecision(testFloor, testCeil);
    boolean agentHiringDecision = testAgent.getHiringDecision();
    boolean agentFiringDecision = testAgent.getFiringDecision();

    assertEquals(agentHiringDecision, expectedHiring);
    assertEquals(agentFiringDecision, expectedFiring);
  }

  private Object[] paramsTestUpdateHiringDecision() {
    return new Object[] {
      new Object[] {
        new ConsumptionFirmAgent(100.d, 10),
        new BigDecimal("5.0"),
        new BigDecimal("10.0"),
        false,
        false
      },
      new Object[] {
        new ConsumptionFirmAgent(100.d, 10),
        new BigDecimal("5.0"),
        new BigDecimal("8.0"),
        false,
        true
      },
      new Object[] {
        new ConsumptionFirmAgent(100.d, 10),
        new BigDecimal("15.0"),
        new BigDecimal("20.0"),
        true,
        false
      }
    };
  }

  @Test
  @Parameters(method = "paramsTestCalcInvBounds")
  public void testCalcInvBounds(
      ConsumptionFirmAgent testAgent,
      BigDecimal testSales,
      BigDecimal expectedCeil,
      BigDecimal expectedFloor) {
    testAgent.calcInvBounds(testSales);
    BigDecimal outputCeil = testAgent.getInvCiel();
    BigDecimal outputFloor = testAgent.getInvFloor();
    System.out.println(outputCeil);
    assertEquals(outputCeil, expectedCeil);
    assertEquals(outputFloor, expectedFloor);
  }

  private Object[] paramsTestCalcInvBounds() {
    return new Object[] {
      new Object[] {
        new ConsumptionFirmAgent(),
        new BigDecimal("10.00"),
        new BigDecimal("10.00"),
        new BigDecimal("2.50")
      }
    };
  }

  @Test
  @Parameters(method = "paramsTestUpdatePrice")
  public void testUpdatePrice(
      ConsumptionFirmAgent testAgent,
      BigDecimal testGivenInventoryFloor,
      BigDecimal testGivenInventoryCeiling,
      Money testGivenPriceFloor,
      Money testGivenPriceCeiling,
      boolean testExpectedPriceIncrease) {

    Money oldPrice = testAgent.getPrice();

    testAgent.updatePrice(
        testGivenInventoryFloor,
        testGivenInventoryCeiling,
        testGivenPriceFloor,
        testGivenPriceCeiling);

    Money newPrice = testAgent.getPrice();

    boolean wasPriceIncreased = newPrice.isGreaterThan(oldPrice);

    assertEquals(wasPriceIncreased, testExpectedPriceIncrease);
  }

  private Object[] paramsTestUpdatePrice() {
    return new Object[] {
      new Object[] {
        new ConsumptionFirmAgent(100.0d, 10),
        new BigDecimal("10.00"),
        new BigDecimal("20.00"),
        Money.of(CurrencyUnit.of("USD"), 5.0d),
        Money.of(CurrencyUnit.of("USD"), 6.0d),
        true
      }
    };
  }

  @Test
  @Parameters(method = "paramsTestCalcCostPerUnitProduced")
  public void testCalcCostPerUnitProduced(
      ConsumptionFirmAgent testAgent,
      Money testGivenOfferedWage,
      BigDecimal testGivenAmountOfWorkers,
      BigDecimal testGivenTechProductionCoeff,
      Money expectedCostPerUnitProduced) {

    testAgent.calcCostPerUnitProduced(
        testGivenOfferedWage, testGivenAmountOfWorkers, testGivenTechProductionCoeff);

    Money outputCostPerUnitProduced = testAgent.getCostPerUnitProduced();
    assertEquals(outputCostPerUnitProduced, expectedCostPerUnitProduced);
  }

  private Object[] paramsTestCalcCostPerUnitProduced() {
    return new Object[] {
      new Object[] {
        new ConsumptionFirmAgent(),
        Money.of(CurrencyUnit.of("USD"), 1120.00d),
        new BigDecimal("10.00"),
        new BigDecimal("3.00"),
        Money.of(CurrencyUnit.of("USD"), 17.78d)
      }
    };
  }

  @Test
  @Parameters(method = "paramsTestCalcPriceBounds")
  public void testCalcPriceBounds(
      ConsumptionFirmAgent testAgent,
      Money testGivenCostPerUnitProduced,
      Money expectedPriceFloor,
      Money expectedPriceCeiling) {
    testAgent.calcPriceBounds(testGivenCostPerUnitProduced);
    Money outputPriceFloor = testAgent.getPriceFloor();
    Money outputPriceCeiling = testAgent.getPriceCeiling();
    assertEquals(outputPriceFloor, expectedPriceFloor);
    assertEquals(outputPriceCeiling, expectedPriceCeiling);
  }

  private Object[] paramsTestCalcPriceBounds() {
    return new Object[] {
      new Object[] {
        new ConsumptionFirmAgent(),
        Money.of(CurrencyUnit.of("USD"), 17.78d),
        Money.of(CurrencyUnit.of("USD"), 18.22d),
        Money.of(CurrencyUnit.of("USD"), 20.45)
      }
    };
  }

  @Test
  @Parameters(method = "paramsTestProduceGoods")
  public void testProduceGoods(
      ConsumptionFirmAgent testAgent,
      BigDecimal testGivenAmountOfWorkers,
      BigDecimal testGivenTechProductionCoeff,
      int expectedLevelOfInventories) {

    testAgent.produceGoods(testGivenAmountOfWorkers, testGivenTechProductionCoeff);
    int outputInventories = testAgent.getInventory();
    assertEquals(outputInventories, expectedLevelOfInventories);
  }

  private Object[] paramsTestProduceGoods() {
    return new Object[] {
      new Object[] {
        new ConsumptionFirmAgent(100.00d, 10), new BigDecimal("10.00"), new BigDecimal("3.00"), 40
      }
    };
  }
}
