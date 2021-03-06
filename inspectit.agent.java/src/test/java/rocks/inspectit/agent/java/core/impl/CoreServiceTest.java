package rocks.inspectit.agent.java.core.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.hamcrest.internal.ArrayIterator;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.springframework.beans.factory.BeanInitializationException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import rocks.inspectit.agent.java.config.StorageException;
import rocks.inspectit.agent.java.core.IPlatformManager;
import rocks.inspectit.agent.java.core.disruptor.IDisruptorStrategy;
import rocks.inspectit.agent.java.core.impl.CoreService.SensorRefresher;
import rocks.inspectit.agent.java.sensor.jmx.IJmxSensor;
import rocks.inspectit.agent.java.sensor.platform.IPlatformSensor;
import rocks.inspectit.agent.java.stats.AgentStatisticsLogger;
import rocks.inspectit.shared.all.communication.DefaultData;
import rocks.inspectit.shared.all.communication.data.SystemInformationData;
import rocks.inspectit.shared.all.testbase.TestBase;

@SuppressWarnings({ "PMD" })
public class CoreServiceTest extends TestBase {

	@InjectMocks
	CoreService coreService;

	@Mock
	Logger log;

	@Mock
	IDisruptorStrategy disruptorStrategy;

	@Mock
	DefaultDataHandler defaultDataHandler;

	@Mock
	IPlatformManager platformManager;

	@Mock
	ScheduledExecutorService executorService;

	@Mock
	List<IPlatformSensor> platformSensors;

	@Mock
	List<IJmxSensor> jmxSensors;

	@Mock
	AgentStatisticsLogger statsLogger;

	@BeforeMethod
	public void executorShutdown() throws InterruptedException {
		// avoid strange log messages in test
		when(executorService.awaitTermination(anyLong(), Mockito.<TimeUnit> any())).thenReturn(true);
	}

	public static class AddDefaultData extends CoreServiceTest {

		@Mock
		DefaultData data;

		@Test
		public void happyPath() throws InterruptedException, StorageException {
			when(disruptorStrategy.getDataBufferSize()).thenReturn(8);
			coreService.start();

			coreService.addDefaultData(data);

			// need to sleep a bit so handler is notified
			Thread.sleep(100);

			ArgumentCaptor<DefaultDataWrapper> captor = ArgumentCaptor.forClass(DefaultDataWrapper.class);
			verify(defaultDataHandler).onEvent(captor.capture(), anyLong(), eq(true));
			assertThat(captor.getValue().getDefaultData(), is(data));
		}

		@Test
		public void noAddOnShutdown() throws InterruptedException, StorageException {
			when(disruptorStrategy.getDataBufferSize()).thenReturn(8);
			coreService.start();
			coreService.stop();

			coreService.addDefaultData(data);

			// need to sleep a bit so handler is notified
			Thread.sleep(100);
			verifyNoMoreInteractions(defaultDataHandler);
		}

		@Test
		public void capacityReached() throws InterruptedException, StorageException {
			when(disruptorStrategy.getDataBufferSize()).thenReturn(2);
			// slow down the wrapper so we get capacity error
			doAnswer(new Answer<Void>() {
				@Override
				public Void answer(InvocationOnMock invocation) throws Throwable {
					Thread.sleep(1);
					return null;
				}
			}).when(defaultDataHandler).onEvent(Mockito.<DefaultDataWrapper> any(), anyLong(), anyBoolean());
			coreService.start();

			coreService.addDefaultData(data);
			coreService.addDefaultData(data);
			coreService.addDefaultData(data);
			coreService.addDefaultData(data);

			// we should report 2 times
			verify(statsLogger, times(2)).dataDropped(1);
		}

		@AfterMethod
		public void stop() {
			coreService.stop();
		}

	}

	public static class Start extends CoreServiceTest {

		@Test(expectedExceptions = BeanInitializationException.class)
		public void bufferSizeNotPowerOf2() throws InterruptedException, StorageException {
			when(disruptorStrategy.getDataBufferSize()).thenReturn(5);
			coreService.start();
		}

		@Test
		public void sensorRefresherScheduled() throws StorageException {
			when(disruptorStrategy.getDataBufferSize()).thenReturn(8);
			coreService.start();

			ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
			verify(executorService).scheduleWithFixedDelay(captor.capture(), Mockito.anyLong(), Mockito.anyLong(), Mockito.<TimeUnit> any());
			assertThat(captor.getValue(), is(instanceOf(SensorRefresher.class)));
		}

	}

	public static class SensorRefresherRun extends CoreServiceTest {

		@Mock
		IPlatformSensor platformSensor;

		@Mock
		IJmxSensor jmxSensor;

		@Test
		public void runtimeException() {
			RuntimeException runtimeException = new RuntimeException();
			when(platformSensors.isEmpty()).thenThrow(runtimeException);

			Runnable sensorRefresher = coreService.new SensorRefresher();
			sensorRefresher.run();

			// verify logged not not exceptional exit
			verify(log).error(Mockito.anyString(), eq(runtimeException));
		}

		@Test
		public void platformSensor() {
			when(jmxSensors.isEmpty()).thenReturn(true);
			doAnswer(new Answer<Iterator<?>>() {
				@Override
				public Iterator<?> answer(InvocationOnMock invocation) throws Throwable {
					return new ArrayIterator(new IPlatformSensor[] { platformSensor });
				}
			}).when(platformSensors).iterator();

			Runnable sensorRefresher = coreService.new SensorRefresher();
			sensorRefresher.run();

			verify(platformSensor).reset();
			verify(platformSensor).gather();
			verifyNoMoreInteractions(platformSensor);
		}

		@Test
		public void platformSensorCollect() throws InterruptedException, StorageException {
			when(jmxSensors.isEmpty()).thenReturn(true);
			doAnswer(new Answer<Iterator<?>>() {
				@Override
				public Iterator<?> answer(InvocationOnMock invocation) throws Throwable {
					return new ArrayIterator(new IPlatformSensor[] { platformSensor });
				}
			}).when(platformSensors).iterator();
			SystemInformationData sid = mock(SystemInformationData.class);
			when(platformSensor.get()).thenReturn(sid);
			when(disruptorStrategy.getDataBufferSize()).thenReturn(8);
			coreService.start();

			Runnable sensorRefresher = coreService.new SensorRefresher();
			sensorRefresher.run();
			sensorRefresher.run();
			sensorRefresher.run();
			sensorRefresher.run();
			sensorRefresher.run();

			verify(platformSensor).reset();
			verify(platformSensor, times(5)).gather();
			verify(platformSensor).get();
			verifyNoMoreInteractions(platformSensor);

			// need to sleep a bit so handler is notified
			Thread.sleep(100);

			ArgumentCaptor<DefaultDataWrapper> captor = ArgumentCaptor.forClass(DefaultDataWrapper.class);
			verify(defaultDataHandler).onEvent(captor.capture(), anyLong(), eq(true));
			assertThat(captor.getValue().getDefaultData(), is((DefaultData) sid));
		}

		@Test
		public void platformSensorError() {
			when(jmxSensors.isEmpty()).thenReturn(true);
			final List<IPlatformSensor> sensors = new ArrayList<IPlatformSensor>();
			sensors.add(platformSensor);
			doAnswer(new Answer<Iterator<?>>() {
				@Override
				public Iterator<?> answer(InvocationOnMock invocation) throws Throwable {
					return sensors.iterator();
				}
			}).when(platformSensors).iterator();
			doThrow(RuntimeException.class).when(platformSensor).gather();

			Runnable sensorRefresher = coreService.new SensorRefresher();
			sensorRefresher.run();

			assertThat(sensors, is(empty()));
		}

		@Test
		public void jmxSensor() {
			when(platformSensors.isEmpty()).thenReturn(true);
			doReturn(new ArrayIterator(new IJmxSensor[] { jmxSensor })).when(jmxSensors).iterator();

			Runnable sensorRefresher = coreService.new SensorRefresher();
			sensorRefresher.run();

			verify(jmxSensor).update(coreService);
			verifyNoMoreInteractions(jmxSensor);
		}

		@Test
		public void jmxSensorTwice() {
			when(platformSensors.isEmpty()).thenReturn(true);
			doAnswer(new Answer<Iterator<?>>() {
				@Override
				public Iterator<?> answer(InvocationOnMock invocation) throws Throwable {
					return new ArrayIterator(new IJmxSensor[] { jmxSensor });
				}
			}).when(jmxSensors).iterator();

			Runnable sensorRefresher = coreService.new SensorRefresher();
			sensorRefresher.run();
			sensorRefresher.run();

			verify(jmxSensor, times(2)).update(coreService);
			verifyNoMoreInteractions(jmxSensor);
		}

	}

	public static class Stop extends CoreServiceTest {

		@Test
		public void stop() throws Exception {
			when(disruptorStrategy.getDataBufferSize()).thenReturn(8);
			when(executorService.awaitTermination(anyLong(), Mockito.<TimeUnit> any())).thenReturn(true);
			coreService.start();

			coreService.stop();

			verify(executorService).scheduleWithFixedDelay(Mockito.<Runnable> any(), anyLong(), anyLong(), Mockito.<TimeUnit> any());
			verify(executorService).shutdown();
			verify(executorService).awaitTermination(anyLong(), Mockito.<TimeUnit> any());
			verifyNoMoreInteractions(executorService);
		}

		@Test
		public void stopTwice() throws Exception {
			when(disruptorStrategy.getDataBufferSize()).thenReturn(8);
			when(executorService.awaitTermination(anyLong(), Mockito.<TimeUnit> any())).thenReturn(true);
			coreService.start();

			coreService.stop();
			coreService.stop();

			verify(executorService).scheduleWithFixedDelay(Mockito.<Runnable> any(), anyLong(), anyLong(), Mockito.<TimeUnit> any());
			verify(executorService).shutdown();
			verify(executorService).awaitTermination(anyLong(), Mockito.<TimeUnit> any());
			verifyNoMoreInteractions(executorService);
		}
	}

}
