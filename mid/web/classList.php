<?php
session_start();
function redirect($flag, $s, $url)
{
    if ($flag) {
        echo "<script>alert(\"$s\");</script>";
    }
    echo "<script>window.location.href = '$url';</script>";
}

if (!isset($_SESSION['username'])) {
    redirect(true, "Please login first!", "login.php");
    die();
}
if (isset($_SESSION['gm']) && $_SESSION['gm'] === true) {
    if(isset($_GET['username'])) {
        $stu_id = $_GET['username'];
    }else{
        Header("Location:classList.php?username=11111111");
        die();
    }
} else {
    $stu_id = $_SESSION['username'];
}
$db = null;
include("connection.php");
$week_array = array(1 => "星期一", 2 => "星期二", 3 => "星期三", 4 => "星期四", 5 => "星期五", 6 => "星期六", 7 => "星期日");
$table_array = array(0 => "label-default", 1 => "label-primary", 2 => "label-success", 3 => "label-info", 4 => "label-warning", 5 => "label-danger");
?>
<!DOCTYPE html>
<html lang="en" class="no-js">
<head>
    <meta charset="utf-8"/>
    <title>课程表</title>
    <link rel="stylesheet" href="css/font-awesome.min.css">
    <link rel="stylesheet" href="css/style.css">
</head>
<!--body style="background-image: url('img/BG.png');margin:0 auto;"-->
<body>
<button class="label label-danger" style="position: absolute;top: 10px;right: 10px;width: 100px;margin-top: 0"
        onclick="window.location.href='logout.php'">Log out
</button>
<button class="label label-warning" style="position: absolute;top: 10px;right: 150px;width: 100px;margin-top: 0"
        onclick="window.location.href='detail.php'">返回
</button>
<script src="js/jquery-1.8.2.min.js"></script>
<br>
<div class="col-md-12" style="position: absolute;top: 20%;width: 100%">
    <table class="table table-bordered table-condensed">
        <thead>
        <tr>
            <th colspan="8" style="font-size: 20px;text-align:center;">
                <?php
                $week = (getdate()["seconds"] % 15 + 1);
                echo "第" . $week . "周";
                ?>
                课程表
            </th>
        </tr>
        <tr>
            <th>节次\星期</th>
            <?php
            for ($i = 1; $i < 8; $i++) {
                echo "<th>" . $week_array[$i] . "</th>";
            }
            ?>
        </tr>
        </thead>
        <tbody>
        <?php
        for ($i = 1; $i < 12; $i += 2) {
            echo "<tr>";
            echo "<td>第" . $i . "-" . ($i + 1) . "节</td>";
            for ($j = 1; $j < 8; $j++) {
                if(isset($_SESSION['th'])&&$_SESSION['th']===true) {
                    $sql = "select location,res.class_id from
                                ((SELECT class_id as ci from course_teach where teacher_id = $1)cids
                                left join time_table as tb on cids.ci = tb.class_id)res where res.weeklist like 
                                '%" . $week . "%' and res.weekday = " . $j . " and (
                                res.classtime like '" . $i . "-%'or res.classtime like '%-" . $i . "'); ";
                }else{
                    $sql = "select location,res.class_id from
                                ((SELECT class_id as ci from course_chosen where student_id = $1)cids
                                left join time_table as tb on cids.ci = tb.class_id)res where res.weeklist like 
                                '%" . $week . "%' and res.weekday = " . $j . " and (
                                res.classtime like '" . $i . "-%'or res.classtime like '%-" . $i . "'); ";
                }
                $res = pg_query_params($db, $sql, array($stu_id));
                $row = pg_fetch_row($res);
                if (isset($row[0])) {
                    $sql = "select name,course_id,teacher_name from classes where class_id = $1";
                    $res = pg_query_params($db, $sql, array($row[1]));
                    $row_0 = pg_fetch_row($res);
                    $sql = "select name from courses where course_id = $1;";
                    $res = pg_query_params($db, $sql, array($row_0[1]));
                    $row_1 = pg_fetch_row($res);
                    echo "<td class=\"" . $table_array[(base_convert(substr(md5($row_1[0] . $row_0[0]), 0, 3), 16, 10) % 6)] .
                        "\">" . $row_1[0] . " " . $row_0[0] . "<br>" . $row[0] . " &nbsp;" . $row_0[2] . "</td>";
                } else {
                    echo "<td></td>";
                }
            }
            echo "</tr>";
        }
        ?>
        </tbody>
    </table>
</div>
</body>
</html>