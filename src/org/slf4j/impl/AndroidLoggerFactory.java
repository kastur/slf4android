package org.slf4j.impl;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

/**
 * The logger factory for Android loggers.
 * @author nick &lt;palmer@cs.vu.nl&gt;
 *
 */
public final class AndroidLoggerFactory implements ILoggerFactory {
	/**
	 * The singleton factory instance.
	 */
	static final AndroidLoggerFactory SINGLETON = new AndroidLoggerFactory();

	/**
	 * The map with all loggers.
	 */
	@SuppressWarnings("rawtypes")
	private final Map loggerMap;

	/**
	 * Constructor for the singleton.
	 */
	@SuppressWarnings("rawtypes")
	private AndroidLoggerFactory() {
		loggerMap = new HashMap();
	}

	/**
	 * @param name the tag for this logger
	 * @return an appropriate {@link AndroidLogger} instance by name.
	 */
	@Override
	@SuppressWarnings("unchecked")
	public Logger getLogger(final String name) {
		Logger slogger = null;
		// protect against concurrent access of the loggerMap
		synchronized (this) {
			slogger = (Logger) loggerMap.get(name);
			if (slogger == null) {
				slogger = new AndroidLogger(name);
				loggerMap.put(name, slogger);
			}
		}
		return slogger;
	}

}
