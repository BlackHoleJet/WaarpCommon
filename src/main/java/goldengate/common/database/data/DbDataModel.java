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
package goldengate.common.database.data;

import java.sql.Types;
import java.util.concurrent.ConcurrentHashMap;

import goldengate.common.database.DbPreparedStatement;
import goldengate.common.database.DbSession;
import goldengate.common.database.exception.GoldenGateDatabaseException;
import goldengate.common.database.exception.GoldenGateDatabaseNoConnectionError;
import goldengate.common.database.exception.GoldenGateDatabaseNoDataException;
import goldengate.common.database.exception.GoldenGateDatabaseSqlError;

/**
 * Example of Table object
 *
 * @author Frederic Bregier
 *
 */
public class DbDataModel extends AbstractDbData {
    public static enum Columns {
        READGLOBALLIMIT,
        WRITEGLOBALLIMIT,
        READSESSIONLIMIT,
        WRITESESSIONLIMIT,
        DELAYLIMIT,
        UPDATEDINFO,
        HOSTID
    }

    public static int[] dbTypes = {
            Types.BIGINT, Types.BIGINT, Types.BIGINT, Types.BIGINT,
            Types.BIGINT, Types.INTEGER, Types.VARCHAR };

    public static String table = " CONFIGURATION ";
    public static String fieldseq = "RUNSEQ";
    public static Columns [] indexes = {
        Columns.READGLOBALLIMIT, Columns.READSESSIONLIMIT, Columns.WRITEGLOBALLIMIT, 
        Columns.WRITESESSIONLIMIT, Columns.HOSTID
    };

    /**
     * HashTable in case of lack of database
     */
    private static final ConcurrentHashMap<String, DbDataModel> dbR66ConfigurationHashMap =
        new ConcurrentHashMap<String, DbDataModel>();

    private String hostid;

    private long readgloballimit;

    private long writegloballimit;

    private long readsessionlimit;

    private long writesessionlimit;

    private long delayllimit;

    private int updatedInfo = UpdatedInfo.UNKNOWN.ordinal();

    private boolean isSaved = false;

    // ALL TABLE SHOULD IMPLEMENT THIS
    private final DbValue primaryKey = new DbValue(hostid, Columns.HOSTID
            .name());

    private final DbValue[] otherFields = {
            new DbValue(readgloballimit, Columns.READGLOBALLIMIT.name()),
            new DbValue(writegloballimit, Columns.WRITEGLOBALLIMIT.name()),
            new DbValue(readsessionlimit, Columns.READSESSIONLIMIT.name()),
            new DbValue(writesessionlimit, Columns.WRITESESSIONLIMIT.name()),
            new DbValue(delayllimit, Columns.DELAYLIMIT.name()),
            new DbValue(updatedInfo, Columns.UPDATEDINFO.name()) };

    private final DbValue[] allFields = {
            otherFields[0], otherFields[1], otherFields[2], otherFields[3],
            otherFields[4], otherFields[5], primaryKey };

    public static final String selectAllFields = Columns.READGLOBALLIMIT
            .name() +
            "," +
            Columns.WRITEGLOBALLIMIT.name() +
            "," +
            Columns.READSESSIONLIMIT.name() +
            "," +
            Columns.WRITESESSIONLIMIT.name() +
            "," +
            Columns.DELAYLIMIT.name() +
            "," + Columns.UPDATEDINFO.name() + "," + Columns.HOSTID.name();

    private static final String updateAllFields = Columns.READGLOBALLIMIT
            .name() +
            "=?," +
            Columns.WRITEGLOBALLIMIT.name() +
            "=?," +
            Columns.READSESSIONLIMIT.name() +
            "=?," +
            Columns.WRITESESSIONLIMIT.name() +
            "=?," +
            Columns.DELAYLIMIT.name() +
            "=?," +
            Columns.UPDATEDINFO.name() +
            "=?";

    private static final String insertAllValues = " (?,?,?,?,?,?,?) ";

    @Override
    protected void setToArray() {
        allFields[Columns.HOSTID.ordinal()].setValue(hostid);
        allFields[Columns.READGLOBALLIMIT.ordinal()].setValue(readgloballimit);
        allFields[Columns.WRITEGLOBALLIMIT.ordinal()]
                .setValue(writegloballimit);
        allFields[Columns.READSESSIONLIMIT.ordinal()]
                .setValue(readsessionlimit);
        allFields[Columns.WRITESESSIONLIMIT.ordinal()]
                .setValue(writesessionlimit);
        allFields[Columns.DELAYLIMIT.ordinal()].setValue(delayllimit);
        allFields[Columns.UPDATEDINFO.ordinal()].setValue(updatedInfo);
    }

    @Override
    protected void setFromArray() throws GoldenGateDatabaseSqlError {
        hostid = (String) allFields[Columns.HOSTID.ordinal()].getValue();
        readgloballimit = (Long) allFields[Columns.READGLOBALLIMIT.ordinal()]
                .getValue();
        writegloballimit = (Long) allFields[Columns.WRITEGLOBALLIMIT.ordinal()]
                .getValue();
        readsessionlimit = (Long) allFields[Columns.READSESSIONLIMIT.ordinal()]
                .getValue();
        writesessionlimit = (Long) allFields[Columns.WRITESESSIONLIMIT
                .ordinal()].getValue();
        delayllimit = (Long) allFields[Columns.DELAYLIMIT.ordinal()].getValue();
        updatedInfo = (Integer) allFields[Columns.UPDATEDINFO.ordinal()]
                .getValue();
    }

    /**
     * @param dbSession
     * @param hostid
     * @param rg
     *            Read Global Limit
     * @param wg
     *            Write Global Limit
     * @param rs
     *            Read Session Limit
     * @param ws
     *            Write Session Limit
     * @param del
     *            Delay Limit
     */
    public DbDataModel(DbSession dbSession, String hostid, long rg, long wg, long rs,
            long ws, long del) {
        super(dbSession);
        this.hostid = hostid;
        readgloballimit = rg;
        writegloballimit = wg;
        readsessionlimit = rs;
        writesessionlimit = ws;
        delayllimit = del;
        setToArray();
        isSaved = false;
    }

    /**
     * @param dbSession
     * @param hostid
     * @throws GoldenGateDatabaseException
     */
    public DbDataModel(DbSession dbSession, String hostid) throws GoldenGateDatabaseException {
        super(dbSession);
        this.hostid = hostid;
        // load from DB
        select();
    }

    /*
     * (non-Javadoc)
     *
     * @see openr66.database.data.AbstractDbData#delete()
     */
    @Override
    public void delete() throws GoldenGateDatabaseException {
        if (dbSession == null) {
            dbR66ConfigurationHashMap.remove(this.hostid);
            isSaved = false;
            return;
        }
        DbPreparedStatement preparedStatement = new DbPreparedStatement(
                dbSession);
        try {
            preparedStatement.createPrepareStatement("DELETE FROM " + table +
                    " WHERE " + primaryKey.column + " = ?");
            primaryKey.setValue(hostid);
            setValue(preparedStatement, primaryKey);
            int count = preparedStatement.executeUpdate();
            if (count <= 0) {
                throw new GoldenGateDatabaseNoDataException("No row found");
            }
            isSaved = false;
        } finally {
            preparedStatement.realClose();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see openr66.database.data.AbstractDbData#insert()
     */
    @Override
    public void insert() throws GoldenGateDatabaseException {
        if (isSaved) {
            return;
        }
        if (dbSession == null) {
            dbR66ConfigurationHashMap.put(this.hostid, this);
            isSaved = true;
            return;
        }
        DbPreparedStatement preparedStatement = new DbPreparedStatement(
                dbSession);
        try {
            preparedStatement.createPrepareStatement("INSERT INTO " + table +
                    " (" + selectAllFields + ") VALUES " + insertAllValues);
            setValues(preparedStatement, allFields);
            int count = preparedStatement.executeUpdate();
            if (count <= 0) {
                throw new GoldenGateDatabaseNoDataException("No row found");
            }
            isSaved = true;
        } finally {
            preparedStatement.realClose();
        }
    }

    /* (non-Javadoc)
     * @see openr66.database.data.AbstractDbData#exist()
     */
    @Override
    public boolean exist() throws GoldenGateDatabaseException {
        if (dbSession == null) {
            return dbR66ConfigurationHashMap.containsKey(hostid);
        }
        DbPreparedStatement preparedStatement = new DbPreparedStatement(
                dbSession);
        try {
            preparedStatement.createPrepareStatement("SELECT " +
                    primaryKey.column + " FROM " + table + " WHERE " +
                    primaryKey.column + " = ?");
            primaryKey.setValue(hostid);
            setValue(preparedStatement, primaryKey);
            preparedStatement.executeQuery();
            return preparedStatement.getNext();
        } finally {
            preparedStatement.realClose();
        }
    }
    /*
     * (non-Javadoc)
     *
     * @see openr66.database.data.AbstractDbData#select()
     */
    @Override
    public void select() throws GoldenGateDatabaseException {
        if (dbSession == null) {
            DbDataModel conf = dbR66ConfigurationHashMap.get(this.hostid);
            if (conf == null) {
                throw new GoldenGateDatabaseNoDataException("No row found");
            } else {
                // copy info
                for (int i = 0; i < allFields.length; i++){
                    allFields[i].value = conf.allFields[i].value;
                }
                setFromArray();
                isSaved = true;
                return;
            }
        }
        DbPreparedStatement preparedStatement = new DbPreparedStatement(
                dbSession);
        try {
            preparedStatement.createPrepareStatement("SELECT " +
                    selectAllFields + " FROM " + table + " WHERE " +
                    primaryKey.column + " = ?");
            primaryKey.setValue(hostid);
            setValue(preparedStatement, primaryKey);
            preparedStatement.executeQuery();
            if (preparedStatement.getNext()) {
                getValues(preparedStatement, allFields);
                setFromArray();
                isSaved = true;
            } else {
                throw new GoldenGateDatabaseNoDataException("No row found");
            }
        } finally {
            preparedStatement.realClose();
        }
    }
    /*
     * (non-Javadoc)
     *
     * @see openr66.database.data.AbstractDbData#update()
     */
    @Override
    public void update() throws GoldenGateDatabaseException {
        if (isSaved) {
            return;
        }
        if (dbSession == null) {
            dbR66ConfigurationHashMap.put(this.hostid, this);
            isSaved = true;
            return;
        }
        DbPreparedStatement preparedStatement = new DbPreparedStatement(
                dbSession);
        try {
            preparedStatement.createPrepareStatement("UPDATE " + table +
                    " SET " + updateAllFields + " WHERE " +
                    primaryKey.column + " = ?");
            setValues(preparedStatement, allFields);
            int count = preparedStatement.executeUpdate();
            if (count <= 0) {
                throw new GoldenGateDatabaseNoDataException("No row found");
            }
            isSaved = true;
        } finally {
            preparedStatement.realClose();
        }
    }
    /**
     * Private constructor for Commander only
     */
    private DbDataModel(DbSession session) {
        super(session);
    }
    /**
     * For instance from Commander when getting updated information
     * @param preparedStatement
     * @return the next updated Configuration
     * @throws GoldenGateDatabaseNoConnectionError
     * @throws GoldenGateDatabaseSqlError
     */
    public static DbDataModel getFromStatement(DbPreparedStatement preparedStatement) throws GoldenGateDatabaseNoConnectionError, GoldenGateDatabaseSqlError {
        DbDataModel dbDataModel = new DbDataModel(preparedStatement.getDbSession());
        dbDataModel.getValues(preparedStatement, dbDataModel.allFields);
        dbDataModel.setFromArray();
        dbDataModel.isSaved = true;
        return dbDataModel;
    }
    /**
     *
     * @return the DbPreparedStatement for getting Updated Object
     * @throws GoldenGateDatabaseNoConnectionError
     * @throws GoldenGateDatabaseSqlError
     */
    public static DbPreparedStatement getUpdatedPrepareStament(DbSession session) throws GoldenGateDatabaseNoConnectionError, GoldenGateDatabaseSqlError {
        String request = "SELECT " +selectAllFields;
        request += " FROM "+table+
            " WHERE "+Columns.UPDATEDINFO.name()+" = "+
            AbstractDbData.UpdatedInfo.TOSUBMIT.ordinal();
        return new DbPreparedStatement(session, request);
    }
    /*
     * (non-Javadoc)
     *
     * @see openr66.database.data.AbstractDbData#changeUpdatedInfo(UpdatedInfo)
     */
    @Override
    public void changeUpdatedInfo(UpdatedInfo info) {
        if (updatedInfo != info.ordinal()) {
            updatedInfo = info.ordinal();
            allFields[Columns.UPDATEDINFO.ordinal()].setValue(updatedInfo);
            isSaved = false;
        }
    }
    
}
