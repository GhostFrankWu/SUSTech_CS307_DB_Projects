<?php
session_start();
function redirect($flag, $s, $url)
{
    if ($flag) {
        echo "<script>alert(\"$s\");</script>";
    }
    echo "<script>window.location.href = \"$url\";</script>";
}

if (!(isset($_SESSION['gm']) && $_SESSION['gm'] === true)) {
    redirect(true, "Please login first!", "login.php");
    die();
}
$db = null;
include("connection.php");
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
<button class="label label-danger" style="position: absolute;top: 10px;right: 10px;width: 100px;margin-top: 0"
        onclick="window.location.href='logout.php'">Log out
</button>
<button class="label label-info" style="position: absolute;top: 10px;right: 150px;width: 100px;margin-top: 0"
        onclick=changePwd()>修改密码
</button>
<script>
    function changePwd() {
        let usr = prompt("请输入学生id");
        if (usr !== null) {
            let pwd = prompt("请再次输入新密码");
            if (pwd !== null) {
                $.ajax({type: 'post', url: 'selectionServer.php', data:
                        'password=' + pwd + '&username='+usr+'&operation=changePwd'}
                ).success(function (m) {
                    alert(m);
                })
            }
        }
    }
</script>
<script src="js/jquery-1.8.2.min.js"></script>
<h1>选课系统sql命令行</h1>
<div class="col-md-12"><p>Welcome back, dear
        <?php echo "Admin " . $_SESSION['username'] . "!</p>"; ?>
</div>
<form method="post" style="float: left">
    <label>
        <input type="text" name="Query" placeholder="<?php echo "Query"; ?>"
               style="width:800px;color: #0f0f0f;height: 40px">
    </label>
    <button type="submit" class="label-info" style="width:100px;">Query</button>
</form>
<br>
<div style="margin: 120px auto 0 auto;">
    <div>
        <table class="table table-bordered table-hover">
                <?php
                if (isset($_POST['Query']) && $_POST['Query'] != "Query") {
                    echo "<thead><tr>";
                    $res = pg_query($db, $_POST['Query']);
                    $tab_ = pg_fetch_assoc($res);
                    for ($i = 0; $i < count($tab_); $i++) {
                        $e = pg_field_name($res, $i);
                        echo "<th>{$e}</th>";
                    }
                    echo "</tr></thead><tbody>";
                    do {
                        echo "<tr>";
                        foreach ($tab_ as $val) {
                            echo "<td>{$val}</td>";
                        }
                        echo "</tr>";
                    } while ($tab_ = pg_fetch_assoc($res));
                    echo "</tbody>";
                }
                ?>
        </table>

    </div>
</div>
</body>
</html>