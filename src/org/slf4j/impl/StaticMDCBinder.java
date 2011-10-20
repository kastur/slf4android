package org.slf4j.impl;

import org.slf4j.helpers.NOPMDCAdapter;
import org.slf4j.spi.MDCAdapter;


/**
 * This implementation is bound to {@link NOPMDCAdapter}.
 *
 * @author Ceki G&uuml;lc&uuml;
 */
public final class StaticMDCBinder {


	/**
	 * The unique instance of this class.
	 */
	public static final StaticMDCBinder SINGLETON = new StaticMDCBinder();

	/**
	 * Construction only for the singleton.
	 */
	private StaticMDCBinder() {
	}

	/**
	 * @return this method always returns an instance of
	 * {@link NOPMDCAdapter}.
	 */
	public MDCAdapter getMDCA() {
		return new NOPMDCAdapter();
	}

	/**
	 * @return the name of the NOPMDCAdapter.
	 */
	public String  getMDCAdapterClassStr() {
		return NOPMDCAdapter.class.getName();
	}
}
