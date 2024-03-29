/*
* Jython Database Specification API 2.0
*
* $Id: PostgresqlDataHandler.java 2469 2005-05-16 06:43:33Z otmarhumbel $
*
* Copyright (c) 2001 brian zimmer <bzimmer@ziclix.com>
*
*/
package com.ziclix.python.sql.handler;

import com.ziclix.python.sql.DataHandler;
import org.python.core.Py;
import org.python.core.PyFile;
import org.python.core.PyObject;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * Postgresql specific data handling.
 *
 * @author brian zimmer
 * @author last revised by $Author: otmarhumbel $
 * @version $Revision: 2469 $
 */
public class PostgresqlDataHandler extends RowIdHandler {

  /**
   * Decorator for handling Postgresql specific issues.
   *
   * @param datahandler the delegate DataHandler
   */
  public PostgresqlDataHandler(DataHandler datahandler) {
    super(datahandler);
  }

  protected String getRowIdMethodName() {
    return "getLastOID";
  }

  /**
   * Override to handle Postgresql related issues.
   *
   * @param set the result set
   * @param col the column number
   * @param type the SQL type
   * @return the mapped Python object
   * @throws SQLException thrown for a sql exception
   */
  public PyObject getPyObject(ResultSet set, int col, int type) throws SQLException {

    PyObject obj = Py.None;

    switch (type) {

      case Types.NUMERIC:
      case Types.DECIMAL:

        // in JDBC 2.0, use of a scale is deprecated
        // The big fix here is a problem with numeric types.  It seems the ResultSet
        // tries to fix a JBuilder bug (as commented in the source) by including a
        // scale of 0.  Well this blows up BigDecimal if the number is, say, 4.22.
        // It appears the workaround is to call the deprecated method with a scale of
        // -1 which forces the return of the BD without setting the scale.
        BigDecimal bd = set.getBigDecimal(col, -1);

        obj = (bd == null) ? Py.None : Py.newFloat(bd.doubleValue());
        break;

      case Types.OTHER:

        // it seems pg doesn't like to handle OTHER types as anything but strings
        // but we'll try first anyways just to see what happens
        try {
          obj = super.getPyObject(set, col, type);
        } catch (SQLException e) {
          obj = super.getPyObject(set, col, Types.VARCHAR);
        }
        break;

      default :
        obj = super.getPyObject(set, col, type);
    }

    return (set.wasNull() || (obj == null)) ? Py.None : obj;
  }

  /**
   * Provide fixes for Postgresql driver.
   *
   * @param stmt
   * @param index
   * @param object
   * @param type
   * @throws SQLException
   */
  public void setJDBCObject(PreparedStatement stmt, int index, PyObject object, int type) throws SQLException {

    if (DataHandler.checkNull(stmt, index, object, type)) {
      return;
    }

    switch (type) {

      case Types.LONGVARCHAR:

        String varchar;
        // Postgresql driver can't handle the setCharacterStream() method so use setObject() instead
        if (object instanceof PyFile) {
          varchar = ((PyFile) object).read();
        } else {
          varchar = (String) object.__tojava__(String.class);
        }
        
        stmt.setObject(index, varchar, type);
        break;

      default :
        super.setJDBCObject(stmt, index, object, type);
    }
  }
}
