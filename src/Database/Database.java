package Database;
import java.sql.*;
/**
 * Created by Bogdan on 4/15/2018.
 */
public class Database {

    public static void getAllEntries () {

        try {
            Class.forName("oracle.jdbc.OracleDriver");

            Connection con = DriverManager.getConnection(
                    "jdbc:oracle:thin:@localhost:1521:orcl", "system", "Amilar123");

            Statement stmt=con.createStatement();

            ResultSet rs=stmt.executeQuery("select * from peco");
            System.out.println("I connected to the database, now trying to get the rows");
            while(rs.next())
                System.out.println(rs.getInt(1)+"  "+rs.getString(2)+"  "+rs.getString(3));
            con.close();
        } catch (Exception e) {
            System.out.println(e);
        }
    }

}
