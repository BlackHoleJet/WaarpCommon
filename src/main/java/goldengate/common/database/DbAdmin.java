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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;

import goldengate.common.database.exception.OpenR66DatabaseNoConnectionError;
import goldengate.common.database.exception.OpenR66DatabaseSqlError;
import goldengate.common.database.model.DbType;
import goldengate.common.database.model.DbModelFactory;

/**
 * Class for access to Database
 *
 * @author Frederic Bregier
 *
 */
public class DbAdmin {
    /**
     * Internal Logger
     */
    private static final GgInternalLogger logger = GgInternalLoggerFactory
            .getLogger(DbAdmin.class);
    
    public static int RETRYNB = 3;
    public static long WAITFORNETOP = 100;
    
    /**
     * Database type
     */
    public DbType typeDriver;

    /**
     * DB Server
     */
    private String server = null;

    /**
     * DB User
     */
    private String user = null;

    /**
     * DB Password
     */
    private String passwd = null;

    /**
     * Is this DB Admin connected
     */
    public boolean isConnected = false;

    /**
     * Is this DB Admin Read Only
     */
    public boolean isReadOnly = false;

    /**
     * Is this DB Admin accessed by only one thread at a time (no concurrency
     * and no lock/unlock problem)
     */
    public boolean isMultipleDBAccess = false;

    /**
     * session is the Session object for all type of requests
     */
    public DbSession session = null;

    /**
     * Validate connection
     *
     * @throws OpenR66DatabaseNoConnectionError
     */
    public void validConnection() throws OpenR66DatabaseNoConnectionError {
        try {
            DbModelFactory.dbModel.validConnection(session);
        } catch (OpenR66DatabaseNoConnectionError e) {
            isConnected = false;
            throw e;
        }
        isConnected = true;
    }

    /**
     * Use a default server for basic connection. Later on, specific connection
     * to database for the scheme that provides access to the table R66DbIndex
     * for one specific Legacy could be done.
     *
     * A this time, only one driver is possible! If a new driver is needed, then
     * we need to create a new DbSession object. Be aware that
     * DbSession.initialize should be call only once for each driver, whatever
     * the number of DbSession objects that could be created (=> need a
     * hashtable for specific driver when created). Also, don't know if two
     * drivers at the same time (two different DbSession) is allowed by JDBC.
     *
     * @param driver
     * @param server
     * @param user
     * @param passwd
     * @throws OpenR66DatabaseNoConnectionError
     */
    public DbAdmin(DbType driver, String server, String user, String passwd)
            throws OpenR66DatabaseNoConnectionError {
        this.server = server;
        this.user = user;
        this.passwd = passwd;
        this.typeDriver = driver;
        if (typeDriver == null) {
            logger.error("Cannot find TypeDriver:" + driver.name());
            throw new OpenR66DatabaseNoConnectionError(
                    "Cannot find database drive:" + driver.name());
        }
        session = new DbSession(this.server, this.user, this.passwd, false);
        isReadOnly = false;
        validConnection();
    }

    /**
     * Use a default server for basic connection. Later on, specific connection
     * to database for the scheme that provides access to the table R66DbIndex
     * for one specific Legacy could be done.
     *
     * A this time, only one driver is possible! If a new driver is needed, then
     * we need to create a new DbSession object. Be aware that
     * DbSession.initialize should be call only once for each driver, whatever
     * the number of DbSession objects that could be created (=> need a
     * hashtable for specific driver when created). Also, don't know if two
     * drivers at the same time (two different DbSession) is allowed by JDBC.
     *
     * @param driver
     * @param server
     * @param user
     * @param passwd
     * @param write
     * @throws OpenR66DatabaseSqlError
     * @throws OpenR66DatabaseNoConnectionError
     */
    public DbAdmin(DbType driver, String server, String user, String passwd,
            boolean write) throws OpenR66DatabaseNoConnectionError {
        this.server = server;
        this.user = user;
        this.passwd = passwd;
        this.typeDriver = driver;
        if (typeDriver == null) {
            logger.error("Cannot find TypeDriver:" + driver.name());
            throw new OpenR66DatabaseNoConnectionError(
                    "Cannot find database drive:" + driver.name());
        }
        if (write) {
            for (int i = 0; i < RETRYNB; i ++) {
                try {
                    session = new DbSession(this.server, this.user,
                            this.passwd, false);
                } catch (OpenR66DatabaseNoConnectionError e) {
                    logger.warn("Attempt of connection in error: " + i);
                    continue;
                }
                isReadOnly = false;
                validConnection();
                return;
            }
        } else {
            for (int i = 0; i < RETRYNB; i ++) {
                try {
                    session = new DbSession(this.server, this.user,
                            this.passwd, true);
                } catch (OpenR66DatabaseNoConnectionError e) {
                    logger.warn("Attempt of connection in error: " + i);
                    continue;
                }
                isReadOnly = true;
                validConnection();
                return;
            }
        }
        session = null;
        isConnected = false;
        logger.error("Cannot connect to Database!");
        throw new OpenR66DatabaseNoConnectionError("Cannot connect to database");
    }

    /**
     * Use a default server for basic connection. Later on, specific connection
     * to database for the scheme that provides access to the table R66DbIndex
     * for one specific Legacy could be done.
     *
     * A this time, only one driver is possible! If a new driver is needed, then
     * we need to create a new DbSession object. Be aware that
     * DbSession.initialize should be call only once for each driver, whatever
     * the number of DbSession objects that could be created (=> need a
     * hashtable for specific driver when created). Also, don't know if two
     * drivers at the same time (two different DbSession) is allowed by JDBC.<BR>
     *
     * <B>This version use given connection. typeDriver must be set before !</B>
     *
     * @param conn
     * @param isread
     * @throws OpenR66DatabaseNoConnectionError
     */
    public DbAdmin(Connection conn, boolean isread)
            throws OpenR66DatabaseNoConnectionError {
        server = null;
        if (conn == null) {
            session = null;
            isConnected = false;
            logger.error("Cannot Get a Connection from Datasource");
            throw new OpenR66DatabaseNoConnectionError(
                    "Cannot Get a Connection from Datasource");
        }
        session = new DbSession(conn, isread);
        isReadOnly = isread;
        isConnected = true;
    }
    /**
     * Empty constructor for no Database support (very thin client)
     */
    public DbAdmin() {
        // not true but to enable pseudo database functions
        DbModelFactory.classLoaded = true;
        isConnected = false;
    }
    /**
     * Close the underlying session. Can be call even for connection given from
     * the constructor DbAdmin(Connection, boolean).
     *
     */
    public void close() {
        if (session != null) {
            session.disconnect();
            session = null;
        }
        isConnected = false;
    }

    /**
     * Commit on connection (since in autocommit, should not be used)
     *
     * @throws OpenR66DatabaseNoConnectionError
     * @throws OpenR66DatabaseSqlError
     *
     */
    public void commit() throws OpenR66DatabaseSqlError,
            OpenR66DatabaseNoConnectionError {
        if (session != null) {
            session.commit();
        }
    }

    /**
     * @return the server
     */
    public String getServer() {
        return server;
    }

    /**
     * @return the user
     */
    public String getUser() {
        return user;
    }

    /**
     * @return the passwd
     */
    public String getPasswd() {
        return passwd;
    }

    /**
     * List all Connection to enable the close call on them
     */
    private static ConcurrentHashMap<Long, Connection> listConnection = new ConcurrentHashMap<Long, Connection>();
    /**
     * Add a Connection into the list
     *
     * @param conn
     */
    public static void addConnection(long id, Connection conn) {
        listConnection.put(Long.valueOf(id), conn);
    }

    /**
     * Remove a Connection from the list
     *
     * @param id Id of the connection
     */
    public static void removeConnection(long id) {
        listConnection.remove(Long.valueOf(id));
    }
    /**
     *
     * @return the number of connection (so number of network channels)
     */
    public static int getNbConnection() {
        return listConnection.size()-1;
    }
    /**
     * Close all database connections
     */
    public static void closeAllConnection() {
        for (Connection con : listConnection.values()) {
            try {
                con.close();
            } catch (SQLException e) {
            }
        }
        listConnection.clear();
    }
}
