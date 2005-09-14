<?

require('../config.php');

// arguments
$title = isset($_POST['title']) ? $_POST['title'] : false;
$section = isset($_POST['section']) ? $_POST['section'] : false;

$page = new Page($title, $section);
echo '<pre>';
echo htmlspecialchars($page->out());

?>