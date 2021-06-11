package work;

import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.dto.CourseSection;
import cn.edu.sustech.cs307.exception.EntityNotFoundException;
import cn.edu.sustech.cs307.exception.IntegrityViolationException;
import cn.edu.sustech.cs307.service.InstructorService;

import javax.annotation.ParametersAreNonnullByDefault;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
public class DoInstructorService implements InstructorService {
    @Override
    public void addInstructor(int userId, String firstName, String lastName) {
        int uid=new DoUserService().addUser(userId,firstName+","+lastName);
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = connection.prepareStatement("insert into instructor (user_id) values (?);")) {
            stmt.setInt(1, uid);
            stmt.execute();
        } catch (SQLException e) {
            throw new IntegrityViolationException();
        }
    }

    @Override
    public List<CourseSection> getInstructedCourseSections(int instructorId, int semesterId) {
        ArrayList<CourseSection> arrayList=new ArrayList<>();
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = connection.prepareStatement(
                     "select semester_id,section_name,total_capacity,left_capacity from course_section " +
                             "cs join course_section_class csc on cs.id = csc.id and instructor_id=(?) and semester_id=(?);")) {
            stmt.setInt(1, instructorId);
            stmt.setInt(2, semesterId);
            stmt.execute();
            ResultSet resultSet=stmt.getResultSet();
            while (resultSet.next()){
                CourseSection courseSection=new CourseSection();
                courseSection.id=resultSet.getInt(1);
                courseSection.name=resultSet.getString(2);
                courseSection.totalCapacity=resultSet.getInt(3);
                courseSection.leftCapacity=resultSet.getInt(4);
                arrayList.add(courseSection);
            }
        } catch (SQLException e) {
            throw new EntityNotFoundException();
        }
        return arrayList;
    }
}
