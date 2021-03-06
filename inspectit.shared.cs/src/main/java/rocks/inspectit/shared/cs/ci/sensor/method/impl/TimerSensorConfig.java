package rocks.inspectit.shared.cs.ci.sensor.method.impl;

import javax.xml.bind.annotation.XmlRootElement;

import rocks.inspectit.shared.all.instrumentation.config.PriorityEnum;
import rocks.inspectit.shared.cs.ci.sensor.StringConstraintSensorConfig;
import rocks.inspectit.shared.cs.ci.sensor.method.IMethodSensorConfig;

/**
 * Timer sensor configuration.
 *
 * @author Ivan Senic
 *
 */
@XmlRootElement(name = "timer-sensor-config")
public class TimerSensorConfig extends StringConstraintSensorConfig implements IMethodSensorConfig {

	/**
	 * Sensor name.
	 */
	public static final String SENSOR_NAME = "Timer Sensor";

	/**
	 * Implementing class name.
	 */
	public static final String CLASS_NAME = "rocks.inspectit.agent.java.sensor.method.timer.TimerSensor";

	/**
	 * No-args constructor.
	 */
	public TimerSensorConfig() {
		super(100);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getName() {
		return SENSOR_NAME;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getClassName() {
		return CLASS_NAME;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public PriorityEnum getPriority() {
		return PriorityEnum.MAX;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isAdvanced() {
		return false;
	}

}
