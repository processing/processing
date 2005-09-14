<?

require('../config.php');
require('lib/Ref.class.php');

$file = CONTENTDIR.$_GET['file'];

$ref = new Ref($file);

?>