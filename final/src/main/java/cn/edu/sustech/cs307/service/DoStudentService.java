package cn.edu.sustech.cs307.service;

import cn.edu.sustech.cs307.dto.Course;
import cn.edu.sustech.cs307.dto.CourseSearchEntry;
import cn.edu.sustech.cs307.dto.CourseTable;
import cn.edu.sustech.cs307.dto.Major;
import cn.edu.sustech.cs307.dto.grade.Grade;

import javax.annotation.Nullable;
import java.sql.Date;
import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;

public class DoStudentService implements StudentService {
    @Override
    public void addStudent(int userId, int majorId, String firstName, String lastName, Date enrolledDate) {

    }

    @Override
    public List<CourseSearchEntry> searchCourse(int studentId, int semesterId, @Nullable String searchCid, @Nullable String searchName, @Nullable String searchInstructor, @Nullable DayOfWeek searchDayOfWeek, @Nullable Short searchClassTime, @Nullable List<String> searchClassLocations, CourseType searchCourseType, boolean ignoreFull, boolean ignoreConflict, boolean ignorePassed, boolean ignoreMissingPrerequisites, int pageSize, int pageIndex) {
        return null;
    }

    @Override
    public EnrollResult enrollCourse(int studentId, int sectionId) {
        return null;
    }

    @Override
    public void dropCourse(int studentId, int sectionId) throws IllegalStateException {

    }

    @Override
    public void addEnrolledCourseWithGrade(int studentId, int sectionId, @Nullable Grade grade) {

    }

    @Override
    public void setEnrolledCourseGrade(int studentId, int sectionId, Grade grade) {

    }

    @Override
    public Map<Course, Grade> getEnrolledCoursesAndGrades(int studentId, @Nullable Integer semesterId) {
        return null;
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
        return null;
    }
}
