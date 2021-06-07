package work.entry;

import cn.edu.sustech.cs307.factory.ServiceFactory;
import cn.edu.sustech.cs307.service.*;
import work.*;

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
