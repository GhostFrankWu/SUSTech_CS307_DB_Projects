package work.temp;

import cn.edu.sustech.cs307.database.SQLDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class Init {
    private static final String initFunc=
            "create or replace function searchCourse(student_id integer, semesterId integer, searchCid varchar,\n" +
                    "                                        searchName varchar, searchInstructor varchar, searchDayOfWeek varchar,\n" +
                    "                                        searchClassTime smallint, searchClassLocations varchar[],\n" +
                    "                                        searchCourseType varchar,\n" +
                    "                                        ignoreFull boolean, ignoreConflict boolean, ignorePassed boolean,\n" +
                    "                                        ignoreMissingPrerequisites boolean,\n" +
                    "                                        pageSize integer, pageIndex integer)\n" +
                    "    returns\n" +
                    "        table\n" +
                    "        (\n" +
                    "            a  integer,\n" +
                    "            b  varchar,\n" +
                    "            c  varchar,\n" +
                    "            d  integer,\n" +
                    "            e  varchar,\n" +
                    "            f  integer,\n" +
                    "            g  integer,\n" +
                    "            h  integer,\n" +
                    "            i  integer,\n" +
                    "            j  boolean,\n" +
                    "            k  integer,\n" +
                    "            l  boolean,\n" +
                    "            ct varchar[]\n" +
                    "        )\n" +
                    "as\n" +
                    "$$\n" +
                    "begin\n" +
                    "    return query (select cid,\n" +
                    "                         cname,\n" +
                    "                         fir.nme,\n" +
                    "                         fir.semester_id,\n" +
                    "                         fir.section_name,\n" +
                    "                         fir.total_capacity,\n" +
                    "                         fir.left_capacity,\n" +
                    "                         fir.credit,\n" +
                    "                         fir.class_hour,\n" +
                    "                         fir.grading,\n" +
                    "                         fir.prerequisite,\n" +
                    "                         is_major_elective,\n" +
                    "                         get_time_bad((student_id), cid) ct\n" +
                    "                  from (select *\n" +
                    "                        from (select *\n" +
                    "                              from (select cs.id cid, cs.name cname, c.name nme, *\n" +
                    "                                    from course_section cs\n" +
                    "                                             join course c on c.id = cs.name) fi\n" +
                    "                                       join course_section_class csc on\n" +
                    "                                      csc.id = fi.cid) f\n" +
                    "                                 left join major_course mc on mc.course_id = f.cname) fir\n" +
                    "                           join users u on fir.instructor_id = u.id\n" +
                    "                      and (semester_id) = (semesterId)\n" +
                    "                      and not (ignoreFull and check_course_full(fir.cid))\n" +
                    "                      and not (ignoreConflict and get_time_bad((student_id), cid) is not null)\n" +
                    "                      and not (ignorePassed and check_course_passed(fir.cid, student_id))\n" +
                    "                      and not (ignoreMissingPrerequisites and not check_prerequisite_by_csc_id(fir.cid, student_id))\n" +
                    "                      and (searchCid is null or fir.cname like '%' || searchCid || '%')\n" +
                    "                      and (searchName is null or\n" +
                    "                           fir.nme || '[' || fir.section_name || ']' like '%' || searchName || '%')\n" +
                    "                      and (searchClassTime is null or (begin <= searchClassTime and \"end\" >= searchClassTime))\n" +
                    "                      and (searchCourseType is null or\n" +
                    "                           ((searchCourseType != 'MAJOR_COMPULSORY' or (is_major_elective is false))\n" +
                    "                               and (searchCourseType != 'MAJOR_ELECTIVE' or (is_major_elective is true))\n" +
                    "                               and (searchCourseType != 'PUBLIC' or (is_major_elective is null))\n" +
                    "                               and (searchCourseType != 'CROSS_MAJOR' or (is_major_elective is not null))\n" +
                    "                               and (searchCourseType = 'ALL' or searchCourseType = 'PUBLIC'\n" +
                    "                                   or ((searchCourseType = 'CROSS_MAJOR' and (select count(*)\n" +
                    "                                                                              from student ss\n" +
                    "                                                                              where ss.id = student_id\n" +
                    "                                                                                and ss.major_id = fir.major_id) = 0)\n" +
                    "                                       or (searchCourseType != 'CROSS_MAJOR' and (select count(*)\n" +
                    "                                                                                  from student ss\n" +
                    "                                                                                  where ss.id = student_id\n" +
                    "                                                                                    and ss.major_id != fir.major_id) =\n" +
                    "                                                                                 0)))))\n" +
                    "                      and (searchDayOfWeek is null or day_of_week = searchDayOfWeek)\n" +
                    "                      and (searchInstructor is null or first_name like (searchInstructor || '%') or\n" +
                    "                           second_name like (searchInstructor || '%') or\n" +
                    "                           first_name || second_name like (searchInstructor || '%'))\n" +
                    "                      and (searchClassLocations is null or check_place_fine(searchClassLocations, fir.location))\n" +
                    "                  group by (cid, cname, fir.nme, fir.semester_id, fir.section_name, fir.total_capacity,\n" +
                    "                            fir.left_capacity, fir.credit, fir.class_hour, fir.grading, fir.prerequisite,\n" +
                    "                            is_major_elective)\n" +
                    "                  order by cname, fir.section_name\n" +
                    "                  offset pageSize * pageIndex limit pageSize);\n" +
                    "end;\n" +
                    "$$ language plpgsql;\n" +
                    "\n" +
                    "create or replace function get_time_bad(sid integer, cid integer) returns varchar[] AS\n" +
                    "$$\n" +
                    "declare\n" +
                    "    arr varchar[];\n" +
                    "BEGIN\n" +
                    "    arr = array_agg(fullname)\n" +
                    "          from (select *\n" +
                    "                from (select ca.name || '[' || sn || ']' fullname\n" +
                    "                      from (\n" +
                    "                               select val.s s, val.sn sn\n" +
                    "                               from (SELECT ncs.id i, cs.semester_id sm, cs.name s, ncs.section_name sn, *\n" +
                    "                                     from course_section ncs\n" +
                    "                                              join\n" +
                    "                                          course_section cs on cs.id = cid and cs.name = ncs.name) val\n" +
                    "                                        join (select *\n" +
                    "                                              from course_select cr\n" +
                    "                                                       join course_section ca\n" +
                    "                                                            on cr.course_section_id = ca.id) c\n" +
                    "                                             on c.course_section_id = val.i and c.semester_id = val.sm and\n" +
                    "                                                c.stu_id = (sid)) a\n" +
                    "                               join course ca on ca.id = a.s) p\n" +
                    "                union\n" +
                    "                select *\n" +
                    "                from (select ful fullname\n" +
                    "                      from (select cse.id l, *\n" +
                    "                            from course_section_class cse\n" +
                    "                                     join course_section cc on cse.id = cc.id) csc\n" +
                    "                               join (select last_time.cna || '[' || section_name || ']' ful,\n" +
                    "                                            day_of_week,\n" +
                    "                                            week_list,\n" +
                    "                                            i,\n" +
                    "                                            last_time.begin,\n" +
                    "                                            last_time.\"end\",\n" +
                    "                                            grade,\n" +
                    "                                            section_name,\n" +
                    "                                            semester_id\n" +
                    "                                     from (select c.name cna, *\n" +
                    "                                           from (select *\n" +
                    "                                                 from (select c.id i, *\n" +
                    "                                                       from course_select cs\n" +
                    "                                                                join course_section c\n" +
                    "                                                                     on c.id = cs.course_section_id and cs.stu_id = (sid)) sections\n" +
                    "                                                          join course_section_class csc on course_section_id = csc.id) body\n" +
                    "                                                    join course c on body.name = c.id) last_time\n" +
                    "                                              join semester s on last_time.semester_id = s.id) t on\n" +
                    "                              ((csc.\"end\" >= t.\"begin\" and csc.\"end\" <= t.\"end\") or\n" +
                    "                               (csc.begin >= t.begin and csc.begin <= t.\"end\")) and csc.semester_id = t.semester_id\n" +
                    "                              and csc.day_of_week = t.day_of_week and not check_week_fine(csc.week_list, t.week_list)\n" +
                    "                              and csc.l = (cid)) b\n" +
                    "                group by fullname\n" +
                    "                order by fullname) c;\n" +
                    "    return arr;\n" +
                    "END;\n" +
                    "$$ LANGUAGE plpgsql;\n" +
                    "\n" +
                    "create or replace function check_place_fine(places varchar[], place varchar) returns boolean as\n" +
                    "$$\n" +
                    "declare\n" +
                    "    pla varchar;\n" +
                    "begin\n" +
                    "    foreach pla IN ARRAY places\n" +
                    "        loop\n" +
                    "            if pla = 'ASK_FOR_SKIP_AFS8' then return true; end if;\n" +
                    "            if place like '%' || pla || '%' then return true; end if;\n" +
                    "        end loop;\n" +
                    "    return false;\n" +
                    "end;\n" +
                    "$$ language plpgsql;\n" +
                    "\n" +
                    "\n" +
                    "create or replace function check_week_fine(week_a integer[], week_b integer[]) returns boolean AS\n" +
                    "$$\n" +
                    "DECLARE\n" +
                    "    wa integer;\n" +
                    "    wb integer;\n" +
                    "BEGIN\n" +
                    "    foreach wa IN ARRAY week_a\n" +
                    "        loop\n" +
                    "            foreach wb IN ARRAY week_b\n" +
                    "                loop\n" +
                    "                    if wa = wb then\n" +
                    "                        return false;\n" +
                    "                    end if;\n" +
                    "                end loop;\n" +
                    "        end loop;\n" +
                    "    return true;\n" +
                    "END;\n" +
                    "$$ LANGUAGE plpgsql;\n" +
                    "\n" +
                    "CREATE OR REPLACE FUNCTION drop_course(sid integer, csc_id integer) RETURNS BOOLEAN AS\n" +
                    "$$\n" +
                    "declare\n" +
                    "    res boolean;\n" +
                    "BEGIN\n" +
                    "    res = (select count(*) from course_select where (stu_id, course_section_id) = (sid, csc_id) and grade is null) != 0;\n" +
                    "    if res then\n" +
                    "        delete from course_select where (stu_id, course_section_id) = (sid, csc_id) and grade is null;\n" +
                    "    end if;\n" +
                    "    return res;\n" +
                    "END;\n" +
                    "$$ LANGUAGE plpgsql;\n" +
                    "\n" +
                    "\n" +
                    "\n" +
                    "CREATE OR REPLACE FUNCTION check_course_full(csc_id integer) RETURNS BOOLEAN AS\n" +
                    "$$\n" +
                    "BEGIN\n" +
                    "    return (select left_capacity from course_section where id = (csc_id)) = 0;\n" +
                    "END;\n" +
                    "$$ LANGUAGE plpgsql;\n" +
                    "\n" +
                    "CREATE OR REPLACE FUNCTION check_course_passed(csc_id integer, stu integer) RETURNS BOOLEAN AS\n" +
                    "$$\n" +
                    "BEGIN\n" +
                    "    return (select count(*)\n" +
                    "            from (SELECT ncs.id\n" +
                    "                  from course_section ncs\n" +
                    "                           join\n" +
                    "                       course_section cs on cs.id = csc_id and cs.name = ncs.name) val\n" +
                    "                     join course_select c on c.course_section_id = val.id and c.stu_id = (stu) and c.grade >= 60) != 0;\n" +
                    "END;\n" +
                    "$$ LANGUAGE plpgsql;\n" +
                    "\n" +
                    "CREATE OR REPLACE FUNCTION check_prerequisite_by_csc_id(csc_id integer, stu integer) RETURNS BOOLEAN AS\n" +
                    "$$\n" +
                    "declare\n" +
                    "    pre_id integer;\n" +
                    "begin\n" +
                    "    pre_id = (select prerequisite\n" +
                    "              from course c\n" +
                    "                       join course_section cs on c.id = cs.name and cs.id = csc_id);\n" +
                    "    if pre_id is null then\n" +
                    "        return true;\n" +
                    "    end if;\n" +
                    "    return check_prerequisite(pre_id, stu);\n" +
                    "end;\n" +
                    "$$ LANGUAGE plpgsql;\n" +
                    "\n" +
                    "\n" +
                    "drop trigger if exists leftCapacity_trigger on course_select;\n" +
                    "\n" +
                    "CREATE OR REPLACE FUNCTION minus_leftCapacity() RETURNS TRIGGER AS\n" +
                    "$$\n" +
                    "BEGIN\n" +
                    "    if new.grade is null then\n" +
                    "        IF tg_op = 'INSERT' then\n" +
                    "            UPDATE course_section SET left_capacity=left_capacity - 1 where id = new.course_section_id;\n" +
                    "            return null;\n" +
                    "        elseif tg_op = 'DELETE' then\n" +
                    "            UPDATE course_section SET left_capacity=left_capacity + 1 where id = old.course_section_id;\n" +
                    "            return null;\n" +
                    "        end if;\n" +
                    "    end if;\n" +
                    "    return null;\n" +
                    "END;\n" +
                    "$$ LANGUAGE plpgsql;\n" +
                    "\n" +
                    "\n" +
                    "\n" +
                    "CREATE OR REPLACE FUNCTION check_prerequisite(pre_id integer, stu integer) RETURNS BOOLEAN AS\n" +
                    "$$\n" +
                    "DECLARE\n" +
                    "    type_id integer;\n" +
                    "    pres    int[];\n" +
                    "    pre     int;\n" +
                    "BEGIN\n" +
                    "    type_id = (select type from list_prerequisite where list_prerequisite.id = pre_id);\n" +
                    "    if type_id = 1 then\n" +
                    "        return (select count(*)\n" +
                    "                from (select cs.id\n" +
                    "                      from basic_prerequisite bp\n" +
                    "                               join course_section cs on\n" +
                    "                                  bp.id = pre_id and bp.course_id = cs.name) course_name\n" +
                    "                         join course_select css on\n" +
                    "                            css.course_section_id = course_name.id and css.stu_id = stu and css.grade >= 60) != 0;\n" +
                    "    elseif type_id = 2 then\n" +
                    "        pres = (select terms from and_prerequisite where id = pre_id);\n" +
                    "        foreach pre IN ARRAY pres\n" +
                    "            loop\n" +
                    "                if not check_prerequisite(pre, stu) then\n" +
                    "                    return false;\n" +
                    "                end if;\n" +
                    "            end loop;\n" +
                    "        return true;\n" +
                    "    elseif type_id = 3 then\n" +
                    "        pres = (select terms from or_prerequisite where id = pre_id);\n" +
                    "        foreach pre IN ARRAY pres\n" +
                    "            loop\n" +
                    "                if check_prerequisite(pre, stu) then\n" +
                    "                    return true;\n" +
                    "                end if;\n" +
                    "            end loop;\n" +
                    "        return false;\n" +
                    "    else\n" +
                    "        RAISE EXCEPTION '====FUCK====';\n" +
                    "    end if;\n" +
                    "END;\n" +
                    "$$ LANGUAGE plpgsql;\n" +
                    "\n" +
                    "CREATE TRIGGER leftCapacity_trigger\n" +
                    "    AFTER INSERT or DELETE\n" +
                    "    ON course_select\n" +
                    "    FOR EACH ROW\n" +
                    "EXECUTE PROCEDURE minus_leftCapacity();";
    private static final String initTable="\n" +
            "drop table if exists semester,major,course_search_entry,course_select,student,course_section_class,instructor,\n" +
            "    users,major_course,course_section,course_grading,course,department,\n" +
            "    list_prerequisite,basic_prerequisite,and_prerequisite,or_prerequisite;\n" +
            "\n" +
            "create table if not exists department(id serial primary key ,name varchar not null );\n" +
            "\n" +
            "create table if not exists major(id serial primary key,name varchar not null ,\n" +
            "                                 department_id integer references department(id));\n" +
            "create table if not exists course(\n" +
            "                                     id varchar primary key ,name varchar not null ,\n" +
            "                                     credit integer, class_hour integer, grading boolean not null,prerequisite integer );\n" +
            "\n" +
            "\n" +
            "create table if not exists course_section (\n" +
            "                                              id serial primary key ,semester_id int,\n" +
            "                                              name varchar references course(id), section_name varchar,total_capacity integer, left_capacity integer);\n" +
            "\n" +
            "create table if not exists users(id integer primary key, first_name varchar, second_name varchar);\n" +
            "\n" +
            "create table if not exists instructor(id serial primary key,\n" +
            "                                      user_id integer references users(id) unique );\n" +
            "\n" +
            "create table if not exists course_section_class(\n" +
            "                                                   serial_id serial primary key,\n" +
            "                                                   id integer references course_section(id),\n" +
            "                                                   instructor_id integer references instructor(user_id),day_of_week varchar not null ,--week_day as enum\n" +
            "                                                   week_list int[] not null,begin int2, \"end\" int2,location varchar,\n" +
            "                                                   check ( begin<12 and \"end\" <12 ));\n" +
            "\n" +
            "create table if not exists student(\n" +
            "                                      id integer primary key references users(id),\n" +
            "                                      enrolled_date date,major_id integer references major(id));;\n" +
            "\n" +
            "create table if not exists course_select(\n" +
            "                                            stu_id integer references student(id),\n" +
            "                                            course_section_id integer references course_section(id),grade int2);\n" +
            "\n" +
            "\n" +
            "create table if not exists semester (id serial primary key, name varchar not null,\n" +
            "                                     begin date not null, \"end\" date not null);\n" +
            "\n" +
            "create table if not exists list_prerequisite(id serial primary key,type smallint);\n" +
            "create table if not exists basic_prerequisite(id integer references list_prerequisite(id),course_id varchar);--1\n" +
            "create table if not exists and_prerequisite(id integer references list_prerequisite(id),terms int[]);--2\n" +
            "create table if not exists or_prerequisite(id integer references list_prerequisite(id),terms int[]);--3\n" +
            "\n" +
            "create table if not exists major_course(\n" +
            "                                           major_id integer references major(id),\n" +
            "                                           course_id varchar references course(id),is_major_elective boolean not null );\n" +
            "\n" +
            "create index index_on on course_select(stu_id,course_section_id);\n" +
            "\n";
    public Init(){
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = connection.prepareStatement(initTable);
             PreparedStatement SQue = connection.prepareStatement(initFunc)) {
            stmt.execute();
            //SQue.execute();
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(-2);
        }
    }
}
