package cn.edu.sustech.cs307.service;

import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.dto.*;
import cn.edu.sustech.cs307.dto.grade.Grade;
import cn.edu.sustech.cs307.dto.grade.HundredMarkGrade;
import cn.edu.sustech.cs307.dto.grade.PassOrFailGrade;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.sql.*;
import java.sql.Date;
import java.time.DayOfWeek;
import java.util.*;

@ParametersAreNonnullByDefault
public class DoStudentService implements StudentService {

    @Override
    public void addStudent(int userId, int majorId, String firstName, String lastName, Date enrolledDate) {
        int uid = new DoUserService().addUser(userId, firstName + "," + lastName);
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
    public List<CourseSearchEntry> searchCourse(
            int studentId, int semesterId, @Nullable String searchCid,
            @Nullable String searchName, @Nullable String searchInstructor, @Nullable DayOfWeek searchDayOfWeek,
            @Nullable Short searchClassTime, @Nullable List<String> searchClassLocations, CourseType searchCourseType,
            boolean ignoreFull, boolean ignoreConflict, boolean ignorePassed, boolean ignoreMissingPrerequisites,
            int pageSize, int pageIndex) {//searchClassLocations
        ArrayList<CourseSearchEntry> arrayList = new ArrayList<>();
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = connection.prepareStatement(
           "select * from (select * from (select * from\n" +
                   "    (select cs.id cid,cs.name cname,c.name nme,* from course_section cs join course c on c.id = cs.name)fi\n" +
                   "        join course_section_class csc on csc.id=fi.cid)f\n" +
                   "    left join major_course mc on mc.course_id=f.cname)fir join users u\n" +
                   "        on fir.instructor_id=u.id\n" +
                   "where (semester_id)= (?)\n" +
                   "  and (not check_course_full(fir.cid) or "+(ignoreFull?"true":"false")+")\n" +
                   "  and (check_prerequisite_by_csc_id(fir.cid,11710010) or "+(ignoreMissingPrerequisites?"true":"false")+")\n" +
                   "  and (not check_course_passed(fir.cid,11710010) or "+(ignorePassed?"true":"false")+")\n" +
                   "  and fir.cname like '%(?)%'\n" +//2
                   "  and (fir.nme||'['||fir.section_name||']' like '%(?)%')\n" +//3
                   "  and ((begin < (?) and \"end\" > (?)) or (?))\n" +//456
                   "  and (is_major_elective=(?) or ?)\n" +//78
                   "  and (day_of_week=(?) or ?)\n" +//910
                   "  and (full_name =(?) or ?)\n" +//1112
                   "offset (?)*(?) limit (?);")//131415
             ) {
            stmt.setInt(1, semesterId);
            stmt.setString(2, searchCid==null?"":searchCid);
            stmt.setString(3, searchName==null?"":searchName);
            stmt.setShort(4, searchClassTime==null?99:searchClassTime);
            stmt.setShort(5, searchClassTime==null?0:searchClassTime);
            stmt.setBoolean(6, searchClassTime == null);
            if(searchCourseType.equals(CourseType.MAJOR_COMPULSORY)||searchCourseType.equals(CourseType.MAJOR_ELECTIVE)) {
                stmt.setBoolean(7, searchCourseType.equals(CourseType.MAJOR_ELECTIVE));
                stmt.setBoolean(8, false);
            }else{
                stmt.setBoolean(8, true);
            }
            stmt.setString(9, searchDayOfWeek==null?"":searchDayOfWeek.toString());
            stmt.setBoolean(10, searchDayOfWeek==null);
            stmt.setString(11, searchInstructor==null?"":searchInstructor);
            stmt.setBoolean(12, searchInstructor==null);
            stmt.setInt(13, pageSize);
            stmt.setInt(14, pageIndex);
            stmt.setInt(15, pageSize);
            stmt.execute();
            ResultSet result = stmt.getResultSet();
            DoCourseService doCourseService=new DoCourseService();
            while (result.next()) {
                CourseSearchEntry courseSearchEntry=new CourseSearchEntry();
                courseSearchEntry.course=doCourseService.getCourseBySection(result.getInt(1));
                courseSearchEntry.section=new CourseSection();
                courseSearchEntry.section.id=result.getInt(1);
                courseSearchEntry.section.leftCapacity=result.getInt(9);
                courseSearchEntry.section.totalCapacity=result.getInt(8);
                courseSearchEntry.section.name=result.getString(2);/*
                courseSearchEntry..add();
                CourseSectionClass cur = new CourseSectionClass();
                cur.id = result.getInt(1);//now its real id
                cur.weekList = result.getString(4);
                cur.dayOfWeek = result.getInt(5);
                cur.location = result.getInt(6);
                cur.classBegin = result.getInt(6);
                cur.classEnd = result.getInt(6);
                cur.instructor = result.getInt(6);
                courseSearchEntry.sectionClasses.add(cur);*/
                courseSearchEntry.conflictCourseNames=new ArrayList<>();
                arrayList.add(courseSearchEntry);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return arrayList;
    }


    private boolean checkPrerequisite(int studentId, int sectionID) {
        try (Connection  connection=SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = connection.prepareStatement(("select check_prerequisite_by_csc_id(?,?);"))) {
            stmt.setInt(1, sectionID);
            stmt.setInt(2, studentId);
            stmt.execute();
            ResultSet resultSet = stmt.getResultSet();
            resultSet.next();
            return resultSet.getBoolean(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return true;
    }


    @Override
    public EnrollResult enrollCourse(int studentId, int sectionId) {//todo go one function
        try (Connection connection=SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement checkCourseExists = connection.prepareStatement(
                "select * from course_section where (id) = (?);");
             PreparedStatement checkCourseSelected = connection.prepareStatement(
                     "select * from course_select where (stu_id, course_section_id) = (?,?) and grade is null;");
             PreparedStatement checkCoursePassed = connection.prepareStatement(
                     "select check_course_passed(?,?);");
             PreparedStatement checkCourseFull = connection.prepareStatement(
                     "select check_course_full(?);");
             PreparedStatement addCourse = connection.prepareStatement(
                     "insert into course_select (stu_id, course_section_id) values (?,?);")) {
            connection.setAutoCommit(true);
            checkCourseExists.setInt(1, sectionId);
            checkCourseExists.execute();
            if (!checkCourseExists.getResultSet().next()) {
                return EnrollResult.COURSE_NOT_FOUND;
            }

            checkCourseSelected.setInt(1, studentId);
            checkCourseSelected.setInt(2, sectionId);
            checkCourseSelected.execute();
            if (checkCourseSelected.getResultSet().next()) {
                return EnrollResult.ALREADY_ENROLLED;
            }

            checkCoursePassed.setInt(2, studentId);
            checkCoursePassed.setInt(1, sectionId);
            checkCoursePassed.execute();
            ResultSet res = checkCoursePassed.getResultSet();
            res.next();
            if (res.getBoolean(1)) {
                return EnrollResult.ALREADY_PASSED;
            }


            if (!checkPrerequisite(studentId, sectionId)) {
                return EnrollResult.PREREQUISITES_NOT_FULFILLED;
            }

            checkCourseFull.setInt(1, sectionId);
            checkCourseFull.execute();
            ResultSet result = checkCourseFull.getResultSet();
            result.next();
            if (result.getBoolean(1)) {
                return EnrollResult.COURSE_IS_FULL;
            }
            //fixme check valid
            /*
                EnrollResult.COURSE_CONFLICT_FOUND;
            */

            addCourse.setInt(1, studentId);
            addCourse.setInt(2, sectionId);
            addCourse.execute();
        } catch (SQLException e) {
            e.printStackTrace();
            return EnrollResult.UNKNOWN_ERROR;
        }
        return EnrollResult.SUCCESS;
    }

    IllegalStateException ill=new IllegalStateException();

    @Override
    public void dropCourse(int studentId, int sectionId){
        try (Connection connection=SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement dropCourse = connection.prepareStatement("select drop_course(?,?);")) {
            dropCourse.setInt(1, studentId);
            dropCourse.setInt(2, sectionId);
            dropCourse.execute();
            ResultSet result = dropCourse.getResultSet();
            result.next();
            if (!result.getBoolean(1)) {
                throw ill;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void addEnrolledCourseWithGrade(int studentId, int sectionId, @Nullable Grade grade) {
        try (Connection connection=SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement enrollCourseWithGrade = connection.prepareStatement(
                "insert into course_select (stu_id, course_section_id,grade) values (?,?,?)")){
            enrollCourseWithGrade.setInt(1, studentId);
            enrollCourseWithGrade.setInt(2, sectionId);
            if (grade != null) {
                if (grade instanceof HundredMarkGrade) {
                    HundredMarkGrade hundredMarkGrade = (HundredMarkGrade) grade;
                    enrollCourseWithGrade.setShort(3, hundredMarkGrade.mark);
                } else {
                    PassOrFailGrade passOrFailGrade = (PassOrFailGrade) grade;
                    enrollCourseWithGrade.setShort(3, (short) (passOrFailGrade.equals(PassOrFailGrade.PASS) ? 60 : 0));
                }
            } else {
                enrollCourseWithGrade.setNull(3, 3);
            }
            enrollCourseWithGrade.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setEnrolledCourseGrade(int studentId, int sectionId, Grade grade) {
        try (Connection connection=SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement setEnrollCourseWithGrade = connection.prepareStatement(
                "update course_select set grade=(?) where (stu_id, course_class_id)=(?,?);")) {
            setEnrollCourseWithGrade.setInt(2, studentId);
            setEnrollCourseWithGrade.setInt(3, sectionId);
            if (grade instanceof HundredMarkGrade) {
                HundredMarkGrade hundredMarkGrade = (HundredMarkGrade) grade;
                setEnrollCourseWithGrade.setShort(1, hundredMarkGrade.mark);
            } else {
                PassOrFailGrade passOrFailGrade = (PassOrFailGrade) grade;
                setEnrollCourseWithGrade.setShort(3, (short) (passOrFailGrade.equals(PassOrFailGrade.PASS) ? 60 : 0));
            }
            setEnrollCourseWithGrade.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Map<Course, Grade> getEnrolledCoursesAndGrades(int studentId, @Nullable Integer Id) {
        HashMap<Course, Grade> map = new HashMap<>();
        try (Connection connection=SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = connection.prepareStatement("select * from course_select where (stu_id) = (?);");
             PreparedStatement SQue = connection.prepareStatement("select * from course c join " +
                     "(select cs.name from course_section cs join course_section_class csc on cs.id = csc.id " +
                     " and csc.serial_id=(?) )j on c.name=j.name;")) {
            stmt.setInt(1, studentId);
            stmt.execute();
            ResultSet result = stmt.getResultSet();
            while (result.next()) {
                SQue.setInt(1, result.getInt(2));
                SQue.execute();
                ResultSet res = SQue.getResultSet();
                Course course = new Course();
                course.id = res.getString(1);
                course.credit = res.getInt(2);
                course.classHour = res.getInt(3);
                course.grading = res.getBoolean(4) ? Course.CourseGrading.HUNDRED_MARK_SCORE : Course.CourseGrading.PASS_OR_FAIL;
                if (res.getBoolean(4)) {
                    HundredMarkGrade hundredMarkGrade = new HundredMarkGrade(res.getShort(3));
                    map.put(course, hundredMarkGrade);
                } else {
                    map.put(course, res.getShort(3) >= 60 ? PassOrFailGrade.PASS : PassOrFailGrade.FAIL);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return map;
    }

    @Override
    public CourseTable getCourseTable(int studentId, Date date) {
        CourseTable courseTable = new CourseTable();
        courseTable.table = new HashMap<>();
        for (DayOfWeek day : DayOfWeek.values()) {
            courseTable.table.put(day, new HashSet<>());
        }
        try (Connection connection=SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = connection.prepareStatement(
                "select instructor_id,location,last_time.cna,section_name,day_of_week,week_list,\n" +
                        "       last_time.begin,last_time.\"end\",(?)-s.begin from\n" +
                        "(select c.name cna,* from(select * from(select * from course_select cs join course_section c\n" +
                        "    on c.id = cs.course_section_id and cs.stu_id=(?))sections\n" +
                        "join course_section_class csc on course_section_id=csc.id)body join course c on body.name=c.id)last_time\n" +
                        "join semester s on last_time.semester_id=s.id and (?)>s.begin and (?)<s.\"end\";")) {
            stmt.setDate(1, date);
            stmt.setInt(2, studentId);
            stmt.setDate(3, date);
            stmt.setDate(4, date);
            stmt.execute();
            ResultSet resultSet = stmt.getResultSet();
            while (resultSet.next()) {
                Array arr = resultSet.getArray(6);
                ResultSet rs = arr.getResultSet();
                int week = (resultSet.getInt(9)+1) / 7 +1;
                boolean inWeek = false;
                while (rs.next()) {
                    if (week == (rs.getInt(1))) {
                        inWeek = true;
                        break;
                    }
                }
                if (!inWeek) {
                    continue;
                }
                CourseTable.CourseTableEntry courseTableEntry = new CourseTable.CourseTableEntry();
                courseTableEntry.instructor = (Instructor) new DoUserService().getUser(resultSet.getInt(1));
                courseTableEntry.location = resultSet.getString(2);
                courseTableEntry.courseFullName = String.format("%s[%s]", resultSet.getString(3), resultSet.getString(4));
                courseTableEntry.classBegin = resultSet.getShort(7);
                courseTableEntry.classEnd = resultSet.getShort(8);
                courseTable.table.get(DayOfWeek.valueOf(resultSet.getString(5))).add(courseTableEntry);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return courseTable;
    }

    @Override
    public boolean passedPrerequisitesForCourse(int studentId, String courseId) {
        int id = -1;
        try (Connection connection=SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = connection.prepareStatement("select id from course_section where name=(?);")) {
            stmt.setString(1, courseId);
            stmt.execute();
            ResultSet resultSet = stmt.getResultSet();
            resultSet.next();
            id = resultSet.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return checkPrerequisite(studentId, id);
    }

    @Override
    public Major getStudentMajor(int studentId) {
        try (Connection connection=SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = connection.prepareStatement("select major_id from student where id = (?);");
             PreparedStatement SQue = connection.prepareStatement("select * from major where id = (?);");
             PreparedStatement NQue = connection.prepareStatement("select * from department where id = (?);")) {
            stmt.setInt(1, studentId);
            stmt.execute();
            ResultSet result = stmt.getResultSet();
            if (result.next()) {
                SQue.setInt(1, studentId);
                SQue.execute();
                ResultSet res = SQue.getResultSet();
                Major cur = new Major();
                cur.id = res.getInt(1);
                cur.name = res.getString(3);
                NQue.setInt(1, res.getInt(2));
                NQue.execute();
                ResultSet r = NQue.getResultSet();
                Department department = new Department();
                department.id = r.getInt(1);
                department.name = r.getString(2);
                cur.department = department;
                return cur;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
