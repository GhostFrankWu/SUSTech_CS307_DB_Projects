<?php
session_start();
function redirect($flag, $s, $url)
{
    if ($flag) {
        echo "<script>alert(\"$s\");</script>";
    }
    echo "<script>window.location.href = \"$url\";</script>";
}

if (!(isset($_SESSION['th']) && $_SESSION['th'] === true)) {
    redirect(true, "Please login first!", "login.php");
    die();
}
$db = null;
include("connection.php");
$time_array = null;
$week_array = null;
include("const.php");
?>
<!DOCTYPE html>
<html lang="en" class="no-js">
<head>
    <link rel="icon" href="favicon.ico" type="image/x-icon"/>
    <meta charset="utf-8"/>
    <title>hceTSUS选课系统</title>
    <link rel="stylesheet" href="css/font-awesome.min.css">
    <link rel="stylesheet" href="css/style.css">
</head>
<body style="background-image: url('img/BG.png');margin:0 auto;">
<script src="js/jquery-1.8.2.min.js"></script>
<script>
    function changePwd() {
        let pwd = prompt("请输入新密码");
        if (pwd !== null) {
            let pwd_ = prompt("请再次输入新密码");
            if (pwd !== pwd_) {
                alert("两次密码不一致！");
                changePwd();
            }
            $.ajax({type: 'post', url: 'selectionServer.php', data: 'password=' + pwd + '&operation=changePwd'}
            ).success(function (m) {
                alert(m);
                window.location.href = "logout.php";
            })
        }
    }
    <?php if (isset($_SESSION['reset']) && $_SESSION['reset'] === true) {
        $_SESSION['reset'] = false;
        echo "changePwd();";
    } ?>
</script>
<button class="label label-danger" style="position: absolute;top: 10px;right: 10px;width: 100px;margin-top: 0"
        onclick="window.location.href='logout.php'">Log out
</button>
<button class="label label-warning" style="position: absolute;top: 10px;right: 150px;width: 100px;margin-top: 0"
        onclick="window.location.href='classList.php'">查看课程表
</button>
<button class="label label-info" style="position: absolute;top: 10px;right: 290px;width: 100px;margin-top: 0"
        onclick=changePwd()>修改密码
</button>
<br>
<p style="background: linear-gradient(10deg, #ff0000,#0000FF);margin-left: 20px;
	-webkit-text-fill-color: transparent;width: 600px;-webkit-background-clip: text;font-size: 18px;
	">Welcome back, dear Teacher <strong>
        <?php
        $sql = "select name from teachers where id= $1 ;";
        $ret = pg_query_params($db, $sql, array($_SESSION['username']));
        $res = pg_fetch_row($ret);
        echo $res[0] . "</strong> !";
        ?>
<div class="col-md-12">
    <h1>&nbsp;</h1>
    <div style="margin: 10px auto 0 auto;">
        <div>
            <table class="table table-bordered table-hover">
                <thead>
                <tr>
                    <th colspan="8" style="font-size: 20px;text-align:center;">您教授的课程</th>
                </tr>
                <tr>
                    <th>Course ID</th>
                    <th>Course Name</th>
                    <th>Teacher Name</th>
                    <th>Time and Location</th>
                    <th>Credit and Length</th>
                    <th>Prerequisite</th>
                    <th>Class Dept</th>
                </tr>
                </thead>
                <tbody>
                <?php
                $sql = "select class_id from course_teach where teacher_id = $1 ;";
                $ret = pg_query_params($db, $sql, array($_SESSION['username']));
                while ($cur = pg_fetch_row($ret)) {
                    $sql = "select course_id,name,class_id,coursedept from classes where class_id= $1 ;";
                    $ret_ = pg_query_params($db, $sql, array($cur[0]));
                    while ($row = pg_fetch_row($ret_)) {
                        printTable($row,0);
                    }
                }
                ?>
                </tbody>
            </table><table class="table table-bordered table-hover">
                <thead>
                <tr>
                    <th colspan="8" style="font-size: 20px;text-align:center;">您课程中的学生</th>
                </tr>
                <tr>
                    <th>Course Name</th>
                    <th>Student Name</th>
                    <th>Student Gender</th>
                    <th>Student id</th>
                    <th>Student College</th>
                    <th>Options</th>
                </tr>
                </thead>
                <tbody>
                <?php
                $sql = "select class_id from course_teach where teacher_id = $1 ;";
                $ret = pg_query_params($db, $sql, array($_SESSION['username']));
                while ($cur = pg_fetch_row($ret)) {
                    $sql = "select cou.name,cls.name from(select course_id,name from classes where class_id=$1)cls
                            join courses cou on cls.course_id=cou.course_id;";
                    $ret_ = pg_query_params($db, $sql, array($cur[0]));
                    $sql = "select r.name, r.gender,r.student_id,colleges.name from
                    (select name, gender,s.student_id, college_id from (select student_id from course_chosen where class_id= $1)stu
                    join students s on s.student_id=stu.student_id)r
                    join colleges on r.college_id=colleges.college_id;";
                    $stu = pg_query_params($db, $sql, array($cur[0]));
                    $cls_ = pg_fetch_row($ret_);
                    while ($stu_ = pg_fetch_row($stu)) {
                        echo "<tr><td>".$cls_[0].$cls_[1]."</td>";
                        echo "<td>".$stu_[0]."</td>";
                        echo "<td>".$stu_[1]."</td>";
                        echo "<td>".$stu_[2]."</td>";
                        echo "<td>".$stu_[3]."</td>";
                        echo '<td><button class="label-danger" style="width:100px;margin-top: 0" onclick=
                    "$.ajax({type:\'post\',url:\'selectionServer.php\',data: \'class_id=' . $cur[0] ."&username=".$stu_[2].
                            '&operation=withdrawCourse\',}).success(function(m){alert(m);window.location.reload();})"
                    >劝退</button></td></tr>';
                    }
                }
                ?>
                </tbody>
            </table>
        </div>
    </div>
</body>
</html>