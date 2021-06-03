package cn.edu.sustech.cs307.service;

import cn.edu.sustech.cs307.dto.Course;
import cn.edu.sustech.cs307.dto.CourseSection;
import cn.edu.sustech.cs307.dto.CourseSectionClass;
import cn.edu.sustech.cs307.dto.Student;
import cn.edu.sustech.cs307.dto.prerequisite.Prerequisite;

import javax.annotation.Nullable;
import java.time.DayOfWeek;
import java.util.List;
import java.util.Set;

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

    }

    @Override
    public int addCourseSection(String courseId, int semesterId, String sectionName, int totalCapacity) {
        return 0;
    }


    @Override
    public int addCourseSectionClass(int sectionId, int instructorId, DayOfWeek dayOfWeek, Set<Short> weekList, short classStart, short classEnd, String location) {
        return 0;
    }

    @Override
    public void removeCourse(String courseId) {

    }

    @Override
    public void removeCourseSection(int sectionId) {

    }

    @Override
    public void removeCourseSectionClass(int classId) {

    }

    @Override
    public List<Course> getAllCourses() {
        return null;
    }

    @Override
    public List<CourseSection> getCourseSectionsInSemester(String courseId, int semesterId) {
        return null;
    }

    @Override
    public Course getCourseBySection(int sectionId) {
        return null;
    }

    @Override
    public List<CourseSectionClass> getCourseSectionClasses(int sectionId) {
        return null;
    }

    @Override
    public CourseSection getCourseSectionByClass(int classId) {
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
        return 0;
    };



}
