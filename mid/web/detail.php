<?php
session_start();
function redirect($flag, $s, $url)
{
    if ($flag) {
        echo "<script>alert(\"$s\");</script>";
    }
    echo "<script>window.location.href = \"$url\";</script>";
}

if (!isset($_SESSION['username'])) {
    redirect(true, "Please login first!", "login.php");
    die();
}
if ((isset($_SESSION['gm']) && $_SESSION['gm'] === true)) {
    redirect(false, "", "detailAdmin.php");
}
if ((isset($_SESSION['th']) && $_SESSION['th'] === true)) {
    redirect(false, "", "detailTeacher.php");
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
	">Welcome back, dear <strong>
    <?php
    $sql = "select * from students where student_id= $1 ;";
    $userInfo = pg_query_params($db, $sql, array($_SESSION['username']));
    $userInfo_ = pg_fetch_row($userInfo);
    echo $userInfo_[1] == "F" ? "Miss." : "Mr.";
    echo $userInfo_[0] . "</strong> from <strong>";
    $sql = "select name from colleges where college_id= $1 ;";
    $userInfo = pg_query_params($db, $sql, array($userInfo_[2]));
    $college = pg_fetch_row($userInfo);
    echo $college[0] . "</strong> college!";
    ?></p>
<div class="col-md-11">
    <h1>&nbsp;</h1>
    <div style="margin: 10px auto 0 auto;">
        <?php if ($userInfo_[4] != ""){ ?>
        <div>
            <table class="table table-bordered table-hover">
                <thead>
                <tr>
                    <th colspan="8" style="font-size: 20px;text-align:center;">已选课程 确认班级</th>
                </tr>
                <tr>
                    <th>Course ID</th>
                    <th>Course Name</th>
                    <th>Teacher Name</th>
                    <th>Time and Location</th>
                    <th>Credit and Length</th>
                    <th>Prerequisite</th>
                    <th>Class Dept</th>
                    <th>Options</th>
                </tr>
                </thead>
                <tbody>
                <?php
                $courses = preg_split("[,]", $userInfo_[4]);
                foreach ($courses as $cur) {
                    $sql = "select course_id,name,class_id,coursedept from classes where course_id= $1 ;";
                    $ret = pg_query_params($db, $sql, array(strtoupper(trim($cur))));
                    while ($row = pg_fetch_row($ret)) {
                        printTable($row, 1);
                    }
                }
                }
                ?></tbody>
            </table>
        </div>
        <form method="post" style="float: left">

            <label>
                <input type="text" name="course_id" placeholder="Course ID" style="width:150px;color: #0f0f0f;height: 40px;margin-left: 20px">
            </label>
            <label>
                <input type="text" name="course_name" placeholder="Course Name" style="width:150px;color: #0f0f0f;height: 40px">
            </label>
            <label>
                <input type="text" name="course_teacher" placeholder="Course Teacher" style="width:150px;color: #0f0f0f;height: 40px">
            </label>
            <!--label>
                <input type="checkbox" name="check_zero" style="width: 42px;position:relative;top: 17px">忽略零余量课程
            </label>
            <label>
                <input type="checkbox" name="check_bad" style="width: 42px;position:relative;top: 17px">忽略冲突课程
            </label-->
            <label>
                <select name="course_dept">
                    <option value="Course Dept">Course Dept</option>
                    <option value="金融系">金融系</option>
                    <option value="创新创业学院">创新创业学院</option>
                    <option value="社会科学中心">社会科学中心</option>
                    <option value="高等教育研究中心">高等教育研究中心</option>
                    <option value="人文科学中心">人文科学中心</option>
                    <option value="生物医学工程系">生物医学工程系</option>
                    <option value="化学系">化学系</option>
                    <option value="海洋科学与工程系">海洋科学与工程系</option>
                    <option value="生物系">生物系</option>
                    <option value="思想政治教育与研究中心">思想政治教育与研究中心</option>
                    <option value="地球与空间科学系">地球与空间科学系</option>
                    <option value="物理系">物理系</option>
                    <option value="艺术中心">艺术中心</option>
                    <option value="力学与航空航天工程系">力学与航空航天工程系</option>
                    <option value="体育中心">体育中心</option>
                    <option value="环境科学与工程学院">环境科学与工程学院</option>
                    <option value="数学系">数学系</option>
                    <option value="计算机科学与工程系">计算机科学与工程系</option>
                    <option value="电子与电气工程系">电子与电气工程系</option>
                    <option value="机械与能源工程系">机械与能源工程系</option>
                    <option value="医学院">医学院</option>
                    <option value="语言中心">语言中心</option>
                    <option value="材料科学与工程系">材料科学与工程系</option>
                </select>
            </label>
            <!--label>
                <select name="course_week">
                    <option value="Course Week">Week</option>
                    <option value="1">1</option>
                    <option value="2">2</option>
                    <option value="3">3</option>
                    <option value="4">4</option>
                    <option value="5">5</option>
                    <option value="6">6</option>
                    <option value="7">7</option>
                </select>
            </label>
            <label>
                <select name="course_start">
                    <option value="Course Start">Start</option>
                    <option value="1">1</option>
                    <option value="2">2</option>
                    <option value="3">3</option>
                    <option value="4">4</option>
                    <option value="5">5</option>
                    <option value="6">6</option>
                    <option value="7">7</option>
                    <option value="8">8</option>
                    <option value="9">9</option>
                    <option value="10">10</option>
                    <option value="11">11</option>
                </select>
            </label>
            <label>
                <select name="course_end">
                    <option value="Course End">End</option>
                    <option value="1">1</option>
                    <option value="2">2</option>
                    <option value="3">3</option>
                    <option value="4">4</option>
                    <option value="5">5</option>
                    <option value="6">6</option>
                    <option value="7">7</option>
                    <option value="8">8</option>
                    <option value="9">9</option>
                    <option value="10">10</option>
                    <option value="11">11</option>
                </select>
            </label-->
            <button type="submit" class="label-info" style="width:100px;">Search</button>
        </form>
        <BR>
        <div  style="margin: 120px auto 0 auto;">
            <div>
                <?php
                if ((isset($_POST['course_id'])||isset($_POST['course_name'])||isset($_POST['course_teacher'])||
                isset($_POST['check_zero'])||isset($_POST['check_bad'])||isset($_POST['course_dept'])||
                isset($_POST['course_week'])||isset($_POST['course_start'])||isset($_POST['course_end']))) {
                ?>
                <table class="table table-bordered table-hover">
                    <thead>
                    <tr>
                        <th colspan="8" style="font-size: 20px;text-align:center;">请选择课程班级</th>
                    </tr>
                    <tr>
                        <th>Course ID</th>
                        <th>Course Name</th>
                        <th>Teacher Name</th>
                        <th>Time and Location</th>
                        <th>Credit and Length</th>
                        <th>Prerequisite</th>
                        <th>Class Dept</th>
                        <th>Options</th>
                    </tr>
                    <?php
                    echo "</thead><tbody>";
                    $sql="select a.course_id,b.name,class_id,coursedept from (
                    (select * from courses where course_id like $1 and name like $2)a inner join
                    (select * from classes where teacher_name like $3 and coursedept like $4 )b
                    on a.course_id=b.course_id)";
                    $ret = pg_query_params($db, $sql, array(
                        $_POST['course_id']==='Course ID'||$_POST['course_id']==""?'%':"%".strtoupper(trim($_POST['course_id']))."%",
                        $_POST['course_name']==='Course Name'||$_POST['course_name']==""?'%':"%".strtoupper(trim($_POST['course_name']))."%",
                        $_POST['course_teacher']==='Course Teacher'||$_POST['course_teacher']==""?'%':"%".strtoupper(trim($_POST['course_teacher']))."%",
                        $_POST['course_dept']==='Course Dept'||$_POST['course_dept']==""?'%':"%".strtoupper(trim($_POST['course_dept']))."%"));
                    if ($ret) {
                        while ($row = pg_fetch_row($ret)) {
                            printTable($row, 1);
                        }
                    }
                    echo "</tbody></table>";
                    }
                    ?>
                    <br>&nbsp;<br>
                    <table class="table table-bordered table-hover">
                        <thead>
                        <tr>
                            <th colspan="8" style="font-size: 20px;text-align:center;">已选课程</th>
                        </tr>
                        <tr>
                            <th>Course ID</th>
                            <th>Course Name</th>
                            <th>Teacher Name</th>
                            <th>Time and Location</th>
                            <th>Credit and Length</th>
                            <th>Prerequisite</th>
                            <th>Class Dept</th>
                            <th>Options</th>
                        </tr>
                        </thead>
                        <tbody>
                        <?php
                        $sql = "select class_id from  course_chosen where student_id = $1 ;";
                        $ret = pg_query_params($db, $sql, array($_SESSION['username']));
                        while ($cur = pg_fetch_row($ret)) {
                            $sql = "select course_id,name,class_id,coursedept from classes where class_id= $1 ;";
                            $ret_ = pg_query_params($db, $sql, array(strtoupper(trim($cur[0]))));
                            while ($row = pg_fetch_row($ret_)) {
                                printTable($row, 2);
                            }
                        }
                        ?>
                        </tbody>
                    </table>
            </div>
        </div>
    </div>
</body>
</html>