drop table if exists semester,major,course_select,student,course_section_class,instructor, users,major_course,course_section,course_grading,course,department, list_prerequisite,basic_prerequisite,and_prerequisite,or_prerequisite;
create table department
(
    id   serial  not null
        constraint department_pkey primary key,
    name varchar not null unique
);
create table major
(
    id            serial  not null
        constraint major_pkey primary key,
    name          varchar not null,
    department_id integer
        constraint major_department_id_fkey references department on delete cascade
);
create table course
(
    id           varchar not null
        constraint course_pkey primary key,
    name         varchar not null,
    credit       integer,
    class_hour   integer,
    grading      boolean not null,
    prerequisite integer
);
create table course_section
(
    id             serial not null
        constraint course_section_pkey primary key,
    semester_id    integer,
    name           varchar
        constraint course_section_name_fkey references course on delete cascade,
    section_name   varchar,
    total_capacity integer,
    left_capacity  integer
);
create table users
(
    id          integer not null
        constraint users_pkey primary key,
    first_name  varchar,
    second_name varchar
);
create table instructor
(
    id      serial not null
        constraint instructor_pkey primary key,
    user_id integer
        constraint instructor_user_id_key unique
        constraint instructor_user_id_fkey references users on delete cascade
);
create table course_section_class
(
    serial_id     serial    not null
        constraint course_section_class_pkey primary key,
    id            integer
        constraint course_section_class_id_fkey references course_section on delete cascade,
    instructor_id integer
        constraint course_section_class_instructor_id_fkey references instructor (user_id) on delete cascade,
    day_of_week   varchar   not null,
    week_list     integer[] not null,
    begin         smallint,
    "end"         smallint,
    location      varchar,
    constraint course_section_class_check check ((begin < 12) AND ("end" < 12))
);
create table student
(
    id            integer not null
        constraint student_pkey primary key
        constraint student_id_fkey references users on delete cascade,
    enrolled_date date,
    major_id      integer
        constraint student_major_id_fkey references major on delete cascade
);
create table course_select
(
    stu_id            integer
        constraint course_select_stu_id_fkey references student on delete cascade,
    course_section_id integer
        constraint course_select_course_section_id_fkey references course_section on delete cascade,
    grade             smallint
);
alter table course_select
    owner to postgres;
create index index_on on course_select (stu_id, course_section_id);
create table semester
(
    id    serial  not null
        constraint semester_pkey primary key,
    name  varchar not null,
    begin date    not null,
    "end" date    not null
);
create table list_prerequisite
(
    id   serial not null
        constraint list_prerequisite_pkey primary key,
    type smallint
);
create table basic_prerequisite
(
    id        integer
        constraint basic_prerequisite_id_fkey references list_prerequisite on delete cascade,
    course_id varchar
);
create table and_prerequisite
(
    id    integer
        constraint and_prerequisite_id_fkey references list_prerequisite on delete cascade,
    terms integer[]
);
create table or_prerequisite
(
    id    integer
        constraint or_prerequisite_id_fkey references list_prerequisite on delete cascade,
    terms integer[]
);
create table major_course
(
    major_id          integer
        constraint major_course_major_id_fkey references major on delete cascade,
    course_id         varchar
        constraint major_course_course_id_fkey references course on delete cascade,
    is_major_elective boolean not null
);






create or replace function searchCourse(student_id integer, semesterId integer, searchCid varchar, searchName varchar,
                                        searchInstructor varchar, searchDayOfWeek varchar, searchClassTime smallint,
                                        searchClassLocations varchar[], searchCourseType varchar, ignoreFull boolean,
                                        ignoreConflict boolean, ignorePassed boolean,
                                        ignoreMissingPrerequisites boolean, pageSize integer, pageIndex integer)
    returns table
            (
                a  integer,
                b  varchar,
                c  varchar,
                d  integer,
                e  varchar,
                f  integer,
                g  integer,
                h  integer,
                i  integer,
                j  boolean,
                k  integer,
                l  boolean,
                ct varchar[]
            )
as
$$
begin
    return query (select cid,
                         cname,
                         fir.nme,
                         fir.semester_id,
                         fir.section_name,
                         fir.total_capacity,
                         fir.left_capacity,
                         fir.credit,
                         fir.class_hour,
                         fir.grading,
                         fir.prerequisite,
                         is_major_elective,
                         get_time_bad((student_id), cid) ct
                  from (select *
                        from (select *
                              from (select cs.id cid, cs.name cname, c.name nme, *
                                    from course_section cs
                                             join course c on c.id = cs.name) fi
                                       join course_section_class csc on csc.id = fi.cid) f
                                 left join major_course mc on mc.course_id = f.cname) fir
                           join users u on fir.instructor_id = u.id and (semester_id) = (semesterId) and
                                           not (ignoreFull and check_course_full(fir.cid)) and
                                           not (ignoreConflict and get_time_bad((student_id), cid) is not null) and
                                           not (ignorePassed and check_course_passed(fir.cid, student_id)) and
                                           not (ignoreMissingPrerequisites and
                                                not check_prerequisite_by_csc_id(fir.cid, student_id)) and
                                           (searchCid is null or fir.cname like '%' || searchCid || '%') and
                                           (searchName is null or
                                            fir.nme || '[' || fir.section_name || ']' like '%' || searchName || '%') and
                                           (searchClassTime is null or
                                            (begin <= searchClassTime and "end" >= searchClassTime)) and
                                           (searchCourseType is null or
                                            ((searchCourseType != 'MAJOR_COMPULSORY' or (is_major_elective is false)) and
                                             (searchCourseType != 'MAJOR_ELECTIVE' or (is_major_elective is true)) and
                                             (searchCourseType != 'PUBLIC' or (is_major_elective is null)) and
                                             (searchCourseType != 'CROSS_MAJOR' or (is_major_elective is not null)) and
                                             (searchCourseType = 'ALL' or searchCourseType = 'PUBLIC' or
                                              ((searchCourseType = 'CROSS_MAJOR' and (select count(*)
                                                                                      from student ss
                                                                                      where ss.id = student_id
                                                                                        and ss.major_id = fir.major_id) =
                                                                                     0) or
                                               (searchCourseType != 'CROSS_MAJOR' and (select count(*)
                                                                                       from student ss
                                                                                       where ss.id = student_id
                                                                                         and ss.major_id != fir.major_id) =
                                                                                      0))))) and
                                           (searchDayOfWeek is null or day_of_week = searchDayOfWeek) and
                                           (searchInstructor is null or first_name like (searchInstructor || '%') or
                                            second_name like (searchInstructor || '%') or
                                            first_name || second_name like (searchInstructor || '%')) and
                                           (searchClassLocations is null or
                                            check_place_fine(searchClassLocations, fir.location))
                  group by (cid, cname, fir.nme, fir.semester_id, fir.section_name, fir.total_capacity,
                            fir.left_capacity, fir.credit, fir.class_hour, fir.grading, fir.prerequisite,
                            is_major_elective)
                  order by cname, fir.section_name
                  offset pageSize * pageIndex limit pageSize);
end;
$$ language plpgsql;
create or replace function get_time_bad(sid integer, cid integer) returns varchar[] AS
$$
declare
    arr varchar[];
BEGIN
    arr = array_agg(fullname)
          from (select *
                from (select ca.name || '[' || sn || ']' fullname
                      from (select val.s s, val.sn sn
                            from (SELECT ncs.id i, cs.semester_id sm, cs.name s, ncs.section_name sn, *
                                  from course_section ncs
                                           join course_section cs on cs.id = cid and cs.name = ncs.name) val
                                     join (select *
                                           from course_select cr
                                                    join course_section ca on cr.course_section_id = ca.id) c
                                          on c.course_section_id = val.i and c.semester_id = val.sm and
                                             c.stu_id = (sid)) a
                               join course ca on ca.id = a.s) p
                union
                select *
                from (select ful fullname
                      from (select cse.id l, *
                            from course_section_class cse
                                     join course_section cc on cse.id = cc.id) csc
                               join (select last_time.cna || '[' || section_name || ']' ful,
                                            day_of_week,
                                            week_list,
                                            i,
                                            last_time.begin,
                                            last_time."end",
                                            grade,
                                            section_name,
                                            semester_id
                                     from (select c.name cna, *
                                           from (select *
                                                 from (select c.id i, *
                                                       from course_select cs
                                                                join course_section c on c.id = cs.course_section_id and cs.stu_id = (sid)) sections
                                                          join course_section_class csc on course_section_id = csc.id) body
                                                    join course c on body.name = c.id) last_time
                                              join semester s on last_time.semester_id = s.id) t
                                    on ((csc."end" >= t."begin" and csc."end" <= t."end") or
                                        (csc.begin >= t.begin and csc.begin <= t."end")) and
                                       csc.semester_id = t.semester_id and csc.day_of_week = t.day_of_week and
                                       not check_week_fine(csc.week_list, t.week_list) and csc.l = (cid)) b
                group by fullname
                order by fullname) c;
    return arr;
END;
$$ LANGUAGE plpgsql;
create or replace function check_place_fine(places varchar[], place varchar) returns boolean as
$$
declare
    pla varchar;
begin
    foreach pla IN ARRAY places
        loop
            if place like '%' || pla || '%' then return true; end if;
        end loop;
    return false;
end;
$$ language plpgsql;
create or replace function check_week_fine(week_a integer[], week_b integer[]) returns boolean AS
$$
DECLARE
    wa integer; wb integer;
BEGIN
    foreach wa IN ARRAY week_a
        loop
            foreach wb IN ARRAY week_b
                loop
                    if wa = wb then return false; end if;
                end loop;
        end loop;
    return true;
END;
$$ LANGUAGE plpgsql;
CREATE OR REPLACE FUNCTION drop_course(sid integer, csc_id integer) RETURNS BOOLEAN AS
$$
declare
    res boolean;
BEGIN
    res = (select count(*) from course_select where (stu_id, course_section_id) = (sid, csc_id) and grade is null) != 0;
    if res then delete from course_select where (stu_id, course_section_id) = (sid, csc_id) and grade is null; end if;
    return res;
END;
$$ LANGUAGE plpgsql;
CREATE OR REPLACE FUNCTION check_course_full(csc_id integer) RETURNS BOOLEAN AS
$$
BEGIN
    return (select left_capacity from course_section where id = (csc_id)) = 0;
END;
$$ LANGUAGE plpgsql;
CREATE OR REPLACE FUNCTION check_course_passed(csc_id integer, stu integer) RETURNS BOOLEAN AS
$$
BEGIN
    return (select count(*)
            from (SELECT ncs.id
                  from course_section ncs
                           join course_section cs on cs.id = csc_id and cs.name = ncs.name) val
                     join course_select c on c.course_section_id = val.id and c.stu_id = (stu) and c.grade >= 60) != 0;
END;
$$ LANGUAGE plpgsql;
CREATE OR REPLACE FUNCTION check_prerequisite_by_csc_id(csc_id integer, stu integer) RETURNS BOOLEAN AS
$$
declare
    pre_id integer;
begin
    pre_id = (select prerequisite
              from course c
                       join course_section cs on c.id = cs.name and cs.id = csc_id);
    if pre_id is null then return true; end if; return check_prerequisite(pre_id, stu);
end;
$$ LANGUAGE plpgsql;
CREATE OR REPLACE FUNCTION check_prerequisite(pre_id integer, stu integer) RETURNS BOOLEAN AS
$$
DECLARE
    type_id integer; pres int[]; pre int;
BEGIN
    type_id = (select type from list_prerequisite where list_prerequisite.id = pre_id);
    if type_id = 1 then
        return (select count(*)
                from (select cs.id
                      from basic_prerequisite bp
                               join course_section cs on bp.id = pre_id and bp.course_id = cs.name) course_name
                         join course_select css
                              on css.course_section_id = course_name.id and css.stu_id = stu and css.grade >= 60) != 0;
    elseif type_id = 2 then
        pres = (select terms from and_prerequisite where id = pre_id);
        foreach pre IN ARRAY pres
            loop
                if not check_prerequisite(pre, stu) then return false; end if;
            end loop;
        return true;
    elseif type_id = 3 then
        pres = (select terms from or_prerequisite where id = pre_id);
        foreach pre IN ARRAY pres
            loop
                if check_prerequisite(pre, stu) then return true; end if;
            end loop;
        return false;
    else
        RAISE EXCEPTION '====FUCK====';
    end if;
END;
$$ LANGUAGE plpgsql;
drop trigger if exists leftCapacity_trigger on course_select;
CREATE OR REPLACE FUNCTION minus_leftCapacity() RETURNS TRIGGER AS
$$
BEGIN
    UPDATE course_section SET left_capacity=left_capacity + 1 where id = old.course_section_id; return null;
END;
$$ LANGUAGE plpgsql;
CREATE TRIGGER leftCapacity_trigger
    AFTER DELETE
    ON course_select
    FOR EACH ROW
EXECUTE PROCEDURE minus_leftCapacity();




-- create index quick_drop on course_select (stu_id, course_section_id);
-- create index quick_sut on student (id, major_id);
-- create index quick_tim on course_section_class (begin,"end");
-- create index quick_id_csc on course_section_class (id);
-- create index quick_id_cs on course_section_class (id);
-- create index quick_id_stu on student (id);
-- create index quick_id_ins on instructor (id);
-- create index quick_id_u on users (id);
-- create index quick_id_m on major (id);
-- create index quick_id_b on basic_prerequisite(id);
-- create index quick_id_mc on major_course (course_id);
-- create index quick_left on course_section (id,left_capacity);
-- create index quick_grade on course_select (stu_id,grade);


select * from course
join course_section cs on course.id = cs.name
join course_section_class csc on cs.id = csc.id
join course_select c on cs.id = c.course_section_id
where course.id = 'MA102B';

delete from course where id = 'MA102B';