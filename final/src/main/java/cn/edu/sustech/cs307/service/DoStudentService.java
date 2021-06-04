package cn.edu.sustech.cs307.service;

import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.dto.*;
import cn.edu.sustech.cs307.dto.grade.Grade;
import cn.edu.sustech.cs307.dto.grade.HundredMarkGrade;
import cn.edu.sustech.cs307.dto.grade.PassOrFailGrade;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.sql.*;
import java.time.DayOfWeek;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ParametersAreNonnullByDefault
public class DoStudentService implements StudentService {
    @Override
    public void addStudent(int userId, int majorId, String firstName, String lastName, Date enrolledDate) {
        int uid=new DoUserService().addUser(userId,firstName+","+lastName);
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = connection.prepareStatement("insert into student (id,enrolled_date,major_id) values (?,?,?);")) {
            stmt.setInt(1, uid);
            stmt.setDate(2, enrolledDate);
            stmt.setInt(3, majorId);
            stmt.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<CourseSearchEntry> searchCourse(int studentId, int semesterId, @Nullable String searchCid, @Nullable String searchName, @Nullable String searchInstructor, @Nullable DayOfWeek searchDayOfWeek, @Nullable Short searchClassTime, @Nullable List<String> searchClassLocations, CourseType searchCourseType, boolean ignoreFull, boolean ignoreConflict, boolean ignorePassed, boolean ignoreMissingPrerequisites, int pageSize, int pageIndex) {
        return null;
    }

    @Override
    public EnrollResult enrollCourse(int studentId, int sectionId) {
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement checkCourseExists = connection.prepareStatement(
                     "select * from course_section_class where (serial_id) = (?);");
             PreparedStatement checkCourseSelected = connection.prepareStatement(
                     "select * from course_select where (stu_id, course_section_class_id) = (?,?) and grade is null;");
             PreparedStatement checkCoursePassed = connection.prepareStatement(
                     "select * from course_select where (stu_id, course_section_class_id) = (?,?) and grade >= 60;");
             PreparedStatement stmt = connection.prepareStatement(
                     "insert into course_select (stu_id, course_section_class_id) values (?,?);")) {
            checkCourseExists.setInt(1,sectionId);
            checkCourseExists.execute();
            if(!checkCourseExists.getResultSet().next()){
                return EnrollResult.COURSE_NOT_FOUND;
            }

            checkCourseSelected.setInt(1,studentId);
            checkCourseSelected.setInt(2,sectionId);
            checkCourseSelected.execute();
            if(checkCourseSelected.getResultSet().next()){
                return EnrollResult.ALREADY_ENROLLED;
            }

            checkCourseSelected.setInt(1,studentId);
            checkCourseSelected.setInt(2,sectionId);
            checkCourseSelected.execute();
            if(checkCourseSelected.getResultSet().next()){
                return EnrollResult.ALREADY_PASSED;
            }

            //fixme check valid
            /*
                EnrollResult.PREREQUISITES_NOT_FULFILLED;
                EnrollResult.COURSE_CONFLICT_FOUND;
                EnrollResult.COURSE_IS_FULL;
            */

            stmt.setInt(1, studentId);
            stmt.setInt(2, sectionId);
            stmt.execute();
        } catch (SQLException e) {
            e.printStackTrace();
            return EnrollResult.UNKNOWN_ERROR;
        }
        return EnrollResult.SUCCESS;
    }

    @Override
    public void dropCourse(int studentId, int sectionId) throws IllegalStateException {
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = connection.prepareStatement(
                     "delete from course_select where (stu_id,course_section_class_id)= (?,?) and grade is null;")) {
            stmt.setInt(1, studentId);
            stmt.setInt(2, sectionId);
            stmt.execute();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new IllegalStateException();
        }
    }

    @Override
    public void addEnrolledCourseWithGrade(int studentId, int sectionId, @Nullable Grade grade) {
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = connection.prepareStatement(
                     "insert into course_select (stu_id, course_section_class_id,grade) values (?,?,?)")) {
            stmt.setInt(1, studentId);
            stmt.setInt(2, sectionId);
            if(grade instanceof HundredMarkGrade) {
                HundredMarkGrade hundredMarkGrade= (HundredMarkGrade) grade;
                stmt.setShort(3, hundredMarkGrade.mark);
            }else{
                PassOrFailGrade passOrFailGrade= (PassOrFailGrade) grade;
                stmt.setShort(3, (short) (passOrFailGrade==PassOrFailGrade.PASS?60:0));
            }
            stmt.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setEnrolledCourseGrade(int studentId, int sectionId, Grade grade) {
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = connection.prepareStatement(
                     "update course_select set grade=(?) where (stu_id, course_section_class_id)=(?,?);")) {
            stmt.setInt(2, studentId);
            stmt.setInt(3, sectionId);
            if(grade instanceof HundredMarkGrade) {
                HundredMarkGrade hundredMarkGrade= (HundredMarkGrade) grade;
                stmt.setShort(1, hundredMarkGrade.mark);
            }else{
                PassOrFailGrade passOrFailGrade= (PassOrFailGrade) grade;
                stmt.setShort(1, (short) (passOrFailGrade==PassOrFailGrade.PASS?60:0));
            }
            stmt.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Map<Course, Grade> getEnrolledCoursesAndGrades(int studentId, @Nullable Integer semesterId) {
        HashMap<Course, Grade> map=new HashMap<>();
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = connection.prepareStatement("select * from course_select where (stu_id) = (?);");
             PreparedStatement SQue = connection.prepareStatement("select * from course c join " +
                     "(select cs.name from course_section cs join course_section_class csc on cs.id = csc.id " +
                     " and csc.serial_id=(?) )j on c.name=j.name;");) {
            stmt.setInt(1, studentId);
            stmt.execute();
            ResultSet result=stmt.getResultSet();
            while (result.next()){
                SQue.setInt(1,result.getInt(2));
                SQue.execute();
                ResultSet res=SQue.getResultSet();
                Course course=new Course();
                course.id=res.getString(1);
                course.credit=res.getInt(2);
                course.classHour=res.getInt(3);
                course.grading=res.getBoolean(4)? Course.CourseGrading.HUNDRED_MARK_SCORE: Course.CourseGrading.PASS_OR_FAIL;
                if(res.getBoolean(4)) {
                    HundredMarkGrade hundredMarkGrade=new HundredMarkGrade(res.getShort(3));
                    map.put(course, hundredMarkGrade);
                }else{
                    map.put(course, res.getShort(3)>=60?PassOrFailGrade.PASS:PassOrFailGrade.FAIL);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return map;
    }

    @Override
    public CourseTable getCourseTable(int studentId, Date date) {
        return null;
    }

    @Override
    public boolean passedPrerequisitesForCourse(int studentId, String courseId) {
        return false;
    }

    @Override
    public Major getStudentMajor(int studentId) {
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = connection.prepareStatement("select major_id from student where id = (?);");
             PreparedStatement SQue = connection.prepareStatement("select * from major where id = (?);");
             PreparedStatement NQue = connection.prepareStatement("select * from department where id = (?);")) {
            stmt.setInt(1,studentId);
            stmt.execute();
            ResultSet result=stmt.getResultSet();
            if(result.next()) {
                SQue.setInt(1,studentId);
                SQue.execute();
                ResultSet res=SQue.getResultSet();
                Major cur=new Major();
                cur.id=res.getInt(1);
                cur.name=res.getString(3);
                NQue.setInt(1, res.getInt(2));
                NQue.execute();
                ResultSet r=NQue.getResultSet();
                Department department=new Department();
                department.id=r.getInt(1);
                department.name=r.getString(2);
                cur.department=department;
                return cur;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
