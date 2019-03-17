package macroABM;

import static org.junit.Assert.assertEquals;
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
      ConsumptionFirmAgent testAgent, boolean testVar1, int testVar2, boolean expected) {
    Money previousWage = testAgent.getOfferedWage();
    testAgent.updateWage(testVar1, testVar2);
    Money newWage = testAgent.getOfferedWage();
    boolean didWageIncrease = newWage.isGreaterThan(previousWage);
    assertEquals(didWageIncrease, expected);
  }

  private Object[] paramsTestUpdateWage() {
    return new Object[] {
      new Object[] {new ConsumptionFirmAgent(), true, 10, false},
      new Object[] {new ConsumptionFirmAgent(), false, 10, true}
    };
  }
}
