<?php

//// needs the database, so include db header
require_once '../../db.inc.php';

//// needs the generation code
require_once 'generate.inc.php';

//// regenerate!
links_generate();

//// go back to index page
header('Location: ../index.php');
?>