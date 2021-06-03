<?php
session_start();
try {
    if (!isset($_SESSION['username']) || !isset($_POST['operation'])) {
        throw new Exception();
    }
    $db = null;
    include("connection.php");
    switch ($_POST['operation']) {
        case 'withdrawCourse':
            $cla_id = $_POST['class_id'];
            if (isset($_SESSION['th']) && $_SESSION['th'] === true) {
                $stu_id = $_POST['username'];
            } else {
                $stu_id = $_SESSION['username'];
            }
            $sql = 'select count(*) from(select course_id from(select class_id from course_chosen 
                    where student_id=$1)cid inner join classes as cl on cid.class_id=cl.class_id)oid 
                    inner join classes as c on c.course_id = oid.course_id where c.class_id = $2;';
            $res = pg_query_params($db, $sql, array($stu_id, $cla_id));
            $sta = pg_fetch_row($res);
            if ($sta[0] == 0) {//!=即为过滤选课，==过滤退课
                echo "已经退选了这门课，请勿重复退课。";
            } else {
                $sql = "delete from course_chosen where student_id=$1 and class_id=$2;";
                $res = pg_query_params($db, $sql, array($stu_id, $cla_id));
                echo "退课成功。";
            }
            die();

        case 'selectCourse':
            $cla_id = $_POST['class_id'];
            $stu_id = $_SESSION['username'];
            $sql = 'select count(*) from(select course_id from(select class_id from course_chosen where student_id=$1)cid
        inner join classes as cl on cid.class_id=cl.class_id)oid inner join classes as c on c.course_id = oid.course_id where c.class_id = $2;';
            $res = pg_query_params($db, $sql, array($stu_id, $cla_id));
            $sta = pg_fetch_row($res);
            if ($sta[0] != 0) {
                echo "已经选修了这门课，请勿重复选择。";
                die();
            }
            $sql = 'select count(*) from course_chosen where class_id=$1';
            $res_0 = pg_query_params($db, $sql, array($cla_id));
            $sql = 'select capacity from classes where class_id=$1';
            $res_1 = pg_query_params($db, $sql, array($cla_id));
            if ($sta_0 = pg_fetch_row($res_0) && $sta_1 = pg_fetch_row($res_1)) {
                if (isset($sta_0[0]) && $sta_0[0] >= $sta_1[0]) {
                    echo "课程班级人数已满，选课失败。";
                    die();
                }
            }
            $sql = 'select prerequisite from courses join (select course_id from classes where class_id=$1)c on c.course_id=courses.course_id;';
            $res = pg_query_params($db, $sql, array($cla_id));
            $sta = pg_fetch_row($res);
            if (isset($sta[0])) {
                $sql = 'select count(*) from students where student_id= $1 and ' . $sta[0];
                $res = pg_query_params($db, $sql, array($stu_id));
                $sta = pg_fetch_row($res);
                if (intval($sta[0]) == 0) {
                    echo "先修课不足，选课失败。";
                    die();
                }
            }
            $sql = "select count(*),y.class_id from 
                (select * from time_table where class_id=$1)z
                join( select * from
                (select class_id s from course_chosen where student_id=$2)a
                join time_table b on a.s=b.class_id) y
                on z.weekday=y.weekday and (z.classtime like \"left\"(y.classtime,2)||'%'
                or z.classtime like '%'||\"right\"(y.classtime,2)) and (z.weeklist like
                y.weeklist||'%' or z.weeklist like '%'||y.weeklist or y.weeklist like
                z.weeklist||'%' or y.weeklist like '%'||z.weeklist) group by y.class_id";
            $res = pg_query_params($db, $sql, array($cla_id, $stu_id));
            $sta = pg_fetch_row($res);
            if (isset($sta[0])) {
                $sql = "select c.name,a.name from (select course_id,name from classes 
                        where class_id=$1)a join courses c on a.course_id=c.course_id;";
                $res = pg_query_params($db, $sql, array($sta[1]));
                $sta_ = pg_fetch_row($res);
                echo "课程时间冲突，冲突课程：" . $sta_[0] . "-" . $sta_[1] . "，选课失败。";
                die();
            }
            $sql = 'INSERT INTO course_chosen (student_id, class_id) values ($1,$2);';
            $res = pg_query_params($db, $sql, array($stu_id, $cla_id));
            $sql = 'SELECT current_course from students where student_id=$1;';
            $res = pg_query_params($db, $sql, array($stu_id));
            $sta = pg_fetch_row($res);
            if (isset($sta[0])) {
                $sql = "select course_id from classes where class_id=$1";
                $res = pg_query_params($db, $sql, array($cla_id));
                $course_id = pg_fetch_row($res);
                $sql = 'UPDATE students SET current_course=$1 where student_id=$2;';
                $res = pg_query_params($db, $sql, array(str_replace(" " . $course_id[0] . " ,", "", $sta[0]), $stu_id));
            }
            echo "选课成功。";
            die();

        case 'changePwd':
            $pwd = hash("sha256", $_POST['password']);
            if (isset($_SESSION['gm']) && $_SESSION['gm'] === true) {
                $stu_id = $_POST['username'];
                if (is_numeric($stu_id)) {
                    $sql = "UPDATE students SET h3k4d_password_no_you_cant_guess_this_column_name_no_injection = $1 where student_id = $2";
                    $res = pg_query_params($db, $sql, array($pwd, $stu_id));
                } else {
                    $sql = "UPDATE authentication SET h3k4d_passwd = $1 where name=$2;";
                    $res = pg_query_params($db, $sql, array($pwd, $stu_id));
                }
                echo "密码已更新";
            } else {
                $stu_id = $_SESSION['username'];
                $sql = "UPDATE students SET h3k4d_password_no_you_cant_guess_this_column_name_no_injection = $1 where student_id = $2";
                $res = pg_query_params($db, $sql, array($pwd, $stu_id));
                echo "密码已更新，请重新登录。";
            }
            die();

        default:
            $f = 1;
            throw new Exception();
    }
} catch (Exception $ex) {
    if (isset($f)) echo "Nice_try!flag{Y0u_wi11_be_r3p0rt3d!}<br >";
    echo "请不要攻击服务器，你的企图将会被报告<br > ";
    echo "请检查你的浏览环境是否安全，你的数据可能已经被黑客劫持";
    die();
}
