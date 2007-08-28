<?php


//// needs the database, so include db header
require_once '../db.inc.php';

//// code to handle network link page regeneration
require_once 'generate.inc.php';

//// validate fields
$errors = array();
if (isset($_POST['submit'])) {
    $_POST['name'] = trim($_POST['name']);
    $_POST['category'] = trim($_POST['category']);
    $_POST['subcategory'] = trim($_POST['subcategory']);
    $_POST['filename'] = trim($_POST['filename']);
    if ($_POST['name'] == '') {
        $errors['name'] = 'Please enter the name of the example.';
    }
    if ($_POST['category'] == '') {
        $errors['title'] = 'Please enter the category.';
    }
    if ($_POST['filename'] == '') {
        $errors['name'] = 'Please enter the sketch filename of the example.';
    }
    if ($_POST['submit'] == 'Submit') {
        if (count($errors) == 0) {
            //// format data to save
            $data = array();
            $data['name'] = htmlentities($_POST['name']);
            $data['category'] = htmlentities($_POST['category']);
            $data['subcategory'] = htmlentities($_POST['subcategory']);
            $data['filename'] = htmlentities($_POST['filename']);
            $data['showonhome'] = isset($_POST['showonhome']) ? "1" : "0";
            $data['submitted'] = strval(time());

            //// insert into database
            $link = db_connect();
            $query = "INSERT INTO examples (name, category, subcategory, filename, showonhome, submitted) VALUES (\"". $data['name'] ."\", \"". $data['category'] ."\", \"". $data['subcategory'] ."\", \"". $data['filename'] ."\", ". $data['showonhome'] .", ". $data['submitted'] .")";
            error_log($query);
            $result = mysql_query($query);

            //// regenerate links pages
            links_generate();

            //// redirect back to exhibition home
            header('Location: index.php');
            exit;
        }
    }
}

$PAGE_TITLE = "Add Example \ Mobile Processing";
$PAGE_LINKHEADER = true;

require '../header.inc.php';
?>
<div class="column2x">
<img src="images/header.png"><br />
<br />
<br />
<?php
if (isset($_POST['submit'])) {
?>
<b>Add Example (Step 2 of 2)</b><br />
<br />
Preview:<br />
<br />
<?php
echo format_example_link($_POST);
?>
<br />
<br />
<?php
} else {
?>
<b>Add Example (Step 1 of 2)</b><br />
<br />
<?php
}
?>
<form action="submit.php" method="post">
<label>Name:</label><br />
<input name="name" type="text" size="48" maxlength="48" 
       value="<?php echo $_POST['name'] ?>" /><br />
<?php
if (isset($errors['name'])) {
?>
<p class="error"><?php echo $errors['name'] ?></p>
<?php
}
?>
<br />
<label>Category:</label><br />
<input name="category" type="text" size="48" maxlength="48" 
       value="<?php echo $_POST['category'] ?>" /><br />
<?php
if (isset($errors['category'])) {
?>
<p class="error"><?php echo $errors['category'] ?></p>
<?php
}
?>
<br />
<label>Subcategory:</label><br />
<input name="subcategory" type="text" size="48" maxlength="48" 
       value="<?php echo $_POST['subcategory'] ?>" /><br />
<?php
if (isset($errors['subcategory'])) {
?>
<p class="error"><?php echo $errors['subcategory'] ?></p>
<?php
}
?>
<br />
<label>Filename:</label><br />
<input name="filename" type="text" size="48" maxlength="48" 
       value="<?php echo $_POST['filename'] ?>" /><br />
<?php
if (isset($errors['filename'])) {
?>
<p class="error"><?php echo $errors['filename'] ?></p>
<?php
}
?>
<br />
<input type="checkbox" name="showonhome" value="true" <?php
if (isset($_POST['showonhome'])) {
    echo "checked";
}
?> /> Show on homepage<br />
<br />
<?php
if (isset($_POST['submit'])) {
?>
<input type="submit" name="submit" value="Refresh Preview" /> <input type="submit" name="submit" value="Submit" /><br />
<br />
<?php
} else {
?>
<input type="submit" name="submit" value="Next >" /><br />
<br />
<?php
}
?>
</form>
</div>
<div class="column">
</div>
<?php
 require '../footer.inc.php';
?>
