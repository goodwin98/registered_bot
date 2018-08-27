package Register;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DataBase {

    private static final Logger log = LoggerFactory.getLogger(DataBase.class);

    private Statement statement;
    private static Connection connection = null;

    public DataBase() {
        statement = ConnectDB();
        //TODO исключение при создании БД

        try {
            createTables();
        } catch (SQLException e) {
            log.error("DataBase not create", e);
        }

    }

    private Statement ConnectDB() {
        Statement statement = null;
        try {
            Class.forName("org.sqlite.JDBC");

            if (connection == null) {
                connection = DriverManager.getConnection("jdbc:sqlite:register.db");
            }
            statement = connection.createStatement();
            statement.setQueryTimeout(30);  // set timeout to 30 sec.

        } catch (SQLException e) {
            log.error("Error in create database", e);
        } catch (ClassNotFoundException e) {
            log.error("Class DataBase not found", e);
        }

        return statement;

    }

    private void createTables() throws SQLException {

        String sqlCreateEvents = "CREATE TABLE IF NOT EXISTS events" +
                "(" +
                "    id integer PRIMARY KEY AUTOINCREMENT NOT NULL," +
                "    discordChannel INTEGER NOT NULL UNIQUE," +
                "    description TEXT," +
                "    isEventEnd INTEGER DEFAULT 0" +
                ");";
        String sqlCreateUser = "CREATE TABLE IF NOT EXISTS userForms (" +
                "    id      INTEGER PRIMARY KEY AUTOINCREMENT," +
                "    discordID  INTEGER  NOT NULL," +
                "    discordName TEXT NOT NULL," +
                "    event INTEGER  NOT NULL," +
                "    bfName TEXT  NOT NULL," +
                "    squadName TEXT ," +
                "    isSquad INTEGER NOT NULL," +
                "    numInSquad INTEGER," +
                "    other TEXT," +
                "    dateOfRegister INTEGER," +
                "    CONSTRAINT user_event UNIQUE (discordID, event)" +
                ");";


        statement.executeUpdate(sqlCreateEvents);
        statement.executeUpdate(sqlCreateUser);


    }

    public boolean createEvent(String description, long channelID) {
        String sqlInsert = "insert into events(description, discordChannel) values (?,?);";

        try {
            PreparedStatement smt = connection.prepareStatement(sqlInsert);
            smt.setString(1, description);
            smt.setLong(2, channelID);
            smt.executeUpdate();
            return true;

        } catch (SQLException e) {

            return false;
        }
    }

    public boolean deleteEvent(long channelID) {
        String sqlDelete = "delete from userForms where event = (SELECT id from events where discordChannel = ?); ";
        String sqlDelete2 = "delete from events where discordChannel = ?;";
        try {
            PreparedStatement smt = connection.prepareStatement(sqlDelete);
            smt.setLong(1, channelID);
            smt.execute();
            smt = connection.prepareStatement(sqlDelete2);
            smt.setLong(1, channelID);
            return smt.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("Error with delete event", e);
            return false;
        }
    }

    public int getIdEvent(long channel) {
        String sqlSelect = "select id from events where discordChannel = ? and isEventEnd = 0;";
        try {
            PreparedStatement smt = connection.prepareStatement(sqlSelect);
            smt.setLong(1, channel);
            ResultSet row = smt.executeQuery();

            if (row.next()) {
                return row.getInt("id");
            } else {
                return -1;
            }
        } catch (SQLException e) {
            log.error("Error with get id by channel", e);
            return -1;
        }
    }

    String getDescription(int id) {
        String sqlSelect = "select description from events where id = ?;";
        try {
            PreparedStatement smt = connection.prepareStatement(sqlSelect);
            smt.setLong(1, id);
            ResultSet row = smt.executeQuery();

            if (row.next()) {
                return row.getString("description");
            } else {
                return "";
            }
        } catch (SQLException e) {
            log.error("Error with description by id", e);
            return "";
        }

    }

    boolean saveForm(Answer answer, int eventID) {
        String sqlInsert = "insert into userForms(discordID, discordName, event, bfName, squadName, isSquad, other,numInSquad, dateOfRegister)" +
                " values (?,?,?,?,?,?,?,?,strftime('%s','now'));";
        try {
            PreparedStatement smt = connection.prepareStatement(sqlInsert);
            smt.setLong(1, answer.discordID);
            smt.setString(2, answer.discordName);
            smt.setInt(3, eventID);
            smt.setString(4, answer.bfName);
            smt.setString(5, answer.squadName);
            smt.setBoolean(6, answer.isSquad);
            smt.setString(7, answer.other);
            smt.setInt(8, answer.numInSquad);
            smt.executeUpdate();
            return true;
        } catch (SQLException e) {
            log.error("NOT SAVED ANSWER OF USER " + answer.discordName + " TO DataBase ", e);
            return false;
        }
    }

    public boolean isUserAlreadyRegister(long userID, int eventID) {
        String sqlSelect = "select id from userForms where discordID = ? and event = ?;";

        try {
            PreparedStatement smt = connection.prepareStatement(sqlSelect);
            smt.setLong(1, userID);
            smt.setInt(2, eventID);
            ResultSet row = smt.executeQuery();

            return row.next();
        } catch (SQLException e) {
            log.error("Error with checking user", e);
            return true;
        }
    }

    public List<Answer> getAllAnswer(long eventChan) {
        String sqlSelect = "select discordID, discordName, bfName, squadName, isSquad,numInSquad, other from userForms where event = (select id from events where discordChannel = ?) ORDER BY dateOfRegister;";
        List<Answer> result = new ArrayList<>();
        try {
            PreparedStatement smt = connection.prepareStatement(sqlSelect);
            smt.setLong(1, eventChan);

            ResultSet row = smt.executeQuery();
            while (row.next()) {
                Answer answer = new Answer();
                answer.discordID = row.getLong("discordID");
                answer.discordName = row.getString("discordName");
                answer.bfName = row.getString("bfName");
                answer.squadName = row.getString("squadName");
                answer.isSquad = row.getBoolean("isSquad");
                answer.numInSquad = row.getInt("numInSquad");
                answer.other = row.getString("other");
                result.add(answer);
            }

        } catch (SQLException e) {
            log.error("error with get of all answers", e);
        }

        return result;
    }

    public boolean deleteUserFromEvent(long user, long channelID) {
        String sqlDelete = "delete from userForms where discordID = ? and event = (SELECT id from events where discordChannel = ?); ";

        try {
            PreparedStatement smt = connection.prepareStatement(sqlDelete);
            smt.setLong(1, user);
            smt.setLong(2, channelID);
            return smt.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("error with deleting user", e);
            return false;
        }
    }

    public List<event> getListEvents() {
        List<event> result = new ArrayList<>();
        String sqlSelect = "select discordChannel, description from events where isEventEnd = 0;";

        try {
            PreparedStatement smt = connection.prepareStatement(sqlSelect);
            ResultSet row = smt.executeQuery();
            while (row.next()) {
                event ev = new event();
                ev.channelID = row.getLong("discordChannel");
                ev.description = row.getString("description");
                result.add(ev);
            }
        } catch (SQLException e) {
            log.error("Error with get list of events");
        }

        return result;
    }
}
