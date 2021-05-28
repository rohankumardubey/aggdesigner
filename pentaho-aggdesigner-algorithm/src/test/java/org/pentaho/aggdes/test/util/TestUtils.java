/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.  See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
package org.pentaho.aggdes.test.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

/** Utilities for aggregate designer tests. */
public class TestUtils {

  public static final String NL = System.getProperty("line.separator");

  // ~ Static fields/initializers ============================================

  private static final Log LOGGER = LogFactory.getLog(TestUtils.class);

  private static Properties testProperties;

  private static Map<String, Driver> registeredDrivers = new HashMap<String, Driver>();

  // ~ Constructors ==========================================================

  private TestUtils() {
  }

  // ~ Methods ===============================================================

  static {
    testProperties = new Properties();
    try {
      testProperties.load(
          TestUtils.class.getResourceAsStream("/test.properties"));
      InputStream overrides =
          TestUtils.class.getResourceAsStream("/testoverride.properties");
      if (overrides != null) {
        testProperties.load(overrides);
      }
    } catch (IOException e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  public static String getTestProperty(final String key, final Object... args) {
    return MessageFormat.format(testProperties.getProperty(key), args);
  }

  public static void registerDriver(final String jdbcDriverClasspath,
      final String jdbcDriverClassname)
      throws Exception {
    Driver newDriver;
    if (jdbcDriverClasspath != null && !jdbcDriverClasspath.equals("")) {
      URLClassLoader urlLoader = new URLClassLoader(new URL[] { new URL(jdbcDriverClasspath) });
      Driver d = (Driver) Class.forName(jdbcDriverClassname, true, urlLoader).newInstance();
      newDriver = new DriverShim(d);
    } else {
      newDriver = (Driver) Class.forName(jdbcDriverClassname).newInstance();
    }
    DriverManager.registerDriver(newDriver);
    registeredDrivers.put(jdbcDriverClasspath + ":" + jdbcDriverClassname,
        newDriver);
  }

  public static void deregisterDriver(final String jdbcDriverClasspath,
      final String jdbcDriverClassname)
      throws Exception {
    Driver registeredDriver =
        registeredDrivers.get(jdbcDriverClasspath + ":" + jdbcDriverClassname);
    if (null != registeredDriver) {
      DriverManager.deregisterDriver(registeredDriver);
    }
  }

  // using http://www.kfu.com/~nsayer/Java/dyn-jdbc.html
  // jdbc loading example
  /** Driver shim. */
  private static class DriverShim implements Driver {
    private Driver driver;

    DriverShim(Driver d) {
      this.driver = d;
    }

    public boolean acceptsURL(String u) throws SQLException {
      return this.driver.acceptsURL(u);
    }

    public Connection connect(String u, Properties p) throws SQLException {
      return this.driver.connect(u, p);
    }

    public int getMajorVersion() {
      return this.driver.getMajorVersion();
    }

    public int getMinorVersion() {
      return this.driver.getMinorVersion();
    }

    public DriverPropertyInfo[] getPropertyInfo(String u, Properties p) throws SQLException {
      return this.driver.getPropertyInfo(u, p);
    }

    public boolean jdbcCompliant() {
      return this.driver.jdbcCompliant();
    }

    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
      return null;
    }
  }

  /**
   * Converts a string constant into locale-specific line endings.
   */
  public static String fold(String string) {
    if (!NL.equals("\n")) {
      string = string.replace("\n", NL);
    }
    return string;
  }

}

// End TestUtils.java
