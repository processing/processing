<?php

//// needs the database, so include db header
require_once '../../db.inc.php';

//// code to handle network link page regeneration
require_once 'generate.inc.php';

//// validate fields
$errors = array();
if (isset($_POST['submit'])) {
    $_POST['name'] = trim($_POST['name']);
    $_POST['title'] = trim($_POST['title']);
    $_POST['url'] = trim($_POST['url']);
    $_POST['imgurl'] = trim($_POST['imgurl']);
    $_POST['description'] = trim($_POST['description']);
    $_POST['mobileurl'] = trim($_POST['mobileurl']);
    $_POST['mobileimgurl'] = trim($_POST['mobileimgurl']);
    $_POST['jadurl'] = trim($_POST['jadurl']);
    if ($_POST['name'] == '') {
        $errors['name'] = 'Please enter the artists name.';
    }
    if ($_POST['title'] == '') {
        $errors['title'] = 'Please enter a title for the work.';
    }
    if ($_POST['url'] == '') {
        $errors['url'] = 'Please enter the URL for the work.';
    } else if (!preg_match('/^(http|https|ftp):\/\/(([A-Z0-9][A-Z0-9_-]*)(\.[A-Z0-9][A-Z0-9_-]*)+)(:(\d+))?\//i', $_POST['url'], $m)) {
        $errors['url'] = 'URL is not well formatted. http://somedomain.abc/';
    }
    if ($_POST['submit'] == 'Submit') {
        if (count($errors) == 0) {
            //// format data to save
            $data = array();
            $data['name'] = htmlentities($_POST['name']);
            $data['title'] = htmlentities($_POST['title']);
            $data['url'] = $_POST['url'];
            $data['imgurl'] = $_POST['imgurl'];
            $data['description'] = htmlentities($_POST['description']);
            $data['mobileurl'] = $_POST['mobileurl'];
            $data['mobileimgurl'] = $_POST['mobileimgurl'];
            $data['jadurl'] = $_POST['jadurl'];
            $data['submitted'] = strval(time());

            //// insert into database
            $link = db_connect();
            $query = "INSERT INTO curated (name, title, url, imgurl, description, mobileurl, mobileimgurl, jadurl, submitted) VALUES (\"". $data['name'] ."\", \"". $data['title'] ."\", \"". $data['url'] ."\", \"". $data['imgurl'] ."\", \"". $data['description'] ."\", \"". $data['mobileurl'] ."\", \"". $data['mobileimgurl'] ."\", \"". $data['jadurl'] ."\", ". $data['submitted'] .")";
            error_log($query);
            $result = mysql_query($query);

            //// regenerate links pages
            links_generate();

            //// redirect back to exhibition home
            header('Location: ../index.php');
            exit;
        }
    }
} else {
    $_POST['url'] = 'http://';
}

$PAGE_TITLE = "Add Curated Item \ Mobile Processing";
$PAGE_LINKHEADER = true;

require '../../header.inc.php';
?>
<div class="column2x">
<img src="../images/header.png"><br />
<br />
<br />
<?php
if (isset($_POST['submit'])) {
?>
<b>Add Curated Item (Step 2 of 2)</b><br />
<br />
Preview:<br />
<br />
<?php
echo format_page_link($_POST);
?>
<br />
<br />
<?php
} else {
?>
<b>Add Curated Item (Step 1 of 2)</b><br />
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
<label>Work title:</label><br />
<input name="title" type="text" size="48" maxlength="48" 
       value="<?php echo $_POST['title'] ?>" /><br />
<?php
if (isset($errors['title'])) {
?>
<p class="error"><?php echo $errors['title'] ?></p>
<?php
}
?>
<br />
<label>URL:</label><br />
<input name="url" type="text" size="48" 
       value="<?php echo $_POST['url'] ?>" /><br />
<?php
if (isset($errors['url'])) {
?>
<p class="error"><?php echo $errors['url'] ?></p>
<?php
}
?>
<br />
<label>Image URL:</label><br />
<input name="imgurl" type="text" size="48" 
       value="<?php echo $_POST['imgurl'] ?>" /><br />
<?php
if (isset($errors['imgurl'])) {
?>
<p class="error"><?php echo $errors['imgurl'] ?></p>
<?php
}
?>
<br />
<label>Descripton:</label><br />
<textarea name="description" rows="10" cols="50">
<?php echo $_POST['description'] ?>
</textarea>
<?php
if (isset($errors['description'])) {
?>
<p class="error"><?php echo $errors['description'] ?></p>
<?php
}
?>
<br />
<label>Mobile URL:</label><br />
<input name="mobileurl" type="text" size="48" 
       value="<?php echo $_POST['mobileurl'] ?>" /><br />
<?php
if (isset($errors['mobileurl'])) {
?>
<p class="error"><?php echo $errors['mobileurl'] ?></p>
<?php
}
?>
<br />
<label>Mobile Image URL:</label><br />
<input name="mobileimgurl" type="text" size="48" 
       value="<?php echo $_POST['mobileimgurl'] ?>" /><br />
<?php
if (isset($errors['mobileimgurl'])) {
?>
<p class="error"><?php echo $errors['mobileimgurl'] ?></p>
<?php
}
?>
<br />
<label>JAD URL:</label><br />
<input name="jadurl" type="text" size="48" 
       value="<?php echo $_POST['jadurl'] ?>" /><br />
<?php
if (isset($errors['jadurl'])) {
?>
<p class="error"><?php echo $errors['jadurl'] ?></p>
<?php
}
?>
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
 require '../../footer.inc.php';
?>
