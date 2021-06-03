<?php
$time_array = array("
['1', '2', '3', '4', '5', '6', '7', '8', '9', '10', '11']" => "1-11周",
    "['9', '10', '11', '12', '13', '14', '15']" => "9-15周",
    "['1', '2', '3', '4', '5', '6', '7', '8', '9', '10', '11', '12', '13', '14', '15']" => "1-15周",
    "['1', '2', '3', '4', '5', '6', '7', '8', '9', '10', '11', '12']" => "1-12周",
    "['2', '4', '6', '8']" => "2-8双周",
    "['1', '3', '5', '7', '9', '11', '13', '15']" => "1-15单周",
    "['1', '2', '3', '4', '5', '6', '7', '8', '9', '10', '12', '13', '14', '15']" => "1-10,12-15周",
    "['2', '4', '6', '8', '10', '12', '14']" => "2-14双周",
    "['1', '2', '3', '4']" => "1-4周",
    "['1', '2', '3', '4', '5', '6', '7', '8']" => "1-8周");
$week_array = array(1 => "星期一", 2 => "星期二", 3 => "星期三", 4 => "星期四", 5 => "星期五", 6 => "星期六", 7 => "星期日");

function printTable(array $row,int $mode){
    global $db, $time_array, $week_array;
    $sql = "select teacher_name from classes where class_id =$1";
    $ret_t = pg_query_params($db, $sql, array($row[2]));
    $row_t = pg_fetch_row($ret_t);
    $sql = "select name,credit,course_hour,prerequisite from courses where course_id= $1;";
    $ret_course = pg_query_params($db, $sql, array($row[0]));
    $row_course = pg_fetch_row($ret_course);
    echo "<tr>";
    echo "<td>" . $row[0] . "</td>";
    echo "<td>" . $row_course[0] . " " . $row[1] . "</td>";
    $cnt = 0;
    echo "<td>" . $row_t[$cnt++];
    while ($cnt < count($row_t)) {
        echo "," . $row_t[$cnt++];
    }
    echo "</td><td>";
    $sql = "select location,weeklist,classtime,weekday from time_table where class_id=$1;";
    $ret_class = pg_query_params($db, $sql, array($row[2]));
    while ($row_class = pg_fetch_row($ret_class)) {
        echo $time_array[$row_class[1]] . ", " . $week_array[$row_class[3]] . "第" . $row_class[2] . "节 " . $row_class[0] . "教室<br>";
    }
    echo "</td>";
    echo "<td>" . $row_course[1] . "学分, " . $row_course[2] . "学时</td>";
    $pre = str_replace("course_learned like '% ", "", $row_course[3]);
    $pre = str_replace(" %'", "", $pre);
    echo "<td>" . $pre . "</td>";
    echo "<td>" . $row[3] . "</td>";
    if($mode===1) {
        echo '<td><button class="label-success" style="width:100px;margin-top: 0" onclick=
"$.ajax({type:\'post\',url:\'selectionServer.php\',data: \'class_id=' . $row[2] . '&operation=selectCourse\',}
).success(function(m){alert(m);window.location.reload();})"
>确认选择</button></td></tr>';
    }elseif ($mode===2){
        echo '<td><button class="label-danger" style="width:100px;margin-top: 0" onclick=
"$.ajax({type:\'post\',url:\'selectionServer.php\',data: \'class_id=' . $row[2] . '&operation=withdrawCourse\',}
).success(function(m){alert(m);window.location.reload();})"
>退课</button></td></tr>';
    }
}
