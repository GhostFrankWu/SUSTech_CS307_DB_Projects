package cn.edu.sustech.cs307.service;

import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.dto.*;
import cn.edu.sustech.cs307.dto.prerequisite.Prerequisite;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.sql.*;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static cn.edu.sustech.cs307.dto.Course.CourseGrading.HUNDRED_MARK_SCORE;
import static cn.edu.sustech.cs307.dto.Course.CourseGrading.PASS_OR_FAIL;

@ParametersAreNonnullByDefault
public class DoCourseService implements CourseService {
    /**
     * Add one course according to following parameters.
     * If some of parameters are invalid, throw {@link cn.edu.sustech.cs307.exception.IntegrityViolationException}
     *
     * @param courseId     represents the id of course. For example, CS307, CS309
     * @param courseName   the name of course
     * @param credit       the credit of course
     * @param classHour    The total teaching hour that the course spends.
     * @param grading      the grading type of course
     * @param prerequisite The root of a {@link cn.edu.sustech.cs307.dto.prerequisite.Prerequisite} expression tree.
     */
    public void addCourse(String courseId, String courseName, int credit, int classHour,
                          Course.CourseGrading grading, @Nullable Prerequisite prerequisite) {
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = connection.prepareStatement(
                     "insert into course (id, name, credit, class_hour, grading) VALUES (?,?,?,?,?);")) {
            stmt.setString(1, courseId);
            stmt.setString(2, courseName);
            stmt.setInt(3, credit);
            stmt.setInt(4, classHour);
            stmt.setBoolean(5, grading == Course.CourseGrading.HUNDRED_MARK_SCORE);
            stmt.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int addCourseSection(String courseId, int semesterId, String sectionName, int totalCapacity) {
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = connection.prepareStatement(
                     "insert into course_section (semester_id, name, section_name, totalCapacity, leftCapacity) VALUES (?,?,?,?,?);");
             PreparedStatement SQue = connection.prepareStatement(
                     "select id from course_section where (semester_id, name, section_name, totalCapacity, leftCapacity) = (?,?,?,?,?);")) {

            stmt.setInt(1, semesterId);
            stmt.setString(2, courseId);
            stmt.setString(3, sectionName);
            stmt.setInt(4, totalCapacity);
            stmt.setInt(5, totalCapacity);
            SQue.setInt(1, semesterId);
            SQue.setString(2, courseId);
            SQue.setString(3, sectionName);
            SQue.setInt(4, totalCapacity);
            SQue.setInt(5, totalCapacity);
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
    public int addCourseSectionClass(int sectionId, int instructorId, DayOfWeek dayOfWeek, Set<Short> weekList, short classStart, short classEnd, String location) {
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = connection.prepareStatement(
                     "insert into course_section_class (id, instructor_id, day_of_week, week_list, begin, \"end\", location) VALUES (?,?,?,?,?,?,?)");
             PreparedStatement SQue = connection.prepareStatement(
                     "select serial_id from course_section_class where (id, instructor_id, day_of_week, week_list, begin, \"end\", location)= (?,?,?,?,?,?,?);")) {
            stmt.setInt(1, sectionId);
            stmt.setInt(2, instructorId);
            stmt.setString(3, dayOfWeek.toString());
            Short[] shorts=weekList.toArray(new Short[0]);
            Array arr=connection.createArrayOf("int",shorts);
            stmt.setArray(4, arr);
            stmt.setInt(5, classStart);
            stmt.setInt(6, classEnd);
            stmt.setString(7, location);
            SQue.setInt(1, sectionId);
            SQue.setInt(2, instructorId);
            SQue.setString(3, dayOfWeek.toString());
            SQue.setArray(4, arr);
            SQue.setInt(5, classStart);
            SQue.setInt(6, classEnd);
            SQue.setString(7, location);
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
    public void removeCourse(String courseId) {
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = connection.prepareStatement(
                     "delete from course where (id) = (?);")) {
            stmt.setString(1, courseId);
            stmt.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void removeCourseSection(int sectionId) {
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = connection.prepareStatement(
                     "delete from course_section where (id) = (?);")) {
            stmt.setInt(1, sectionId);
            stmt.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void removeCourseSectionClass(int classId) {
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = connection.prepareStatement(
                     "delete from course_section_class where (id) = (?);")) {
            stmt.setInt(1, classId);
            stmt.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<Course> getAllCourses() {
        ArrayList<Course> courses = new ArrayList<>();
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = connection.prepareStatement("select * from course;")) {
            stmt.execute();
            ResultSet result=stmt.getResultSet();
            while(result.next()) {
                Course cur=new Course();
                cur.id=result.getString(1);
                cur.name=result.getString(2);
                cur.credit=result.getInt(3);
                cur.classHour=result.getInt(4);
                courses.add(cur);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return courses;
    }

    @Override
    public List<CourseSection> getCourseSectionsInSemester(String courseId, int semesterId) {
        ArrayList<CourseSection> courseSections = new ArrayList<>();
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = connection.prepareStatement("select * from course_section where (name,semester_id)= (?,?);")) {
            stmt.setString(1, courseId);
            stmt.setInt(1, semesterId);
            stmt.execute();
            ResultSet result=stmt.getResultSet();
            while(result.next()) {
                CourseSection cur=new CourseSection();
                cur.id=result.getInt(2);
                cur.name=result.getString(4);
                cur.totalCapacity=result.getInt(5);
                cur.leftCapacity=result.getInt(6);
                courseSections.add(cur);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return courseSections;
    }

    @Override
    public Course getCourseBySection(int sectionId) {
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = connection.prepareStatement("select name from course_section where id=(?);");
             PreparedStatement SQue = connection.prepareStatement("select * from course where id=(?);")) {
            stmt.setInt(1, sectionId);
            stmt.execute();
            ResultSet result=stmt.getResultSet();
            if(result.next()) {
                SQue.setString(1, result.getString(1));
                SQue.execute();
                ResultSet res=SQue.getResultSet();
                Course cur=new Course();
                cur.id=res.getString(1);
                cur.name=res.getString(2);
                cur.credit=res.getInt(3);
                cur.classHour=res.getInt(4);
                cur.grading=res.getBoolean(5)?HUNDRED_MARK_SCORE:PASS_OR_FAIL;
                return cur;
            }else{
                //todo raise error
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<CourseSectionClass> getCourseSectionClasses(int sectionId) {
        ArrayList<CourseSectionClass> courseSectionClasses = new ArrayList<>();
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = connection.prepareStatement("select name from course_section_class where id=(?);");
             PreparedStatement SQue = connection.prepareStatement("select user_id from instructor where id=(?);");
             PreparedStatement NQue = connection.prepareStatement("select * from users where id=(?);")) {
            stmt.setInt(1, sectionId);
            stmt.execute();
            ResultSet result=stmt.getResultSet();
            if(result.next()) {
                CourseSectionClass cur=new CourseSectionClass();
                SQue.setInt(1, result.getInt(3));
                SQue.execute();
                ResultSet res=SQue.getResultSet();
                NQue.setInt(1, res.getInt(1));//user_id
                NQue.execute();
                ResultSet r=NQue.getResultSet();
                Instructor instructor=new Instructor();
                instructor.id=r.getInt(1);
                instructor.fullName=r.getString(2);
                cur.id=res.getInt(2);
                cur.instructor=instructor;
                cur.dayOfWeek=DayOfWeek.valueOf(res.getString(4));
                Array arr=res.getArray(5);
                ResultSet rs=arr.getResultSet();
                HashSet<Short> set=new HashSet<>();
                while (rs.next()){
                    set.add(rs.getShort(1));
                }
                cur.weekList=set;
                cur.classBegin=res.getShort(6);
                cur.classEnd=res.getShort(7);
                cur.location=res.getString(8);
                courseSectionClasses.add(cur);
            }else{
                //todo raise error
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return courseSectionClasses;
    }

    @Override
    public CourseSection getCourseSectionByClass(int classId) {
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = connection.prepareStatement("select * from course_section where id = (?);")) {
            stmt.setInt(1,classId);
            stmt.execute();
            ResultSet result=stmt.getResultSet();
            if(result.next()) {
                CourseSection cur=new CourseSection();
                cur.id=result.getInt(2);
                cur.name=result.getString(4);
                cur.totalCapacity=result.getInt(5);
                cur.leftCapacity=result.getInt(6);
                return cur;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<Student> getEnrolledStudentsInSemester(String courseId, int semesterId) {
        return null;
    }


    /**
     * Add one course section class according to following parameters:
     * If some of parameters are invalid, throw {@link cn.edu.sustech.cs307.exception.IntegrityViolationException}
     *
     * @param sectionId
     * @param instructorId
     * @param dayOfWeek
     * @param weekList
     * @param classStart
     * @param classEnd
     * @param location
     * @return the CourseSectionClass id of new inserted line.
     */
    int addCourseSectionClass(int sectionId, int instructorId, DayOfWeek dayOfWeek, List<Short> weekList,
                              short classStart, short classEnd, String location){
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = connection.prepareStatement(
                     "insert into course_section_class (id, instructor_id, day_of_week, week_list, begin, \"end\", location) VALUES (?,?,?,?,?,?,?)");
             PreparedStatement SQue = connection.prepareStatement(
                     "select serial_id from course_section_class where (id, instructor_id, day_of_week, week_list, begin, \"end\", location)= (?,?,?,?,?,?,?);")) {
            stmt.setInt(1, sectionId);
            stmt.setInt(2, instructorId);
            stmt.setString(3, dayOfWeek.toString());
            Short[] shorts=weekList.toArray(new Short[0]);
            Array arr=connection.createArrayOf("int",shorts);
            stmt.setArray(4, arr);
            stmt.setInt(5, classStart);
            stmt.setInt(6, classEnd);
            stmt.setString(7, location);
            SQue.setInt(1, sectionId);
            SQue.setInt(2, instructorId);
            SQue.setString(3, dayOfWeek.toString());
            SQue.setArray(4, arr);
            SQue.setInt(5, classStart);
            SQue.setInt(6, classEnd);
            SQue.setString(7, location);
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
    };



}
