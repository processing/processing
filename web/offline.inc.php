<?php
//// function to delete old generated files
function clean() {
    if ($handle = opendir('.')) {
        while (false !== ($file = readdir($handle))) {
            if (strcmp(substr($file, -5), ".html") == 0) {
                unlink($file);
            }
        }
        closedir($handle);
    }
}

function reflink($filename) {
    global $offline, $argv;
    if ($offline) {
        $url = $filename .".html";
        if (!file_exists($url)) {
            exec($argv[2] ." reference.php ". $argv[1] ." ". $argv[2] ." ". $filename ." > ". $filename .".html");
        }
    } else {
        $url = "reference.php?name=". $filename;
    }
    return $url;
}
?>
