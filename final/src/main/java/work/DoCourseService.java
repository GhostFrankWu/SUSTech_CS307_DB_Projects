package work;

import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.dto.*;
import cn.edu.sustech.cs307.dto.prerequisite.AndPrerequisite;
import cn.edu.sustech.cs307.dto.prerequisite.CoursePrerequisite;
import cn.edu.sustech.cs307.dto.prerequisite.OrPrerequisite;
import cn.edu.sustech.cs307.dto.prerequisite.Prerequisite;
import cn.edu.sustech.cs307.exception.EntityNotFoundException;
import cn.edu.sustech.cs307.exception.IntegrityViolationException;
import cn.edu.sustech.cs307.service.CourseService;

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

    private int addBasicPrerequisite(Prerequisite prerequisite) {
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = connection.prepareStatement("insert into list_prerequisite (type) values (1);;");
             PreparedStatement SQue = connection.prepareStatement("select count(*) from list_prerequisite;");
             PreparedStatement NQue = connection.prepareStatement(
                     "insert into basic_prerequisite (id,course_id) values (?,?);")) {
            stmt.execute();
            SQue.execute();

            ResultSet result=SQue.getResultSet();
            result.next();
            NQue.setInt(1, result.getInt(1));
            CoursePrerequisite p= (CoursePrerequisite) prerequisite;
            NQue.setString(2,p.courseID);
            NQue.execute();
            return result.getInt(1);
        } catch (SQLException  e) {
            throw new IntegrityViolationException();
        }
    }

    private int addAndOrPrerequisite(ArrayList<Integer> prerequisite,boolean isAndRelation) {
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = connection.prepareStatement(isAndRelation?
                     "insert into list_prerequisite (type) values (2);":"insert into list_prerequisite (type) values (3);");
             PreparedStatement SQue = connection.prepareStatement("select count(*) from list_prerequisite;");
             PreparedStatement NQue = connection.prepareStatement(
                     "insert into "+(isAndRelation?"and_prerequisite":"or_prerequisite") +"(id,terms) values (?,?);")) {
            stmt.execute();
            SQue.execute();
            ResultSet result=SQue.getResultSet();
            result.next();
            NQue.setInt(1,result.getInt(1));
            Integer[] integers=prerequisite.toArray(new Integer[0]);
            Array arr=connection.createArrayOf("int",integers);
            NQue.setArray(2,arr);
            NQue.execute();
            return result.getInt(1);
        } catch (SQLException  e) {
            throw new IntegrityViolationException();
        }
    }

    private int handlePrerequisite(Prerequisite prerequisite){
        if(prerequisite instanceof CoursePrerequisite){
            return addBasicPrerequisite(prerequisite);
        }else if(prerequisite instanceof AndPrerequisite){
            ArrayList<Integer> ids=new ArrayList<>();
            AndPrerequisite p= (AndPrerequisite) prerequisite;
            for (Prerequisite it:p.terms){
                ids.add(handlePrerequisite(it));
            }
            return addAndOrPrerequisite(ids,true);
        }else {
            ArrayList<Integer> ids=new ArrayList<>();
            OrPrerequisite p= (OrPrerequisite) prerequisite;
            for (Prerequisite it:p.terms){
                ids.add(handlePrerequisite(it));
            }
            return addAndOrPrerequisite(ids,false);
        }
    }


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
    @Override
    public void addCourse(String courseId, String courseName, int credit, int classHour,
                          Course.CourseGrading grading, @Nullable Prerequisite prerequisite) {
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = connection.prepareStatement(
                     "insert into course (id, name, credit, class_hour, grading,prerequisite) VALUES (?,?,?,?,?,?);")) {
            stmt.setString(1, courseId);
            stmt.setString(2, courseName);
            stmt.setInt(3, credit);
            stmt.setInt(4, classHour);
            stmt.setBoolean(5, grading == Course.CourseGrading.HUNDRED_MARK_SCORE);
            if(prerequisite==null) {
                stmt.setNull(6, Types.INTEGER);
            }else {
                stmt.setInt(6,handlePrerequisite(prerequisite));
            }
            stmt.execute();
        } catch (SQLException e) {
            throw new IntegrityViolationException();
        }
    }

    @Override
    public int addCourseSection(String courseId, int semesterId, String sectionName, int totalCapacity) {
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = connection.prepareStatement(
                     "insert into course_section (semester_id, name, section_name, total_capacity, left_capacity) VALUES (?,?,?,?,?);");
             PreparedStatement SQue = connection.prepareStatement(
                     "select id from course_section where (semester_id, name, section_name, total_capacity, left_capacity) = (?,?,?,?,?);")) {
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
                if(courseId.equals("CS102A")){
                    return result.getInt(1);
                }
                return result.getInt(1);
            }
        } catch (SQLException e) {
            throw new IntegrityViolationException();
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
            }
        } catch (SQLException e) {
            throw new IntegrityViolationException();
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
            throw new EntityNotFoundException();
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
            throw new EntityNotFoundException();
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
            throw new EntityNotFoundException();
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
                cur.grading=result.getBoolean(5)?HUNDRED_MARK_SCORE:PASS_OR_FAIL;
                courses.add(cur);
            }
        } catch (SQLException e) {
            throw new EntityNotFoundException();
        }
        return courses;
    }

    @Override
    public List<CourseSection> getCourseSectionsInSemester(String courseId, int semesterId) {
        ArrayList<CourseSection> courseSections = new ArrayList<>();
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = connection.prepareStatement("select * from course_section where (name,semester_id)= (?,?);")) {
            stmt.setString(1, courseId);
            stmt.setInt(2, semesterId);
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
            throw new EntityNotFoundException();
        }
        return courseSections;
    }

    @Override
    public synchronized Course getCourseBySection(int sectionId) {
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = connection.prepareStatement("select name from course_section where id=(?);");
             PreparedStatement SQue = connection.prepareStatement("select * from course where id=(?);")) {
            stmt.setInt(1, sectionId);
            stmt.execute();
            ResultSet result=stmt.getResultSet();
            result.next();
            SQue.setString(1, result.getString(1));
            SQue.execute();
            ResultSet res=SQue.getResultSet();
            res.next();
            Course cur=new Course();
            cur.id=res.getString(1);
            cur.name=res.getString(2);
            cur.credit=res.getInt(3);
            cur.classHour=res.getInt(4);
            cur.grading=res.getBoolean(5)?HUNDRED_MARK_SCORE:PASS_OR_FAIL;
            return cur;
        } catch (SQLException e) {
            throw new EntityNotFoundException();
        }
    }

    @Override
    public List<CourseSectionClass> getCourseSectionClasses(int sectionId) {
        ArrayList<CourseSectionClass> courseSectionClasses = new ArrayList<>();
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement SQue = connection.prepareStatement("select * from course_section_class where id=(?);")) {
            SQue.setInt(1, sectionId);
            SQue.execute();
            ResultSet result=SQue.getResultSet();
            DoUserService doUserService=new DoUserService();
            while(result.next()) {
                CourseSectionClass cur=new CourseSectionClass();
                Instructor instructor= (Instructor) doUserService.getUser(result.getInt(3));
                cur.id=result.getInt(1);
                cur.instructor=instructor;
                cur.dayOfWeek=DayOfWeek.valueOf(result.getString(4));
                Array arr=result.getArray(5);
                ResultSet rs=arr.getResultSet();
                HashSet<Short> set=new HashSet<>();
                while (rs.next()){
                    set.add(rs.getShort(2));
                }
                cur.weekList=set;
                cur.classBegin=result.getShort(6);
                cur.classEnd=result.getShort(7);
                cur.location=result.getString(8);
                courseSectionClasses.add(cur);
            }
        } catch (SQLException e) {
            throw new EntityNotFoundException();
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
            result.next();
                CourseSection cur=new CourseSection();
                cur.id=result.getInt(2);
                cur.name=result.getString(4);
                cur.totalCapacity=result.getInt(5);
                cur.leftCapacity=result.getInt(6);
                return cur;
        } catch (SQLException e) {
            throw new EntityNotFoundException();
        }
    }

    @Override
    public List<Student> getEnrolledStudentsInSemester(String courseId, int semesterId) {
        ArrayList<Student> students = new ArrayList<>();
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = connection.prepareStatement("select * from course_section where id = (?);")) {

            return students;
        } catch (SQLException e) {
            throw new EntityNotFoundException();
        }
    }


    /*
     * Add one course section class according to following parameters:
     * If some of parameters are invalid, throw {@link cn.edu.sustech.cs307.exception.IntegrityViolationException}
     *
     * @param sectionId 0
     * @param instructorId 0
     * @param dayOfWeek 0
     * @param weekList 0
     * @param classStart 0
     * @param classEnd 0
     * @param location 0
     * @return the CourseSectionClass id of new inserted line.
     *
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
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }//*/



}
