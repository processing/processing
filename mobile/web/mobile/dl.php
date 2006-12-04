<?php

//// get device profile, if any

//// read jad file into array
$lines = file($_GET['n'] .'/proguard/'. $_GET['n'] .'.jad');

//// compare profile and configuration compatibility
$compatible = true;

if ($compatible || isset($_GET['f'])) {
    //// increment and fetch download count for id
    $id = 1;

    //// set content type for jad file
    header("Content-Type: text/vnd.sun.j2me.app-descriptor");

    //// write out jad with additional params, full url to jar file
    foreach ($lines as $l) {
        if (stristr($l, "MIDlet-Jar-URL:") !== false) {        
            $l = 'MIDlet-JAR-URL: http://'. $_SERVER['SERVER_NAME'] . substr($_SERVER['PHP_SELF'], 0, strrpos($_SERVER['PHP_SELF'], "/") + 1) . $_GET['n'] .'/proguard/'. ltrim(substr(strrchr($l, ":"), 1));
        }
        echo $l;
    }
    echo "MP-Id: {$id}\n";
    echo "MP-UserAgent: {$_SERVER['HTTP_USER_AGENT']}\n";
} else {
    //// output compatibility prompt
}
?>