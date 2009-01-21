package org.slf4j.impl;

import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;
import org.slf4j.spi.LoggerFactoryBinder;

/**
 * The binding of {@link LoggerFactory} class with an actual instance of 
 * {@link ILoggerFactory} is performed using information returned by this class. 
 */
public class StaticLoggerBinder implements LoggerFactoryBinder {

  /**
   * The unique instance of this class.
   */
  public static final StaticLoggerBinder SINGLETON = new StaticLoggerBinder();
  
  /**
   * Declare the version of the SLF4J API this implementation is compiled against. 
   * The value of this field is usually modified with each release. 
   */
  // to avoid constant folding by the compiler, this field must *not* be final
  public static String REQUESTED_API_VERSION = "1.5.5";  // !final
  
  private static final String loggerFactoryClassStr = AndroidLoggerFactory.class.getName();

  /** The ILoggerFactory instance returned by the {@link #getLoggerFactory} method
   * should always be the same object
   */
  private final ILoggerFactory loggerFactory;
  
  private StaticLoggerBinder() {
    loggerFactory = AndroidLoggerFactory.SINGLETON;
  }
  
  public ILoggerFactory getLoggerFactory() {
    return loggerFactory;
  }
  
  public String getLoggerFactoryClassStr() {
    return loggerFactoryClassStr;
  }   
}