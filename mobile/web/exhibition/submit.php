<?php

//// needs the database, so include db header
require_once '../db.inc.php';

//// code to handle network link page regeneration
require_once 'network/generate.inc.php';

//// code for anti-spam image
require_once 'recaptchalib.php';
require_once 'recaptchakeys.inc.php';

//// validate fields
$errors = array();
if (isset($_POST['submit'])) {
    $_POST['by'] = trim($_POST['by']);
    $_POST['name'] = trim($_POST['name']);
    $_POST['url'] = trim($_POST['url']);
    if ($_POST['by'] == '') {
	$errors['by'] = 'Please enter your name.';
    }
    if ($_POST['name'] == '') {
	$errors['name'] = 'Please enter a title for your work.';
    }
    if ($_POST['url'] == '') {
	$errors['url'] = 'Please enter the URL for your work.';
    } else if (!preg_match('/^(http|https|ftp):\/\/(([A-Z0-9][A-Z0-9_-]*)(\.[A-Z0-9][A-Z0-9_-]*)+)(:(\d+))?\//i', $_POST['url'], $m)) {
        $errors['url'] = 'URL is not well formatted. http://somedomain.abc/';
    }
    if ($_POST['submit'] == 'Submit') {
        $resp = recaptcha_check_answer ($privatekey,
                                        $_SERVER["REMOTE_ADDR"],
                                        $_POST["recaptcha_challenge_field"],
                                        $_POST["recaptcha_response_field"]);
        if (!$resp->is_valid) {
            $errors['code'] = "The reCAPTCHA wasn't entered correctly. Please try again.";
        }
        if (count($errors) == 0) {
            //// format data to save
            $data = array();
            $data[] = htmlentities($_POST['by']);
            $data[] = htmlentities($_POST['name']);
            $data[] = htmlentities($_POST['url']);
            $data[] = strval(time());

            //// insert into database
            $link = db_connect();
            $result = mysql_query("INSERT INTO links (name, title, url, submitted) VALUES ('". $_POST['by'] ."', '". $_POST['name'] ."', '". $_POST['url'] ."', ". strval(time()) .")");

            //// regenerate links pages
            links_generate();

            //// redirect back to exhibition home
            header('Location: index.php');
            exit;
        }
    }
} else {
    $_POST['url'] = 'http://';
}

$PAGE_TITLE = "Add Network Link \ Mobile Processing";
$PAGE_LINKHEADER = true;

require '../header.inc.php';
?>
<div class="column2x">
<img src="images/network.png"><br />
<br />
<br />
<?php
if (isset($_POST['submit'])) {
?>
<b>Add Network Link (Step 2 of 2)</b><br />
<br />
Preview:<br />
<br />
<a href="<?php echo $_POST['url'] ?>"><?php echo htmlentities($_POST['name']) ?></a><br />
<?php echo htmlentities($_POST['by']) ?><br />
<span class="date"><?php echo date("d M 'y") ?></span><br />
<br />
<br />
<?php
} else {
?>
<b>Add Network Link (Step 1 of 2)</b><br />
<br />
<?php
}
?>
<form action="submit.php" method="post">
<label>Your name:</label><br />
<input name="by" type="text" size="48" maxlength="48" 
       value="<?php echo $_POST['by'] ?>" /><br />
<?php
if (isset($errors['by'])) {
?>
<p class="error"><?php echo $errors['by'] ?></p>
<?php
}
?>
<br />
<label>Work title:</label><br />
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
<?php
if (isset($_POST['submit'])) {
?>
<input type="submit" name="submit" value="Refresh Preview" /><br />
<br />
<label>Type the two words below to submit:</label><br />
<?php
echo recaptcha_get_html($publickey);
if (isset($errors['code'])) {
?>
<p class="error"><?php echo $errors['code'] ?></p>
<?php
}
?>
<br />
<input type="submit" name="submit" value="Submit" />
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
