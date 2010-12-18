/**
   This file is part of GoldenGate Project (named also GoldenGate or GG).

   Copyright 2009, Frederic Bregier, and individual contributors by the @author
   tags. See the COPYRIGHT.txt in the distribution for a full listing of
   individual contributors.

   All GoldenGate Project is free software: you can redistribute it and/or 
   modify it under the terms of the GNU General Public License as published 
   by the Free Software Foundation, either version 3 of the License, or
   (at your option) any later version.

   GoldenGate is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with GoldenGate .  If not, see <http://www.gnu.org/licenses/>.
 */
package goldengate.common.database;

import goldengate.common.logging.GgInternalLogger;
import goldengate.common.logging.GgInternalLoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import goldengate.common.database.exception.OpenR66DatabaseNoConnectionError;
import goldengate.common.database.exception.OpenR66DatabaseNoDataException;
import goldengate.common.database.exception.OpenR66DatabaseSqlError;
import goldengate.common.database.model.DbModelFactory;

/**
 * Class to handle request
 *
 * @author Frederic Bregier
 *
 */
public class DbRequest {
    /**
     * Internal Logger
     */
    private static final GgInternalLogger logger = GgInternalLoggerFactory
            .getLogger(DbRequest.class);

    /**
     * Internal Statement
     */
    private Statement stmt = null;

    /**
     * Internal Result Set
     */
    private ResultSet rs = null;

    /**
     * Internal DB Session
     */
    private final DbSession ls;

    /**
     * Create a new request from the DbSession
     *
     * @param ls
     * @throws OpenR66DatabaseNoConnectionError
     */
    public DbRequest(DbSession ls) throws OpenR66DatabaseNoConnectionError {
        DbModelFactory.dbModel.validConnection(ls);
        this.ls = ls;
    }

    /**
     * Create a new request from the DbSession but without validating it
     *
     * @param ls
     * @param ignored ignored param
     * @throws OpenR66DatabaseNoConnectionError
     */
    public DbRequest(DbSession ls, boolean ignored) throws OpenR66DatabaseNoConnectionError {
        this.ls = ls;
    }

    /**
     * Create a statement with some particular options
     *
     * @return the new Statement
     * @throws OpenR66DatabaseNoConnectionError
     * @throws OpenR66DatabaseSqlError
     */
    private Statement createStatement()
            throws OpenR66DatabaseNoConnectionError, OpenR66DatabaseSqlError {
        if (ls == null) {
            throw new OpenR66DatabaseNoConnectionError("No connection");
        }
        if (ls.conn == null) {
            throw new OpenR66DatabaseNoConnectionError("No connection");
        }
        try {
            return ls.conn.createStatement();
        } catch (SQLException e) {
            throw new OpenR66DatabaseSqlError("Error while Create Statement", e);
        }
    }

    /**
     * Execute a SELECT statement and set of Result. The statement must not be
     * an update/insert/delete. The previous statement and resultSet are closed.
     *
     * @param select
     * @throws OpenR66DatabaseSqlError
     * @throws OpenR66DatabaseNoConnectionError
     */
    public void select(String select) throws OpenR66DatabaseNoConnectionError,
            OpenR66DatabaseSqlError {
        close();
        stmt = createStatement();
        // rs = stmt.executeQuery(select);
        // or alternatively, if you don't know ahead of time that
        // the query will be a SELECT...
        try {
            if (stmt.execute(select)) {
                rs = stmt.getResultSet();
            }
        } catch (SQLException e) {
            logger.error("SQL Exception Request:" + select+
                    "\n"+e.getMessage());
            DbSession.error(e);
            throw new OpenR66DatabaseSqlError(
                    "SQL Exception Request:" + select, e);
        }
    }

    /**
     * Execute a UPDATE/INSERT/DELETE statement and returns the number of row.
     * The previous statement and resultSet are closed.
     *
     * @param query
     * @return the number of row in the query
     * @throws OpenR66DatabaseSqlError
     * @throws OpenR66DatabaseNoConnectionError
     */
    public int query(String query) throws OpenR66DatabaseNoConnectionError,
            OpenR66DatabaseSqlError {
        close();
        stmt = createStatement();
        try {
            int rowcount = stmt.executeUpdate(query);
            logger.debug("QUERY(" + rowcount + "): {}", query);
            return rowcount;
        } catch (SQLException e) {
            logger.error("SQL Exception Request:" + query+
                    "\n"+e.getMessage());
            DbSession.error(e);
            throw new OpenR66DatabaseSqlError("SQL Exception Request:" + query,
                    e);
        }
    }

    /**
     * Finished a Request (ready for a new one)
     */
    public void close() {
        // it is a good idea to release
        // resources in a finally{} block
        // in reverse-order of their creation
        // if they are no-longer needed
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException sqlEx) {
            } // ignore
            rs = null;
        }
        if (stmt != null) {
            try {
                stmt.close();
            } catch (SQLException sqlEx) {
            } // ignore
            stmt = null;
        }
    }

    /**
     * Get the last ID autoincrement from the last request
     *
     * @return the long Id or DbConstant.ILLEGALVALUE (Long.MIN_VALUE) if an
     *         error occurs.
     * @throws OpenR66DatabaseNoDataException
     */
    public long getLastId() throws OpenR66DatabaseNoDataException {
        ResultSet rstmp;
        long result = DbConstant.ILLEGALVALUE;
        try {
            rstmp = stmt.getGeneratedKeys();
            if (rstmp.next()) {
                result = rstmp.getLong(1);
            }
            rstmp.close();
            rstmp = null;
        } catch (SQLException e) {
            DbSession.error(e);
            throw new OpenR66DatabaseNoDataException("No data found", e);
        }
        return result;
    }

    /**
     * Move the cursor to the next result
     *
     * @return True if there is a next result, else False
     * @throws OpenR66DatabaseNoConnectionError
     * @throws OpenR66DatabaseSqlError
     */
    public boolean getNext() throws OpenR66DatabaseNoConnectionError,
            OpenR66DatabaseSqlError {
        if (rs == null) {
            logger.error("SQL ResultSet is Null into getNext");
            throw new OpenR66DatabaseNoConnectionError(
                    "SQL ResultSet is Null into getNext");
        }
        try {
            return rs.next();
        } catch (SQLException e) {
            logger.warn("SQL Exception to getNextRow"+
                    "\n"+e.getMessage());
            DbSession.error(e);
            throw new OpenR66DatabaseSqlError("SQL Exception to getNextRow", e);
        }
    }

    /**
     *
     * @return The resultSet (can be used in conjunction of getNext())
     * @throws OpenR66DatabaseNoConnectionError
     */
    public ResultSet getResultSet() throws OpenR66DatabaseNoConnectionError {
        if (rs == null) {
            throw new OpenR66DatabaseNoConnectionError(
                    "SQL ResultSet is Null into getResultSet");
        }
        return rs;
    }

    /**
     * Test if value is null and create the string for insert/update
     *
     * @param value
     * @return the string as result
     */
    public static String getIsNull(String value) {
        return value == null? " is NULL" : " = '" + value + "'";
    }
}
