package macroABM;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import junitparams.JUnitParamsRunner;

@RunWith(JUnitParamsRunner.class)
public class TestEconomicAgent {

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {}

  @AfterClass
  public static void tearDownAfterClass() throws Exception {}

  @Before
  public void setUp() throws Exception {}

  @After
  public void tearDown() throws Exception {}

  /*
  @Test
  public void testEconomicAgentDefaultConstructor() {
    HouseholdAgent testingAgent = new HouseholdAgent();
    BigDecimal expectedCash = new BigDecimal("0.00");
    assertEquals(
        "Default Constructor of EconomicAgent does not work", testingAgent.getCash(), expectedCash);
  }

  @Test
  @Parameters(method = "paramsTestEconomicAgentConstructor")
  public void testEconomicAgentConstructor(HouseholdAgent testAgent, BigDecimal expectedCash) {
    assertEquals("Constructor of EconomicAgent does not work", testAgent.getCash(), expectedCash);
  }

  @Test
  @Parameters(method = "paramsTestCalculateExpectedVariable")
  public void testCalculateExpectedVariable(
      Money testCalculatedVariable, BigDecimal expectedValue) {
    assertEquals(
        "calculateExpectedVariable did not work",
        testCalculatedVariable.getAmount(),
        expectedValue);
  }

  private Object[] paramsTestEconomicAgentConstructor() {
    return new Object[] {
      new Object[] {new HouseholdAgent(100.0d), new BigDecimal("100.00")},
      new Object[] {new HouseholdAgent(150.0d), new BigDecimal("150.00")}
    };
  }

  private Object[] paramsTestCalculateExpectedVariable() {
    return new Object[] {
      new Object[] {
        EconomicAgent.calculateExpectedVariable(
            Money.of(CurrencyUnit.of("USD"), 150.00), Money.of(CurrencyUnit.of("USD"), 160.00)),
        new BigDecimal("152.50")
      },
      new Object[] {
        EconomicAgent.calculateExpectedVariable(
            Money.of(CurrencyUnit.of("USD"), 160.00), Money.of(CurrencyUnit.of("USD"), 150.00)),
        new BigDecimal("157.50")
      }
    };
  }
  */
}
