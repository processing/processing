<?php

require_once '../mobile/db.inc.php';

$link = db_connect();

//// get device profile, if any

//// read jad file into array
$lines = file($_GET['n'] .'.jad');

//// compare profile and configuration compatibility
$compatible = true;

if ($compatible || isset($_GET['f'])) {
    //// add to download log and get id
    mysql_query("INSERT INTO downloads (name, useragent) VALUES ('".
                $_GET['n'] ."', '". $_SERVER['HTTP_USER_AGENT'] ."')");
    $id = mysql_insert_id();

    //// set content type for jad file
    header("Content-Type: text/vnd.sun.j2me.app-descriptor");

    //// write out jad with additional params, full url to jar file
    foreach ($lines as $l) {
        if (stristr($l, "MIDlet-Jar-URL:") !== false) {        
            $l = 'MIDlet-Jar-URL: http://'. $_SERVER['SERVER_NAME'] .'/'. $_GET['n'] .".jar\n";
        }
        echo $l;
    }
    echo "MP-Id: {$id}\n";
    echo "MP-UserAgent: {$_SERVER['HTTP_USER_AGENT']}\n";
} else {
    //// output compatibility prompt
}
?>