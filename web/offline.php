<?php
require 'offline.inc.php';

//// command line php script for generating offline documentation
//// must be executed IN this directory, first argument being the
//// absolute path to the php cli binary

//// first language
chdir("reference");
clean();
//exec($argv[1] ." index.php ../ ". $argv[1] ." > index.html");
//// environment
chdir("environment");
clean();
//exec($argv[1] ." index.php ../../ ". $argv[1] ." > index.html");
//// libraries
chdir("../libraries");
clean();
exec($argv[1] ." index.php ../../ ". $argv[1] ." > index.html");
?>
