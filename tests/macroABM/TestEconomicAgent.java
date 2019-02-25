package macroABM;

import static org.junit.Assert.*;

import java.math.BigDecimal;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestEconomicAgent {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testEconomicAgent() {
		HouseholdAgent testingAgent = new HouseholdAgent();
		BigDecimal testDecimal = new BigDecimal("0.00");
		assertEquals("Default Constructor of EconomicAgent works", testingAgent.getCash().getAmount(), testDecimal);
	}

	@Test
	public void testEconomicAgentDoubleDoubleDouble() {
		fail("Not yet implemented");
	}

	@Test
	public void testCalculateExpectedVariable() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetCash() {
		fail("Not yet implemented");
	}

}
