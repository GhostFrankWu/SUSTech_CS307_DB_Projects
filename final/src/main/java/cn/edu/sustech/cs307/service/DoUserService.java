package cn.edu.sustech.cs307.service;

import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.dto.Instructor;
import cn.edu.sustech.cs307.dto.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DoUserService implements UserService {

    public int addUser(int userId,String name) {
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = connection.prepareStatement("insert into users (id, fullname) values (?,?);");
             PreparedStatement SQue = connection.prepareStatement("select id from users where (id, fullname) = (?,?);")) {
            stmt.setInt(1, userId);
            stmt.setString(2, name);
            SQue.setInt(1, userId);
            SQue.setString(2, name);
            stmt.execute();
            SQue.execute();
            ResultSet result=SQue.getResultSet();
            if(result.next()) {
                return result.getInt(1);
            }else{
                //todo raise ERROR
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    @Override
    public void removeUser(int userId) {
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = connection.prepareStatement("delete from users where id= (?);")) {
            stmt.setInt(1, userId);
            stmt.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<User> getAllUsers() {
        ArrayList<User> users = new ArrayList<>();
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = connection.prepareStatement("select * from users;")) {
            stmt.execute();
            ResultSet result=stmt.getResultSet();
            while(result.next()) {
                User cur=new Instructor();
                cur.id=result.getInt(1);
                cur.fullName=result.getString(2);
                users.add(cur);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }

    @Override
    public User getUser(int userId) {
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = connection.prepareStatement("select * from users where id = (?);")) {
            stmt.setInt(1,userId);
            stmt.execute();
            ResultSet result=stmt.getResultSet();
            if(result.next()) {
                User cur=new Instructor();
                cur.id=result.getInt(1);
                cur.fullName=result.getString(2);
                return cur;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
