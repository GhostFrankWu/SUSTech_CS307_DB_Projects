<?php
error_reporting(E_ERROR);
ini_set("display_errors","Off");
$host    = "host=���ݿ������������ʵ��ַ";
$port    = "port=���ݿ���������������˿�";
$dbname   = "dbname=cs307";
$credentials = "user=serverquery password=�������";
$db = pg_connect( "$host $port $dbname $credentials" );
