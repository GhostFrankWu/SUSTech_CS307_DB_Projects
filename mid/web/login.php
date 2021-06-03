<?php
if (!session_id()) {
    session_start();
}
$db = null;
include("connection.php");
?>
<!DOCTYPE html>
<html lang="en" class="no-js">
<head>
    <link rel="icon" href="favicon.ico" type="image/x-icon"/>
    <meta charset="utf-8">
    <title>hceTSUS选课系统</title>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="description" content="">
    <meta name="author" content="">
    <link rel="stylesheet" href="css/supersized.css">
    <link rel="stylesheet" href="css/style.css">

</head>

<body>

<div class="page-container"
     style=" height: 450px;width:800px; background-image:url(img/card.png);background-size:100% 100%;color: #ffffff">
    <h1 class="dynamic_rainbow"><br>Login<br>&nbsp;</h1>
    <form method="post">
        <i class="fa fa-user-circle " style="font-size:30px;">&nbsp;&nbsp;</i><label>
            <input type="text" name="username" class="username" placeholder="Username" style="width:250px">
        </label>
        <br>
        <i class="fa fa-key " style="font-size:30px;">&nbsp;&nbsp;</i><label>
            <input type="password" name="password" class="password" placeholder="Password" style="width:250px">
        </label>
        <br>
        <button type="submit" class="submit_button" style="width:250px; margin-left:40px;">Login Now!</button>
    </form>

</div>
<script src="js/jquery-1.8.2.min.js"></script>
<script src="js/supersized.3.2.7.min.js"></script>
<script>jQuery(function ($) {
        $.supersized({
            slide_interval: 4000,
            transition: 1,
            transition_speed: 1000,
            performance: 1,
            min_width: 0,
            min_height: 0,
            vertical_center: 1,
            horizontal_center: 1,
            fit_always: 0,
            fit_portrait: 1,
            fit_landscape: 0,
            slide_links: 'blank',
            slides: [{image: 'img/1.jpg'}, {image: 'img/2.jpg'}, {image: 'img/3.jpg'}]
        });
    });</script>
</body>
<div style="text-align:center;">
</div>
</html>
<?php
function redirect($flag, $s, $url)
{
    if ($flag) {
        echo "<script>alert(\"$s\");</script>";
    }
    echo "<script>window.location.href = \"$url\";</script>";
}

if (isset($_SESSION['username'])) {
    redirect(false, null, "detail.php");
}
if (!$db) {
    echo '<script>console.log("database offline");</script>';
} else {
    if (isset($_POST['username']) || isset($_POST['password'])) {
        if (isset($_POST['username']) && isset($_POST['password'])) {
            if ($_POST['username'] != "Username") {
                $username = $_POST['username'];
                $pwd = hash("sha256", $_POST['password']);
                $sql = 'select count(*) from authentication where name= $1 and h3k4d_passwd = $2 ;';
                $ret = pg_query_params($db, $sql, array($username, $pwd));
                $row = pg_fetch_row($ret);
                if ($row[0] == 0) {
                    if (is_numeric($username)) {
                        if($username<29999999||$username>99999999) {//behind condition only for test
                            $sql = 'select h3k4d_password_no_you_cant_guess_this_column_name_no_injection from students where student_id = CAST($1 as integer );';
                            $ret = pg_query_params($db, $sql, array($username));
                            $row = pg_fetch_row($ret);
                            if (($row[0] == $pwd) || ($row[0] == null && $pwd == hash("sha256", $username))) {
                                $_SESSION['username'] = $username;
                                if ($row[0] == null) {
                                    $_SESSION['reset'] = true;
                                }
                                redirect(false, null, "detail.php");
                            }
                        }else{
                            $sql = 'select h3k4d_passw0rd from teachers where id = $1;';
                            $ret = pg_query_params($db, $sql, array($username));
                            $row = pg_fetch_row($ret);
                            if (($row[0] == $pwd) || ($row[0] == null && $pwd == hash("sha256", '666666'))) {
                                $_SESSION['username'] = $username;
                                $_SESSION['th'] = true;
                                if ($row[0] == null) {
                                    $_SESSION['reset'] = true;
                                }
                                redirect(false, null, "detailTeacher.php");
                            }
                        }
                    }
                    redirect(true, "Password incorrect!", "login.php");
                    exit();
                } else {
                    $_SESSION['username'] = $username;
                    $_SESSION['gm'] = true;
                    redirect(false, null, "detailAdmin.php");
                }
            }
        } else {
            echo '<script>alert("Username or password can not be null !");</script>';
        }
    }
}
?>
