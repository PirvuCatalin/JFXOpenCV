package Database;
import java.sql.*;
import java.lang.String;
import Database.Database;
/**
 * Created by Bogdan on 4/15/2018.
 */
public class Database {

    public static void getAllEntries (String toFind) {

        try {
            /* Load the Driver for the oracle Database */
            Class.forName("oracle.jdbc.OracleDriver");
            /* This query is for creatin the database, will be required when used the first time */
            String tableCreateQuery = "create table fastpeco(username varchar2(10), password varchar2(10), " +
                    "licenceplate varchar2(10))";
            /* Simple inser query to test the functionality */
            String insertQuery = "insert into fastpeco values('username', 'password', 'AR18AUG')";
            /* Set the connection to the database */
            Connection con = DriverManager.getConnection(
                    "jdbc:oracle:thin:@localhost:1521:orcl", "system", "Amilar123");
            /* Statement body for the execution of statements */
            Statement stmt=con.createStatement();
            //stmt.execute(tableCreateQuery);
            //stmt.execute(insertQuery);
            /* Created table and inserted a value, now trying to retrieve the rows */
            ResultSet rs=stmt.executeQuery("select * from fastpeco");
            /* Successfully connected to the database, retrieving the rows */
            System.out.println(toFind);
            while(rs.next()) {
                System.out.println(rs.getString(1) + "  " + rs.getString(2) + "  " + rs.getString(3));
                if (rs.getString(3).equalsIgnoreCase(toFind) ) {
                    System.out.println("I found the licence plate in the database");
                }
            }
            con.close();
        } catch (Exception e) {
            System.out.println(e);
        }
    }

}
