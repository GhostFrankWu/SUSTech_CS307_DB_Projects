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
           "select cid,cname,fir.nme,fir.semester_id,fir.section_name,fir.total_capacity,\n" +
                   "       fir.left_capacity,fir.credit,fir.class_hour,fir.grading,fir.prerequisite\n" +
                   "        from (select * from (select cid, cname, nme, semester_id, section_name, total_capacity,\n" +
                   "    left_capacity, credit, class_hour, grading, prerequisite, serial_id, instructor_id,\n" +
                   "                                     day_of_week, week_list, begin, \"end\", location from\n" +
                   "    (select cs.id cid,cs.name cname,c.name nme,* from course_section cs join course c on c.id = cs.name)fi\n" +
                   "        join course_section_class csc on csc.id=fi.cid)f\n" +
                   "    left join major_course mc on mc.course_id=f.cname)fir join users u\n" +
                   "        on fir.instructor_id=u.id\n" +
                   "where (semester_id)= (?)\n" +//1
                   "  and (not check_course_full(fir.cid) or "+(!ignoreFull?"true":"false")+")\n" +
                   "  and (check_prerequisite_by_csc_id(fir.cid,(?)) or "+(!ignoreMissingPrerequisites?"true":"false")+")\n" +///2
                   "  and (not check_course_passed(fir.cid,(?)) or "+(!ignorePassed?"true":"false")+")\n" +//3
                   "  and (check_time_fine((?),fir.cid) or "+(!ignoreConflict?"true":"false")+") "+//4
                   "  and fir.cname like '%'||(?)||'%'\n" +//5
                   "  and (fir.nme||'['||fir.section_name||']' like '%'||(?)||'%')\n" +//6
                   "  and ((begin < (?) and \"end\" > (?)) or (?))\n" +//789
                   "  and (is_major_elective=(?) or (?))\n" +//1011
                   "  and (day_of_week=(?) or (?))\n" +//1213
                   "  and (full_name =(?) or (?))\n" +//1415
                   " group by (cid,cname,fir.nme,fir.semester_id,fir.section_name,fir.total_capacity,\n" +
                   "         fir.left_capacity,fir.credit,fir.class_hour,fir.grading,fir.prerequisite\n" +
                   "         ) order by cname,nme||section_name offset (?)*(?) limit (?);")//161718
             ) {
            stmt.setInt(1, semesterId);
            stmt.setInt(2, studentId);
            stmt.setInt(3, studentId);
            stmt.setInt(4, studentId);
            stmt.setString(5, searchCid==null?"":searchCid);
            stmt.setString(6, searchName==null?"":searchName);
            stmt.setShort(7, searchClassTime==null?99:searchClassTime);
            stmt.setShort(8, searchClassTime==null?0:searchClassTime);
            stmt.setBoolean(9, searchClassTime == null);
            stmt.setBoolean(10, searchCourseType.equals(CourseType.MAJOR_ELECTIVE));
            if(searchCourseType.equals(CourseType.MAJOR_COMPULSORY)||searchCourseType.equals(CourseType.MAJOR_ELECTIVE)) {
                stmt.setBoolean(11, false);
            }else{
                stmt.setBoolean(11, true);
            }
            stmt.setString(12, searchDayOfWeek==null?"":searchDayOfWeek.toString());
            stmt.setBoolean(13, searchDayOfWeek==null);
            stmt.setString(14, searchInstructor==null?"":searchInstructor);
            stmt.setBoolean(15, searchInstructor==null);
            stmt.setInt(16, pageSize);
            stmt.setInt(17, pageIndex);
            stmt.setInt(18, pageSize);
            stmt.execute();
            ResultSet result = stmt.getResultSet();
            DoCourseService doCourseService=new DoCourseService();
            while (result.next()) {
                CourseSearchEntry courseSearchEntry=new CourseSearchEntry();
                courseSearchEntry.course=doCourseService.getCourseBySection(result.getInt(1));
                courseSearchEntry.section=new CourseSection();
                courseSearchEntry.section.id=result.getInt(1);
                courseSearchEntry.section.totalCapacity=result.getInt(6);
                courseSearchEntry.section.leftCapacity=result.getInt(7);
                courseSearchEntry.section.name=result.getString(5);
                courseSearchEntry.sectionClasses= new HashSet<>(doCourseService.getCourseSectionClasses(result.getInt(1)));
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
    public EnrollResult enrollCourse(int studentId, int sectionId) {
        try (Connection connection=SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement checkCourseExists = connection.prepareStatement(
                "select * from course_section where (id) = (?);");
             PreparedStatement checkCourseSelected = connection.prepareStatement(
                     "select * from course_select where (stu_id, course_section_id) = (?,?);");// and (grade is null or grade<60)
             PreparedStatement checkCoursePassed = connection.prepareStatement(
                     "select check_course_passed(?,?);");
             PreparedStatement checkCourseFull = connection.prepareStatement(
                     "select check_course_full(?);");
             PreparedStatement checkTime = connection.prepareStatement(
                     "select check_time_fine(?,?);");
             PreparedStatement addCourse = connection.prepareStatement(
                     "insert into course_select (stu_id, course_section_id) values (?,?);")) {
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

            checkTime.setInt(1,studentId);
            checkTime.setInt(2,sectionId);
            checkTime.execute();
            ResultSet r = checkTime.getResultSet();
            r.next();
            if (!r.getBoolean(1)) {
                return EnrollResult.COURSE_CONFLICT_FOUND;
            }

            checkCourseFull.setInt(1, sectionId);
            checkCourseFull.execute();
            ResultSet result = checkCourseFull.getResultSet();
            result.next();
            if (result.getBoolean(1)) {
                return EnrollResult.COURSE_IS_FULL;
            }

            addCourse.setInt(1, studentId);
            addCourse.setInt(2, sectionId);
            addCourse.execute();
        } catch (SQLException e) {
            e.printStackTrace();
            return EnrollResult.UNKNOWN_ERROR;
        }
        return EnrollResult.SUCCESS;
    }


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
                throw new IllegalStateException();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

/*
Import time usage: 2.63s
Test search course 1: 500
Test search course 1 time: 2.08s
Test enroll course 1: 1000
Test enroll course 1 time: 0.32s
Test drop enrolled course 1: 804
Test drop enrolled course 1 time: 0.17s
Import student courses
Import student courses time: 2.77s
Test drop course: 88657
Test drop course time: 1.30s
Test course table 2: 1000
Test course table 2 time: 0.79s
Test search course 2: 500
Test search course 2 time: 1.56s
Test enroll course 2: 1000
Test enroll course 2 time: 0.28s
 */
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
                } else if (grade instanceof PassOrFailGrade) {
                    PassOrFailGrade passOrFailGrade = (PassOrFailGrade) grade;
                    if(passOrFailGrade.equals(PassOrFailGrade.PASS)) {
                        enrollCourseWithGrade.setShort(3, (short) 60);
                    }else if(passOrFailGrade.equals(PassOrFailGrade.FAIL)) {
                        enrollCourseWithGrade.setShort(3, (short) 0);
                    }else{
                        System.err.println("===FUCK ARG===");
                        return;
                    }
                }else{
                    System.err.println("===FUCK U===");
                    return;
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
                "select instructor_id,location,last_time.cna||'['||section_name||']' ful,day_of_week,week_list,\n" +
                        "       last_time.begin,last_time.\"end\",(?)-s.begin from\n" +
                        "(select c.name cna,* from(select * from(select * from course_select cs join course_section c\n" +
                        "    on c.id = cs.course_section_id and cs.stu_id=(?))sections\n" +
                        "join course_section_class csc on course_section_id=csc.id)body join course c on body.name=c.id)last_time\n" +
                        "join semester s on last_time.semester_id=s.id and (?)>=s.begin and (?)<=s.\"end\";")) {
            stmt.setDate(1, date);
            stmt.setInt(2, studentId);
            stmt.setDate(3, date);
            stmt.setDate(4, date);
            stmt.execute();
            ResultSet resultSet = stmt.getResultSet();
            while (resultSet.next()) {
                Array arr = resultSet.getArray(5);
                ResultSet rs = arr.getResultSet();
                int week = (resultSet.getInt(8)) / 7+1;
                boolean inWeek = false;
                while (rs.next()) {
                    int cur=(rs.getInt(2));
                    if (week == cur) {
                        inWeek = true;
                    }
                }
                if (!inWeek) {
                    continue;
                }
                CourseTable.CourseTableEntry courseTableEntry = new CourseTable.CourseTableEntry();
                courseTableEntry.instructor = (Instructor) new DoUserService().getUser(resultSet.getInt(1));
                courseTableEntry.location = resultSet.getString(2);
                courseTableEntry.courseFullName = resultSet.getString(3);
                courseTableEntry.classBegin = resultSet.getShort(6);
                courseTableEntry.classEnd = resultSet.getShort(7);
                courseTable.table.get(DayOfWeek.valueOf(resultSet.getString(4))).add(courseTableEntry);
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
