<?php

//// get device profile, if any

//// read jad file into array
$lines = file($_GET['n'] .'.jad');

//// compare profile and configuration compatibility
$compatible = true;

if ($compatible || isset($_GET['f'])) {
    //// increment and fetch download count for id
    $id = 1;

    //// set content type for jad file
    header("Content-Type: text/vnd.sun.j2me.app-descriptor");

    //// write out jad with additional params
    foreach ($lines as $l) {
        echo $l;
    }
    echo "MP-Id: {$id}\n";
    echo "MP-UserAgent: {$_SERVER['HTTP_USER_AGENT']}\n";
} else {
    //// output compatibility prompt
}
?>