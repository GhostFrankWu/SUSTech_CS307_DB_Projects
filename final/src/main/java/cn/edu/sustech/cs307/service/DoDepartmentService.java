package cn.edu.sustech.cs307.service;

import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.dto.Department;

import javax.annotation.ParametersAreNonnullByDefault;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
public class DoDepartmentService implements DepartmentService {
    @Override
    public int addDepartment(String name) {
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = connection.prepareStatement("insert into department (name) values (?);");
             PreparedStatement SQue = connection.prepareStatement("select id from department where (name) = (?);")) {
            stmt.setString(1, name);
            SQue.setString(1, name);
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
    public void removeDepartment(int departmentId) {
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = connection.prepareStatement("delete from department where id= (?);")) {
            stmt.setInt(1, departmentId);
            stmt.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<Department> getAllDepartments() {
        ArrayList<Department> departments = new ArrayList<>();
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = connection.prepareStatement("select * from department;")) {
            stmt.execute();
            ResultSet result=stmt.getResultSet();
            while(result.next()) {
                Department cur=new Department();
                cur.id=result.getInt(1);
                cur.name=result.getString(2);
                departments.add(cur);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return departments;
    }

    @Override
    public Department getDepartment(int departmentId) {
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = connection.prepareStatement("select * from department where id = (?);")) {
            stmt.setInt(1,departmentId);
            stmt.execute();
            ResultSet result=stmt.getResultSet();
            if(result.next()) {
                Department cur=new Department();
                cur.id=result.getInt(1);
                cur.name=result.getString(2);
                return cur;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
