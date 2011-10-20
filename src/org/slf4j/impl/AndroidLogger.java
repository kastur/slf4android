package org.slf4j.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Properties;

import org.slf4j.helpers.MarkerIgnoringBase;
import org.slf4j.helpers.MessageFormatter;

import android.util.Log;

/**
 * <p>
 * This class adapts the SLF4J interface to the Android Log system. The
 * intention is to make it easy to control logging from legacy code systems
 * properly. As such each log method checks to see if that log level is enabled
 * since the legacy code this adapts for is often written with out such checks
 * counting on the logging system to handle filtering out these messages. This
 * is not the best thing for performance but we view it as the lesser of two
 * evils.
 * </p>
 * <p>
 * Note that most legacy logging system use a class name as the tag. Android
 * however requires tags be of a specific (and short) length. As such all class
 * names are automatically shortened in some way. To see how the class name you
 * are interested in gets shortened monitor the 'slf4j' tag. Generally we try to
 * use the whole class name if it will fit but more often this will get
 * shortened to the first letters of each portion of the path followed by the
 * class name or simply the class name if that will not fit.
 * </p>
 * <p>
 * Note also that we check isDebugEnabled with every call despite this adding
 * possible overhead since a great deal of legacy code does not check the
 * logging level first.
 * </p>
 * <p>
 * To configure this logger you can include an SLF4J.properties file in your apk
 * with lines of the form:
 *
 * <pre>
 * &lt;package&gt;.&lt;class&gt;=&lt;level&gt;
 * </pre>
 *
 * where level is one of: 'disabled', 'trace', 'debug', 'info', 'warn' or
 * 'error'<br/>
 * You may also include a 'default.log.level=<level>' line to set the default
 * level for all classes. Note that it is allowed to specify a class twice in
 * which case the lowest level specified in the file will be used except for the
 * default where the last instance in the file will be used.
 * </p>
 * <p>
 * It is also possible to ask this logger to check the android log level by
 * adding "android.util.Log.check=true" to your properties file.
 * </p>
 * <p>
 * It is also possible to have all log statements forced into a single tag
 * for your application using:<br/>
 * force.tag=true<br/>
 * With this option we suggest adding:<br/>
 * force.tag.prepend=true<br/>
 * Which will make all logged statements include what the log tag would have
 * been at the start of the log line.
 * </p>
 * <p>
 * We search for this properties file in the root of your JAR.
 * </p>
 * <p>
 * Finally, it is possible to efficiently disable all logging entirely
 * by including a class named NOSLF4J in the default package in which case
 * we will disable all logging without checking for log properties. This
 * is perfect for a production environment.
 * </p>
 **/
public class AndroidLogger extends MarkerIgnoringBase {

	/** The name of the properties file. */
	private static final String CONFIG_FILE_NAME = "SLF4J.properties";

	/** The length for half a tag we are cutting in the middle. */
	private static final int	HALF_TAG_LENGTH	= 10;

	/** The maximum length allowed for an Android log tag. */
	private static final int	MAX_LOG_TAG	= 23;

	/**
	 * Serial Version ID.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Log tag for SLF4J itself.
	 **/
	private static final String SLF4J_TAG = "slf4j";

	/**
	 * The tag this logger will log with.
	 */
	private final String tag;

	/**
	 * In prepend mode we use this.
	 */
	private final String prependTag;

	/**
	 * The level this logger is working at.
	 */
	private final int level;

	/** Trace log level. */
	private static final int TRACE = 1;
	/** Debug log level. */
	private static final int DEBUG = 2;
	/** Info log level. */
	private static final int INFO = 3;
	/** Warn log level. */
	private static final int WARN = 4;
	/** Error log level. */
	private static final int ERROR = 5;
	/** Disabled log level. */
	private static final int DISABLED = 0;
	/** The default log level is DISABLED. */
	private static final int DEFAULT_LOG_LEVEL = DISABLED;
	/** Log Levels > ERROR are invalid. */
	private static final int INVALID_LEVEL = ERROR + 1;

	/** The default level for all loggers. */
	private static int sDefaultLevel = DEFAULT_LOG_LEVEL;
	/** A static forced tag so all logging goes to the same tag. */
	private static String sForceTag = null;
	/** Force the real tag to prepend to the message. */
	private static boolean sForcePrependTag = false;

	/** String equivalents of log levels. */
	private static final String[] LEVEL_NAMES = { "disabled", "trace", "debug",
		"info", "warn", "error"};

	/**
	 * The levels for various tags parsed from the configuration file.
	 */
	// This is dumb. You can't have an array of generics?
	@SuppressWarnings("unchecked")
	private static final ArrayList<String>[] TAG_LEVELS =
		new ArrayList[LEVEL_NAMES.length];

	/** Property with default log level: default.log.level. */
	private static final String DEFAULT_LEVEL_NAME = "default.log.level";
	/** Property for triggering check android level: check.android.level. */
	private static final String ANDROID_LEVEL_CHECK = "check.android.level";
	/** Property for a forced tag: force.tag. */
	private static final String FORCE_TAG = "force.tag";
	/**
	 * Property for prepending the real tag if force.tag is set:
	 * force.tag.prepend.
	 */
	private static final String FORCE_PREPEND_TAG = "force.tag.prepend";

	/**
	 * Should we ignore android level?
	 */
	private static boolean ignoreAndroidLevel = true;

	/**
	 * Parses a level name into the numeric equivalent.
	 * @param levelName the name to parse
	 * @return the level or -1 if it can not be parsed
	 */
	private static int parseLevel(final String levelName) {
		for (int i = 0; i < LEVEL_NAMES.length; i++) {
			if (LEVEL_NAMES[i].equals(levelName)) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Android offers no way to turn off info and higher level messages This is
	 * not so great for integrating legacy services that expect a more advanced
	 * logger that can do some filtering. We thus use a property file in the
	 * form class.path.Class=level where level is one of "disabled", "trace",
	 * "debug", "info", "warn" or "error";
	 *
	 * If you're going to release your application you can include an empty
	 * NOSLF4J.java in your source. This will turn off logging and increase
	 * performance.
	 **/
	static {
		try {
			Class.forName("NOSLF4J");
			sDefaultLevel = DISABLED;
		} catch (ClassNotFoundException classNotFoundException) {
			// Load and parse the properties.
			Log.d(SLF4J_TAG, "Trying to load properties from: "
			+ CONFIG_FILE_NAME);
			InputStream in = AndroidLogger.class.getClassLoader()
					.getResourceAsStream(CONFIG_FILE_NAME);
			if (null != in) {
				Log.d(SLF4J_TAG, "Loading properties...");
				Properties props = new java.util.Properties();
				try {
					props.load(in);
					for (Enumeration<?> names = props.propertyNames(); names
							.hasMoreElements();) {
						String name = (String) names.nextElement();
						if (name.equals(FORCE_TAG)) {
							sForceTag = props.getProperty(name);
							Log.d(SLF4J_TAG, "Set force tag to: " + sForceTag);
							continue;
						}
						if (name.equals(FORCE_PREPEND_TAG)) {
							sForcePrependTag =
									Boolean.parseBoolean(
											props.getProperty(name));
							Log.d(SLF4J_TAG,
									"Set force prepend tag to:"
											+ sForcePrependTag);
							continue;
						}
						if (name.equals(ANDROID_LEVEL_CHECK)) {
							ignoreAndroidLevel = !Boolean.parseBoolean(props
									.getProperty(name));
							Log.d(SLF4J_TAG, "Set ignore android level to:"
									+ ignoreAndroidLevel);
							continue;
						}
						// What level is this?
						int value = parseLevel(props.getProperty(name));
						if (value >= 0) {
							if (DEFAULT_LEVEL_NAME.equals(name)) {
								Log.d(SLF4J_TAG, "Setting default level to: "
										+ LEVEL_NAMES[value]);
								sDefaultLevel = value;
							} else {
								// Add it to the list for that level.
								Log.d(SLF4J_TAG, "Setting level for: " + name
										+ " to: " + LEVEL_NAMES[value]);
								if (TAG_LEVELS[value] == null) {
									TAG_LEVELS[value] = new ArrayList<String>();
								}
								TAG_LEVELS[value].add(name);
							}
						} else {
							Log.w(SLF4J_TAG, "Unknown level for: " + name
									+ ": '" + props.getProperty(name)
									+ "'. Using default.");
						}
					}
				} catch (IOException e) {
					Log.e(SLF4J_TAG, "Error while loading properties: "
							+ e.getMessage());
				}
			} else {
				Log.w(SLF4J_TAG, "No configuration file found: " + in);
			}
		}
	}

	/**
	 * Package access allows only {@link AndroidLoggerFactory} to instantiate
	 * AndroidLogger instances.
	 * @param loggerTag the tag for this logger
	 */
	AndroidLogger(final String loggerTag) {
		// Android only supports tags of length <= 23
		if (loggerTag.length() > MAX_LOG_TAG) {
			// We try to do something smart here to shorten
			StringBuffer shortTag = new StringBuffer();
			String[] parts = loggerTag.split("\\.");
			String lastPart = parts[parts.length - 1];
			// Can we use the whole last part?
			if (lastPart.length() < MAX_LOG_TAG) {
				if (((parts.length - 1) * 2) + lastPart.length()
						<= MAX_LOG_TAG) {
					for (int x = 0; x < parts.length - 1; x++) {
						shortTag.append(parts[x].charAt(0));
						shortTag.append('.');
					}
				}
				shortTag.append(lastPart);
			} else {
				shortTag.append(lastPart.substring(0,
						HALF_TAG_LENGTH));
				shortTag.append("...");
				shortTag.append(lastPart.substring(
						lastPart.length() - HALF_TAG_LENGTH));
			}
			this.tag = shortTag.toString();
			Log.d(SLF4J_TAG, "Tag: " + loggerTag
					+ " shortened to: " + this.tag);
		} else {
			this.tag = loggerTag;
		}

		if (sForcePrependTag) {
			StringBuffer spaces = new StringBuffer(this.tag);

			for (int i = 0; i < MAX_LOG_TAG - this.tag.length(); i++) {
				spaces.append(' ');
			}
			spaces.append(':');
			spaces.append(' ');

			this.prependTag = spaces.toString();
			Log.d(SLF4J_TAG, "Prepend Tag: " + this.prependTag);
		} else {
			this.prependTag = "";
		}

		// Now to figure out what level this should be at
		// We take the lowest level we can find and run with it
		int foundLevel = -1;
		for (int i = 0; i < TAG_LEVELS.length; i++) {
			if (TAG_LEVELS[i] != null) {
				if (TAG_LEVELS[i].contains(this.tag)
						|| TAG_LEVELS[i].contains(loggerTag)) {
					foundLevel = i;
					break;
				}
			}
		}
		if (foundLevel < 0 || foundLevel == INVALID_LEVEL) {
			this.level = sDefaultLevel;
		} else {
			this.level = foundLevel;
		}
		Log.d(SLF4J_TAG, "Level for: " + this.tag + " set to: "
				+ LEVEL_NAMES[this.level]);
	}

	/**
	 * @param message the message to prepend to
	 * @return the string with possible tag prepened.
	 */
	private String getPrepend(final String message) {
		if (sForcePrependTag) {
			return prependTag + message;
		}
		return message;
	}

	/**
	 * @param message the message to format
	 * @param parameter the parameter to the message
	 * @return the formatted message possibly with tag prepended.
	 */
	private String getPrepend(final String message, final Object parameter) {
		String formatted =
				MessageFormatter.format(message, parameter).getMessage();
		return getPrepend(formatted);
	}

	/**
	 * @param message the message to format
	 * @param parameters the parameters for the message
	 * @return the formatted message with possible prepended tag
	 */
	private String getPrepend(final String message, final Object[] parameters) {
		String formatted =
				MessageFormatter.arrayFormat(message, parameters).getMessage();
		return getPrepend(formatted);
	}

	/**
	 * @param message the message to format
	 * @param firstParam first message parameter
	 * @param secondParam second message parameter
	 * @return the formatted message with possible prepended tag
	 */
	private String getPrepend(final String message,
			final Object firstParam, final Object secondParam) {
		String formatted =
				MessageFormatter.format(message,
						firstParam, secondParam).getMessage();
		return getPrepend(formatted);
	}

	/**
	 * @return the tag for this logger.
	 */
	private String getTag() {
		if (sForceTag != null) {
			return sForceTag;
		}
		return tag;
	}

	@Override
	public final void debug(final String message) {
		if (isDebugEnabled()) {
			Log.d(getTag(), getPrepend(message));
		}
	}

	@Override
	public final void debug(final String arg0, final Object arg1) {
		if (isDebugEnabled()) {
			Log.d(getTag(), getPrepend(arg0, arg1));
		}
	}

	@Override
	public final void debug(final String arg0, final Object[] arg1) {
		if (isDebugEnabled()) {
			Log.d(getTag(), getPrepend(arg0, arg1));
		}
	}

	@Override
	public final void debug(final String arg0, final Throwable arg1) {
		if (isDebugEnabled()) {
			Log.d(getTag(), getPrepend(arg0), arg1);
		}
	}

	@Override
	public final void debug(final String arg0, final Object arg1,
			final Object arg2) {
		if (isDebugEnabled()) {
			Log.d(getTag(), getPrepend(arg0, arg1, arg2));
		}
	}

	@Override
	public final void error(final String arg0) {
		if (isErrorEnabled()) {
			Log.e(getTag(), getPrepend(arg0));
		}
	}

	@Override
	public final void error(final String arg0, final Object arg1) {
		if (isErrorEnabled()) {
			Log.e(getTag(), getPrepend(arg0, arg1));
		}
	}

	@Override
	public final void error(final String arg0, final Object[] arg1) {
		if (isErrorEnabled()) {
			Log.e(getTag(), getPrepend(arg0, arg1));
		}
	}

	@Override
	public final void error(final String arg0, final Throwable arg1) {
		if (isErrorEnabled()) {
			Log.e(getTag(), getPrepend(arg0), arg1);
		}
	}

	@Override
	public final void error(final String arg0, final Object arg1,
			final Object arg2) {
		if (isErrorEnabled()) {
			Log.e(getTag(), getPrepend(arg0, arg1, arg1));
		}
	}

	@Override
	public final void info(final String arg0) {
		if (isInfoEnabled()) {
			Log.i(getTag(), getPrepend(arg0));
		}
	}

	@Override
	public final void info(final String arg0, final Object arg1) {
		if (isInfoEnabled()) {
			Log.i(getTag(), getPrepend(arg0, arg1));
		}
	}

	@Override
	public final void info(final String arg0, final Object[] arg1) {
		if (isInfoEnabled()) {
			Log.i(getTag(), getPrepend(arg0, arg1));
		}
	}

	@Override
	public final void info(final String arg0, final Throwable arg1) {
		if (isInfoEnabled()) {
			Log.i(getTag(), getPrepend(arg0), arg1);
		}
	}

	@Override
	public final void info(final String arg0, final Object arg1,
			final Object arg2) {
		if (isInfoEnabled()) {
			Log.i(getTag(), getPrepend(arg0, arg1, arg2));
		}
	}

	@Override
	public final boolean isDebugEnabled() {
		return this.level <= DEBUG
				&& (ignoreAndroidLevel || Log.isLoggable(tag, Log.DEBUG));
	}

	@Override
	public final boolean isErrorEnabled() {
		return this.level <= ERROR
				&& (ignoreAndroidLevel || Log.isLoggable(tag, Log.ERROR));
	}

	@Override
	public final boolean isInfoEnabled() {
		return this.level <= INFO
				&& (ignoreAndroidLevel || Log.isLoggable(tag, Log.INFO));
	}

	@Override
	public final boolean isTraceEnabled() {
		return this.level <= TRACE
				&& (ignoreAndroidLevel || Log.isLoggable(tag, Log.VERBOSE));
	}

	@Override
	public final boolean isWarnEnabled() {
		return this.level <= WARN
				&& (ignoreAndroidLevel || Log.isLoggable(tag, Log.WARN));
	}

	@Override
	public final void trace(final String arg0) {
		if (isTraceEnabled()) {
			Log.v(getTag(), getPrepend(arg0));
		}
	}

	@Override
	public final void trace(final String arg0, final Object arg1) {
		if (isTraceEnabled()) {
			Log.v(getTag(), getPrepend(arg0, arg1));
		}
	}

	@Override
	public final void trace(final String arg0, final Object[] arg1) {
		if (isTraceEnabled()) {
			Log.v(getTag(), getPrepend(arg0, arg1));
		}
	}

	@Override
	public final void trace(final String arg0, final Throwable arg1) {
		if (isTraceEnabled()) {
			Log.v(getTag(), getPrepend(arg0), arg1);
		}
	}

	@Override
	public final void trace(final String arg0, final Object arg1,
			final Object arg2) {
		if (isTraceEnabled()) {
			Log.v(getTag(), getPrepend(arg0, arg1, arg2));
		}
	}

	@Override
	public final void warn(final String arg0) {
		if (isWarnEnabled()) {
			Log.w(getTag(), getPrepend(arg0));
		}
	}

	@Override
	public final void warn(final String arg0, final Object arg1) {
		if (isWarnEnabled()) {
			Log.w(getTag(), getPrepend(arg0, arg1));
		}
	}

	@Override
	public final void warn(final String arg0, final Object[] arg1) {
		if (isWarnEnabled()) {
			Log.w(getTag(), getPrepend(arg0, arg1));
		}
	}

	@Override
	public final void warn(final String arg0, final Throwable arg1) {
		if (isWarnEnabled()) {
			Log.w(getTag(), getPrepend(arg0), arg1);
		}
	}

	@Override
	public final void warn(final String arg0, final Object arg1,
			final Object arg2) {
		if (isWarnEnabled()) {
			Log.w(getTag(), getPrepend(arg0, arg1, arg2));
		}
	}

}
