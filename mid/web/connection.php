<?php
error_reporting(E_ERROR);
ini_set("display_errors","Off");
$host    = "host=数据库服务器主机真实地址";
$port    = "port=数据库服务器主机监听端口";
$dbname   = "dbname=cs307";
$credentials = "user=serverquery password=你的密码";
$db = pg_connect( "$host $port $dbname $credentials" );
