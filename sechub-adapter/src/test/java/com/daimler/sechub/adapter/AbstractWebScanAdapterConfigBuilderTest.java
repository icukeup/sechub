// SPDX-License-Identifier: MIT
package com.daimler.sechub.adapter;

import static org.junit.Assert.*;

import java.util.Iterator;
import java.util.List;

import org.junit.Test;

public class AbstractWebScanAdapterConfigBuilderTest {

	@Test
	public void login_basic() {
		/* @formatter:off */
		TestWebScanAdapterConfig webScanConfig = new TestAbstractWebScanAdapterConfigBuilder().
				login().
					basic().
						username("user1").
						password("passwd1").
						realm("realm1").
				endLogin()
				.build();
		/* @formatter:on */

		/* test */
		LoginConfig config = webScanConfig.getLoginConfig();
		assertNotNull(config);
		assertTrue(null, config.isBasic());
		assertEquals("user1",config.asBasic().getUser());
		assertEquals("passwd1",config.asBasic().getPassword());
		assertEquals("realm1",config.asBasic().getRealm());
	}

	@Test
	public void login_form_automated() {
		/* @formatter:off */
		TestWebScanAdapterConfig x = new TestAbstractWebScanAdapterConfigBuilder().
				login().
					form().autoDetect().
						username("user1").
						password("passwd1").
				endLogin()
				.build();
		/* @formatter:on */

		/* test */
		assertNotNull(x);
		LoginConfig config = x.getLoginConfig();
		assertNotNull(config);
		assertTrue(null, config.isFormAutoDetect());
		assertEquals("user1",config.asFormAutoDetect().getUser());
		assertEquals("passwd1",config.asFormAutoDetect().getPassword());
	}

	@Test
	public void login_form_scripted() {
		/* @formatter:off */
		TestWebScanAdapterConfig x = new TestAbstractWebScanAdapterConfigBuilder().
				login().
					form().script().
					    addStep("input").select("#user_id").enterValue("user1").endStep().
					    addStep("input").select("#pwd_id").enterValue("pwd1").endStep().
					    addStep("click").select("#login_butotn_id").enterValue(null).endStep().
				endLogin()
				.build();
		/* @formatter:on */

		/* test */
		assertNotNull(x);
		LoginConfig config = x.getLoginConfig();
		assertNotNull(config);
		assertTrue(null, config.isFormScript());
		List<LoginScriptStep> steps = config.asFormScript().getSteps();
		assertNotNull(steps);
		assertEquals(3,steps.size());

		Iterator<LoginScriptStep> it = steps.iterator();
		LoginScriptStep step = it.next();

		assertTrue(step.isInput());
		assertFalse(step.isClick());
		assertEquals("user1",step.getValue());
		assertEquals("#user_id",step.getSelector());

		step = it.next();
		assertTrue(step.isInput());
		assertFalse(step.isClick());
		assertEquals("pwd1",step.getValue());
		assertEquals("#pwd_id",step.getSelector());

		step = it.next();
		assertFalse(step.isInput());
		assertTrue(step.isClick());
		assertEquals("#login_butotn_id",step.getSelector());
		assertEquals(null,step.getValue());
	}



	private class TestAbstractWebScanAdapterConfigBuilder extends
			AbstractWebScanAdapterConfigBuilder<TestAbstractWebScanAdapterConfigBuilder, TestWebScanAdapterConfig> {

		@Override
		protected void customBuild(TestWebScanAdapterConfig config) {

		}


		@Override
		protected TestWebScanAdapterConfig buildInitialConfig() {
			return new TestWebScanAdapterConfig();
		}

		@Override
		protected void customValidate() {

		}

	}

	private class TestWebScanAdapterConfig extends AbstractWebScanAdapterConfig {

	}
}
