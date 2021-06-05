package cn.edu.sustech.cs307.service;

import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.dto.Semester;

import javax.annotation.ParametersAreNonnullByDefault;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
public class DoSemesterService implements SemesterService {
    @Override
    public int addSemester(String name, Date begin, Date end) {
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = connection.prepareStatement("insert into semester (name,begin,\"end\") values (?,?,?);");
             PreparedStatement SQue = connection.prepareStatement("select id from semester where (name,begin,\"end\") = (?,?,?);")) {
            stmt.setString(1, name);
            stmt.setDate(2, begin);
            stmt.setDate(3, end);
            SQue.setString(1, name);
            SQue.setDate(2, begin);
            SQue.setDate(3, end);
            stmt.execute();
            SQue.execute();
            ResultSet result=SQue.getResultSet();
            if(result.next()) {
                return result.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    @Override
    public void removeSemester(int semesterId) {
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = connection.prepareStatement("delete from semester where id= (?);")) {
            stmt.setInt(1, semesterId);
            stmt.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<Semester> getAllSemesters() {
        ArrayList<Semester> semesters = new ArrayList<>();
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = connection.prepareStatement("select * from semesters;")) {
            stmt.execute();
            ResultSet result=stmt.getResultSet();
            while(result.next()) {
                Semester cur=new Semester();
                cur.id=result.getInt(1);
                cur.name=result.getString(2);
                cur.begin=result.getDate(3);
                cur.end=result.getDate(4);
                semesters.add(cur);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return semesters;
    }

    @Override
    public Semester getSemester(int semesterId) {
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = connection.prepareStatement("select * from semesters where id = (?);")) {
            stmt.setInt(1,semesterId);
            stmt.execute();
            ResultSet result=stmt.getResultSet();
            if(result.next()) {
                Semester cur=new Semester();
                cur.id=result.getInt(1);
                cur.name=result.getString(2);
                cur.begin=result.getDate(3);
                cur.end=result.getDate(4);
                return cur;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
