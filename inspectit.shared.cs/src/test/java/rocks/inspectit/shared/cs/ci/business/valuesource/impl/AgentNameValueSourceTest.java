package rocks.inspectit.shared.cs.ci.business.valuesource.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.emptyArray;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.testng.annotations.Test;

import rocks.inspectit.shared.all.cmr.model.PlatformIdent;
import rocks.inspectit.shared.all.communication.data.InvocationSequenceData;
import rocks.inspectit.shared.all.testbase.TestBase;
import rocks.inspectit.shared.cs.cmr.service.cache.CachedDataService;

/**
 * Tests the {@link AgentNameValueSource} class.
 * 
 * @author Tobias Angerstein
 *
 */
@SuppressWarnings("PMD")
public class AgentNameValueSourceTest extends TestBase {

	@InjectMocks
	AgentNameValueSource valueSource;

	@Mock
	CachedDataService cachedDataService;

	@Mock
	InvocationSequenceData invocationSequenceData;

	@Mock
	PlatformIdent platformIdent;

	/**
	 * Tests
	 * {@link AgentNameValueSource#getStringValues(InvocationSequenceData, rocks.inspectit.shared.all.cmr.service.ICachedDataService)}}
	 * .
	 */
	public static class GetStringValues extends AgentNameValueSourceTest {

		@Test
		public void retrieveAgentName() {
			when(cachedDataService.getPlatformIdentForId(1L)).thenReturn(platformIdent);
			when(invocationSequenceData.getPlatformIdent()).thenReturn(1L);
			when(platformIdent.getAgentName()).thenReturn("agentName");

			String[] values = valueSource.getStringValues(invocationSequenceData, cachedDataService);

			assertThat(values, is(arrayContaining("agentName")));
		}

		@Test
		public void platformIdentIsNull() {
			when(cachedDataService.getPlatformIdentForId(1L)).thenReturn(null);
			when(invocationSequenceData.getPlatformIdent()).thenReturn(1L);

			String[] values = valueSource.getStringValues(invocationSequenceData, cachedDataService);

			assertThat(values, is(emptyArray()));
		}
	}

}
