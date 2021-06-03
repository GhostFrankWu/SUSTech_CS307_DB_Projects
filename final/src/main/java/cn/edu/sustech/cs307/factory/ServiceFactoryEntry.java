package cn.edu.sustech.cs307.factory;

import cn.edu.sustech.cs307.service.*;

public class ServiceFactoryEntry extends ServiceFactory {
    public ServiceFactoryEntry(){
        registerService(CourseService.class,new DoCourseService());
        registerService(UserService.class,new DoUserService());
        registerService(StudentService.class,new DoStudentService());
        registerService(SemesterService.class,new DoSemesterService());
        registerService(MajorService.class,new DoMajorService());
        registerService(InstructorService.class,new DoInstructorService());
        registerService(DepartmentService.class,new DoDepartmentService());
    }
}
