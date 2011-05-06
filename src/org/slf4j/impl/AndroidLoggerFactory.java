package org.slf4j.impl;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

public class AndroidLoggerFactory implements ILoggerFactory {
    final static AndroidLoggerFactory SINGLETON = new AndroidLoggerFactory();

    @SuppressWarnings("rawtypes")
	Map loggerMap;

    @SuppressWarnings("rawtypes")
	private AndroidLoggerFactory() {
        loggerMap = new HashMap();
    }

    /**
     * Return an appropriate {@link AndroidLogger} instance by name.
     */
    @SuppressWarnings("unchecked")
    public Logger getLogger(String name) {
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
