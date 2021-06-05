package cn.edu.sustech.cs307.service;

import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.dto.Department;
import cn.edu.sustech.cs307.dto.Major;

import javax.annotation.ParametersAreNonnullByDefault;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
public class DoMajorService implements MajorService {
    @Override
    public int addMajor(String name, int departmentId) {
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = connection.prepareStatement("insert into major (name, department_id) values (?,?);");
             PreparedStatement SQue = connection.prepareStatement("select id from major where (name, department_id) = (?,?);")) {
            stmt.setString(1, name);
            stmt.setInt(2, departmentId);
            SQue.setString(1, name);
            SQue.setInt(2, departmentId);
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
    public void removeMajor(int majorId) {
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = connection.prepareStatement("delete from major where id= (?);")) {
            stmt.setInt(1, majorId);
            stmt.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<Major> getAllMajors() {
        ArrayList<Major> majors = new ArrayList<>();
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = connection.prepareStatement("select * from major;");
             PreparedStatement SQue = connection.prepareStatement("select * from department where id = (?);")) {
            stmt.execute();
            ResultSet result=stmt.getResultSet();
            while(result.next()) {
                Major cur=new Major();
                cur.id=result.getInt(1);
                cur.name=result.getString(3);
                SQue.setInt(1, result.getInt(2));
                SQue.execute();
                ResultSet res=SQue.getResultSet();
                Department department=new Department();
                department.id=res.getInt(1);
                department.name=res.getString(2);
                cur.department=department;
                majors.add(cur);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return majors;
    }

    @Override
    public Major getMajor(int majorId) {
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = connection.prepareStatement("select * from major where id = (?);");
             PreparedStatement SQue = connection.prepareStatement("select * from department where id = (?);")) {
            stmt.setInt(1,majorId);
            stmt.execute();
            ResultSet result=stmt.getResultSet();
            if(result.next()) {
                Major cur=new Major();
                cur.id=result.getInt(1);
                cur.name=result.getString(3);
                SQue.setInt(1, result.getInt(2));
                SQue.execute();
                ResultSet res=SQue.getResultSet();
                Department department=new Department();
                department.id=res.getInt(1);
                department.name=res.getString(2);
                cur.department=department;
                return cur;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void addMajorCompulsoryCourse(int majorId, String courseId) {
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = connection.prepareStatement(
                     "insert into major_course (major_id, course_id,is_major_elective) values (?,?,?);")) {
            stmt.setInt(1, majorId);
            stmt.setString(2, courseId);
            stmt.setBoolean(3, false);
            stmt.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void addMajorElectiveCourse(int majorId, String courseId) {
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = connection.prepareStatement(
                     "insert into major_course (major_id, course_id,is_major_elective) values (?,?,?);")) {
            stmt.setInt(1, majorId);
            stmt.setString(2, courseId);
            stmt.setBoolean(3, true);
            stmt.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
