
CREATE USER serverQuery with NOINHERIT LOGIN;
alter role serverQuery with password 你的密码;
grant connect on database cs307 to serverquery;
GRANT ALL PRIVILEGES ON DATABASE cs307 TO serverquery;
    GRANT ALL PRIVILEGES ON table 
        authentication ,teachers,classes,classes_class_id_seq,colleges,colleges_college_id_seq,
    course_chosen,courses,students,teachers,time_table TO serverquery;

create table authentication
(
    name         varchar     not null
        constraint authentication_pkey
            primary key,
    h3k4d_passwd varchar(64) not null,
    gm_level     integer
);

create table classes
(
    course_id    varchar(10) not null
        constraint classes_course_id_fkey
            references courses,
    name         varchar(40) not null,
    coursedept   varchar(40) not null,
    class_id     serial      not null
        constraint classes_class_id_key
            unique,
    time_hash    integer     not null,--fucking double unique, since time is foreign key, use hash of time
    capacity     integer,
    constraint classes_pkey
        primary key (course_id, name, time_hash)
);

create table colleges
(
    name       varchar(20) not null
        constraint colleges_pkey
            primary key,
    college_id serial      not null
        constraint colleges_college_id_key
            unique,
    name_en    varchar(20)
);

create table course_chosen
(
    class_id   integer not null
        constraint fk_classes
            references classes (class_id),
    student_id integer not null
        constraint fk_students
            references students,
    constraint pk_chosen_id
        primary key (class_id, student_id)
);

create table course_teach
(
    class_id   integer not null
        constraint course_teach_class_id_fkey
            references classes (class_id),
    teacher_id integer not null
        constraint course_teach_teacher_id_fkey
            references teachers (id),
    constraint course_teach_pkey
        primary key (class_id, teacher_id)
);

create table courses
(
    name         varchar(30) not null,--材料力学有两个名字一样ID不一样的课程
    course_id    varchar(10) not null--BIO302和BIO222有两个，重复加A注明
        constraint courses_pkey
            primary key,
    credit       integer     not null,
    course_hour  integer     not null,
    prerequisite varchar
);

create table students
(
    name                                                           varchar(20) not null,
    gender                                                         varchar(1)  not null,
    college_id                                                     integer     not null
        constraint students_college_id_fkey
            references colleges (college_id),
    student_id                                                     integer     not null
        constraint students_pkey
            primary key,
    current_course                                                 varchar,
    course_learned                                                 varchar,
    h3k4d_password_no_you_cant_guess_this_column_name_no_injection varchar(64)
);

create table teachers
(
    name varchar(40) not null
        constraint teachers_pkey
            primary key,
    id   integer     not null
        constraint teachers_id_key
            unique
        constraint teachers_id_check
            check (id > 29999999)
);

create table time_table
(
    weeklist  varchar(120) not null,
    location  varchar(50)  not null,
    classtime varchar(10)  not null,
    weekday   integer      not null
        constraint time_table_weekday_check
            check ((weekday > 0) AND (weekday < 8)),
    class_id  integer      not null
        constraint time_table_class_id_fkey
            references classes (class_id),
    constraint time_table_pkey
        primary key (weeklist, location, classtime, weekday, class_id)
);