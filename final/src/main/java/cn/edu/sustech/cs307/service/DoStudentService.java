package cn.edu.sustech.cs307.service;

import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.dto.*;
import cn.edu.sustech.cs307.dto.grade.Grade;
import cn.edu.sustech.cs307.dto.grade.HundredMarkGrade;
import cn.edu.sustech.cs307.dto.grade.PassOrFailGrade;
import cn.edu.sustech.cs307.dto.prerequisite.AndPrerequisite;
import cn.edu.sustech.cs307.dto.prerequisite.CoursePrerequisite;
import cn.edu.sustech.cs307.dto.prerequisite.OrPrerequisite;
import cn.edu.sustech.cs307.dto.prerequisite.Prerequisite;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.sql.*;
import java.sql.Date;
import java.time.DayOfWeek;
import java.util.*;

@ParametersAreNonnullByDefault
public class DoStudentService implements StudentService {
    static Connection connection;

    static {
        try {
            connection = SQLDataSource.getInstance().getSQLConnection();
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

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
    public List<CourseSearchEntry> searchCourse(int studentId, int semesterId, @Nullable String searchCid,
                @Nullable String searchName, @Nullable String searchInstructor, @Nullable DayOfWeek searchDayOfWeek,
                @Nullable Short searchClassTime, @Nullable List<String> searchClassLocations, CourseType searchCourseType,
                boolean ignoreFull, boolean ignoreConflict, boolean ignorePassed, boolean ignoreMissingPrerequisites,
                int pageSize, int pageIndex) {
        ArrayList<CourseSearchEntry> arrayList = new ArrayList<>();
        ArrayList<CourseSearchEntry> res = new ArrayList<>();
        ArrayList<CourseSection> courseSections = new ArrayList<>();
        DoCourseService getCourse=new DoCourseService();
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = connection.prepareStatement(
                     "select * from course_section where (semester_id)= (?);");//todo can be better
             PreparedStatement checkCoursePassed = connection.prepareStatement(
                     "select * from(select * from" +//todo can be better
                             " (SELECT name FROM course_section WHERE id=(?))courses join course_section cs" +
                             " on cs.name=courses.name)val join course_select c on c.course_section_class_id=val.id" +
                             " and c.stu_id=(?);");
             ) {
            stmt.setInt(1, semesterId);
            stmt.execute();
            ResultSet result = stmt.getResultSet();
            while (result.next()) {
                CourseSection cur = new CourseSection();
                cur.id = result.getInt(1);//now its real id
                cur.name = result.getString(4);
                cur.totalCapacity = result.getInt(5);
                cur.leftCapacity = result.getInt(6);
                courseSections.add(cur);
            }
            for (CourseSection courseSection : courseSections) {
                if (!ignoreFull && courseSection.leftCapacity == 0) {
                    continue;
                }
                if (!ignorePassed) {
                    checkCoursePassed.setInt(1,courseSection.id);
                    checkCoursePassed.setInt(2,studentId);
                    checkCoursePassed.execute();
                    if(checkCoursePassed.getResultSet().next()){
                        continue;
                    }
                }
                if(!ignoreConflict){
                    //todo
                }
                if(!ignoreMissingPrerequisites){
                    if(checkPrerequisite(studentId, courseSection.id)){
                        continue;
                    }
                }
                CourseSearchEntry courseSearchEntry = new CourseSearchEntry();
                courseSearchEntry.section = courseSection;
                courseSearchEntry.course = getCourse.getCourseBySection(courseSection.id);
                courseSearchEntry.sectionClasses = new HashSet<>(getCourse.getCourseSectionClasses(courseSection.id));
                courseSearchEntry.conflictCourseNames = new ArrayList<>();
                arrayList.add(courseSearchEntry);
            }
            for (int i = (pageIndex) * pageSize; i < pageSize && i < arrayList.size(); i++) {
                res.add(arrayList.get(i));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return res;
    }

    private Prerequisite generatePrerequisite(int prerequisiteID){
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = connection.prepareStatement("select type from list_prerequisite where id=(?);");
             PreparedStatement basic = connection.prepareStatement("select course_id from basic_prerequisite where id=(?);");
             PreparedStatement and = connection.prepareStatement("select terms from and_prerequisite where id=(?);");
             PreparedStatement or = connection.prepareStatement("select terms from or_prerequisite where id=(?);")) {
            stmt.setInt(1, prerequisiteID);
            stmt.execute();
            ResultSet resultSet=stmt.getResultSet();
            resultSet.next();
            int type=resultSet.getInt(1);
            if(type==1){
                basic.setInt(1,prerequisiteID);
                basic.execute();
                ResultSet result=basic.getResultSet();
                result.next();
                return new CoursePrerequisite(result.getString(1));
            }else if(type==2){
                and.setInt(1,prerequisiteID);
                and.execute();
                ResultSet result=and.getResultSet();
                result.next();
                Array array=result.getArray(1);
                ResultSet rs=array.getResultSet();
                ArrayList<Prerequisite> arrayList=new ArrayList<>();
                while (rs.next()){
                    arrayList.add(generatePrerequisite(rs.getInt(1)));
                }
                return new AndPrerequisite(arrayList);
            }else{
                or.setInt(1,prerequisiteID);
                or.execute();
                ResultSet result=or.getResultSet();
                result.next();
                Array array=result.getArray(1);
                ResultSet rs=array.getResultSet();
                ArrayList<Prerequisite> arrayList=new ArrayList<>();
                while (rs.next()){
                    arrayList.add(generatePrerequisite(rs.getInt(1)));
                }
                return new OrPrerequisite(arrayList);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println(prerequisiteID);
        }
        return null;
    }

    private boolean check(Prerequisite prerequisite,ArrayList<String> courseLearned){
        if(prerequisite instanceof CoursePrerequisite){
            for(String s:courseLearned){
                if(s.equals(((CoursePrerequisite) prerequisite).courseID)){
                    return true;
                }
            }
            return false;
        }else if(prerequisite instanceof AndPrerequisite){
            for(Prerequisite p:((AndPrerequisite) prerequisite).terms){
                if(!check(p,courseLearned)){
                    return false;
                }
            }
            return true;
        }else{
            for(Prerequisite p:((OrPrerequisite) prerequisite).terms){
                if(check(p,courseLearned)){
                    return true;
                }
            }
            return false;
        }
    }

    private boolean checkPrerequisite(int studentId, int sectionID){
        ArrayList<String> courseLearned=new ArrayList<>();
        try(PreparedStatement checkCoursePrerequisite = connection.prepareStatement(
                        "select course_section_class_id from course_select where (stu_id) = (?) and grade >= 60;")){
            checkCoursePrerequisite.setInt(1,studentId);
            checkCoursePrerequisite.execute();
            ResultSet resultSet=checkCoursePrerequisite.getResultSet();
            DoCourseService getCourse=new DoCourseService();
            while (resultSet.next()){
                Course course=getCourse.getCourseBySection(resultSet.getInt(1));
                courseLearned.add(course.name);
            }
        }catch (SQLException e){
            e.printStackTrace();
        }
        Course course=new DoCourseService().getCourseBySection(sectionID);
        try (PreparedStatement stmt = connection.prepareStatement("select prerequisite from course where id=(?);")) {
            stmt.setString(1, course.id);
            stmt.execute();
            ResultSet resultSet=stmt.getResultSet();
            resultSet.next();
            int status=resultSet.getInt(1);
            if(status!=0){
                Prerequisite prerequisite=generatePrerequisite(status);
                assert prerequisite!=null;//
                return !check(prerequisite, courseLearned);
            }else {
                return false;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public EnrollResult enrollCourse(int studentId, int sectionId) {
        try (PreparedStatement checkCourseExists = connection.prepareStatement(
                     "select * from course_section_class where (serial_id) = (?);");
             PreparedStatement checkCourseSelected = connection.prepareStatement(
                     "select * from course_select where (stu_id, course_section_class_id) = (?,?) and grade is null;");
             PreparedStatement checkCoursePassed = connection.prepareStatement(
                     "select * from(select * from" +
                             " (SELECT name FROM course_section WHERE id=(?))courses join course_section cs" +
                             " on cs.name=courses.name)val join course_select c on c.course_section_class_id=val.id" +
                             " and c.stu_id=(?);");
             PreparedStatement checkCourseFull = connection.prepareStatement(
                     "select left_capacity from course_section cs join course_section_class csc on cs.id = (?);");
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

            checkCoursePassed.setInt(2,studentId);
            checkCoursePassed.setInt(1,sectionId);
            checkCoursePassed.execute();
            if(checkCoursePassed.getResultSet().next()){
                return EnrollResult.ALREADY_PASSED;
            }


            if(checkPrerequisite(studentId, sectionId)){
                return EnrollResult.PREREQUISITES_NOT_FULFILLED;
            }

            checkCourseFull.setInt(1,sectionId);
            checkCourseFull.execute();
            ResultSet result=checkCourseFull.getResultSet();
            result.next();
            if(result.getInt(1)==0){
                return EnrollResult.COURSE_IS_FULL;
            }
            //fixme check valid
            /*
                EnrollResult.COURSE_CONFLICT_FOUND;
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
        try (PreparedStatement stmt = connection.prepareStatement(
                "delete from course_select where (stu_id,course_section_class_id) = (?,?);");
             PreparedStatement SQue = connection.prepareStatement(
                     "select * from course_select where (stu_id,course_section_class_id) = (?,?) and grade is null;")) {//
            SQue.setInt(1, studentId);
            SQue.setInt(2, sectionId);
            SQue.execute();
            ResultSet result=SQue.getResultSet();
            if(!result.next()){
                throw new IllegalStateException();
            }
            stmt.setInt(1, studentId);
            stmt.setInt(2, sectionId);
            stmt.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void addEnrolledCourseWithGrade(int studentId, int sectionId, @Nullable Grade grade) {
        try (PreparedStatement stmt = connection.prepareStatement(
                     "insert into course_select (stu_id, course_section_class_id,grade) values (?,?,?)")) {
            stmt.setInt(1, studentId);
            stmt.setInt(2, sectionId);
            stmt.setNull(3, 3);
            if (grade!=null) {
                if (grade instanceof HundredMarkGrade) {
                    HundredMarkGrade hundredMarkGrade = (HundredMarkGrade) grade;
                    stmt.setShort(3, hundredMarkGrade.mark);
                } else {
                    PassOrFailGrade passOrFailGrade = (PassOrFailGrade) grade;
                    stmt.setShort(3, (short) (passOrFailGrade.equals(PassOrFailGrade.PASS) ? 60 : 0));
                }
            }
            stmt.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setEnrolledCourseGrade(int studentId, int sectionId, Grade grade) {
        try (PreparedStatement stmt = connection.prepareStatement(
                     "update course_select set grade=(?) where (stu_id, course_section_class_id)=(?,?);")) {
            stmt.setInt(2, studentId);
            stmt.setInt(3, sectionId);
            if(grade instanceof HundredMarkGrade) {
                HundredMarkGrade hundredMarkGrade= (HundredMarkGrade) grade;
                stmt.setShort(1, hundredMarkGrade.mark);
            }else{
                PassOrFailGrade passOrFailGrade= (PassOrFailGrade) grade;
                stmt.setShort(3, (short) (passOrFailGrade.equals(PassOrFailGrade.PASS)?60:0));
            }
            stmt.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Map<Course, Grade> getEnrolledCoursesAndGrades(int studentId, @Nullable Integer semesterId) {
        HashMap<Course, Grade> map=new HashMap<>();
        try (PreparedStatement stmt = connection.prepareStatement("select * from course_select where (stu_id) = (?);");
             PreparedStatement SQue = connection.prepareStatement("select * from course c join " +
                     "(select cs.name from course_section cs join course_section_class csc on cs.id = csc.id " +
                     " and csc.serial_id=(?) )j on c.name=j.name;")) {
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
        CourseTable courseTable=new CourseTable();
        try(PreparedStatement stmt = connection.prepareStatement(
                "select * from course_section_class csc join course_select cs on csc.serial_id = " +
                        "cs.course_section_class_id and cs.stu_id=(?);")){
            stmt.setInt(1,studentId);
            stmt.execute();
            ResultSet resultSet=stmt.getResultSet();
            courseTable.table=new HashMap<>();
            while(resultSet.next()) {
                CourseTable.CourseTableEntry courseTableEntry = new CourseTable.CourseTableEntry();
                //fixme
            }
        }catch (SQLException e){
            e.printStackTrace();
        }
        return courseTable;
    }

    @Override
    public boolean passedPrerequisitesForCourse(int studentId, String courseId) {
        int id=-1;
        try(PreparedStatement stmt = connection.prepareStatement("select id from course_section where name=(?);")){
            stmt.setString(1,courseId);
            stmt.execute();
            ResultSet resultSet=stmt.getResultSet();
            resultSet.next();
            id=resultSet.getInt(1);
        }catch (SQLException e){
            e.printStackTrace();
        }
        return checkPrerequisite(studentId,id);
    }

    @Override
    public Major getStudentMajor(int studentId) {
        try (PreparedStatement stmt = connection.prepareStatement("select major_id from student where id = (?);");
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
                stmt.close();//todo notclose
                SQue.close();
                NQue.close();
                return cur;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
