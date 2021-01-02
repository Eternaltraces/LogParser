import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class LogParser {
    public static void main(String[] args) {
        Map<String, Log> logMap = readJsonFile(new File(args[0]));
        addTable(logMap);
        //System.out.println(logMap);

    }

    public static void addTable(Map<String, Log> logMap) {
        int inject = 0;
        final String query = "INSERT INTO LOGS (EVENTID, EVENTDURATION, EVENT_TYPE, HOST, ALERT) VALUES (?, ?, ?, ?, ?)";
        try (Connection con = DriverManager.getConnection("jdbc:hsqldb:file:logdb", "SA", "");
             Statement stmt = con.createStatement()) {
            System.out.println("Connection created successfully");
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS LOGS (EVENTID VARCHAR(255) NOT NULL, EVENTDURATION INTEGER NOT NULL, EVENT_TYPE VARCHAR(25), HOST VARCHAR(25), ALERT BOOLEAN, PRIMARY KEY (EVENTID))");
            PreparedStatement preparedStatement = con.prepareStatement(query);
            for (Map.Entry<String, Log> entry : logMap.entrySet()) {
                Log log = entry.getValue();
                preparedStatement.setString(1, log.getId());
                preparedStatement.setLong(2, log.getRunTime());
                preparedStatement.setString(3, log.getType());
                preparedStatement.setString(4, log.getHost());
                preparedStatement.setBoolean(5, log.isAlert());
                preparedStatement.execute();
            }
            ResultSet select = stmt.executeQuery("SELECT * FROM LOGS");
            System.out.println("------------------------ PRINTING DB ENTRY ----------------------------");
            while (select.next()) {
                System.out.println(select.getString("EVENTID"));
                System.out.println(select.getInt("EVENTDURATION"));
                System.out.println(select.getString("EVENT_TYPE"));
                System.out.println(select.getString("HOST"));
                System.out.println(select.getBoolean("ALERT"));
            }
            System.out.println("------------------------ FINISHED PRINTING DB ENTRY ----------------------------");
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
    }

    public static Map<String, Log> readJsonFile(File file) {
        Map<String, Log> data = new HashMap<>();
        String line;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            while ((line = reader.readLine()) != null) {
                Log log = convertToLog(line);
                if (data.containsKey(log.getId())) {
                    Log mergedLogs = mergeLogs(log, data.get(log.getId()));
                    data.put(log.getId(), mergedLogs);
                } else {
                    data.put(log.getId(), log);
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return data;
    }


    public static Log convertToLog(String line) {
        ObjectMapper mapper = new ObjectMapper();
        Log log = new Log();
        try {
            JsonNode jsonNode = mapper.readTree(line);
            State state = State.valueOf(getValue(jsonNode, "state"));
            log.setId(getValue(jsonNode, "id"));
            log.setHost(getValue(jsonNode, "host"));
            log.setType(getValue(jsonNode, "type"));
            log.setState(state);
            if (State.STARTED == state) {
                log.startedAt(Long.valueOf(getValue(jsonNode, "timestamp")));
            } else if (State.FINISHED == state) {
                log.finishedAt(Long.valueOf(getValue(jsonNode, "timestamp")));
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return log;
    }
    public static String getValue(JsonNode node, String attributeName) {
        return node.has(attributeName) ? node.get(attributeName).asText() : "";

    }

    public static Log mergeLogs(Log log1, Log log2) {
        Log finalLog = null;
        if (State.STARTED == log1.getState() && State.FINISHED == log2.getState()) {
            finalLog = log1;
            finalLog.setFinishedAt(log2.getFinishedAt());

        } else if (State.STARTED == log2.getState() && State.FINISHED == log1.getState()) {
            finalLog = log2;
            finalLog.setFinishedAt(log1.getFinishedAt());
        }
        finalLog.setState(State.COMBINED);
        finalLog.setRunTime(finalLog.getFinishedAt() - finalLog.getStartedAt());

        if ( finalLog.runTime > 4 )  {
            finalLog.setAlert(true);
        }

        return finalLog;
    }

    public static class Log {
        private String id;
        private State state;
        private String type;
        private Long startedAt;
        private Long finishedAt;
        private Long runTime;
        private String host;
        private boolean alert;


        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public State getState() {
            return state;
        }

        public void setState(State state) {
            this.state = state;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Long getStartedAt() {
            return startedAt;
        }

        public void setStartedAt(Long startedAt) {
            this.startedAt = startedAt;
        }

        public Long getFinishedAt() {
            return finishedAt;
        }

        public void setFinishedAt(Long finishedAt) {
            this.finishedAt = finishedAt;
        }

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public void startedAt(Long timestamp) {
            this.startedAt = timestamp;
        }

        public void finishedAt(Long timestamp) {
            this.finishedAt = timestamp;
        }

        public Long getRunTime() {
            return runTime;
        }

        public void setRunTime(Long runTime) {
            this.runTime = runTime;
        }

        public boolean isAlert() {
            return alert;
        }

        public void setAlert(boolean alert) {
            this.alert = alert;
        }

        @Override
        public String toString() {
            return "Log{" +
                    "id='" + id + '\'' +
                    ", state=" + state +
                    ", type='" + type + '\'' +
                    ", startedAt=" + startedAt +
                    ", finishedAt=" + finishedAt +
                    ", runTime=" + runTime +
                    ", host='" + host + '\'' +
                    ", alert=" + alert +
                    '}';
        }
    }

    private enum State {
        STARTED, FINISHED, COMBINED
    }

}