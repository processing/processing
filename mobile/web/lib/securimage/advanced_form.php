<?php

/*
    This is the contact form I use on my site
    Feel free to modify it and use it as a guide
    to setting up securimage for your site.
*/

$to = "YOU@YOURSITE.COM";


if (!isset($_POST['submit'])) {

  showForm();

} else { //form submitted

  $error = 0;


  if(empty($_POST['name'])) {
    $error = 1;
    $errstr[] = "Please enter a name";
  }

  if(!preg_match("/^(?:[\w\d]+\.?)+@(?:(?:[\w\d]\-?)+\.)+\w{2,4}$/", $_POST['email'])) {
    $error = 1;
    $errstr[] = "Please enter a valid email address";
  }

  if (empty($_POST['subject'])) {
    $error = 1;
    $errstr[] = "Please enter a subject";
  }

  if(empty($_POST['message']) || preg_match("/^enter your message here$/i", $_POST['message'])) {
    $error = 1;
    $errstr[] = "Please enter a message";
  }

  if(empty($_POST['imagetext'])) {
    $error = 1;
    $errstr[] = "Please validate the image code";
  } else {
    include "securimage.php";
    $img = new securimage();
    $valid = $img->check($_POST['imagetext']);

    if(!$valid) {
      $error = 1;
      $errstr[] = "The code you entered was incorrect";
    }
  }

  if ($error == 1) {
    echo "<center>\n<font style=\"color: #FF0000\">\n";
    foreach($errstr as $err) {
      echo "<li> " . $err . "</li>\n";
    }
    echo "</font>\n</center>\n<br />\n\n";

    showForm();

  } else {
    @mail($to, "Site Contact - " . $_POST['subject'], 
    "Drew,\nOn " . date("r") . ", " . $_POST['name'] . " " . $_POST['email'] .
    " sent the following message.\nReason " .
    $_POST['reason'] . "\n\n" . stripslashes($_POST['message']), "From: " . $_POST['email']);

    echo "<center>\nThanks for contacting me.  I'll try to get back to you as soon as I can.  Thanks for 
          visiting my website.  If I don't get back to you within one week, please fill out the form again.<br /><br />"
        ."Click <a href=\"#\" onclick=\"self.close()\">here</a> to close this window.";

  }

} //else submitted



function showForm()
{
  $_POST['message'] = @htmlspecialchars(@$_POST['message']);

  echo <<<EOD
<form method="POST">
<table class="dl" cellpadding="5" cellspacing="1" width="95%" align="center">
  <tr>
    <td class="head" align="center" colspan="2">Contact Form</td>
  </tr>
  <tr>
    <td class="body" align="left">Name</td>
    <td class="body" align="center"><input type="text" name="name" value="{$_POST['name']}" /></td>
  </tr>
  <tr>
    <td class="body" align="left">Email Address</td>
    <td class="body" align="center"><input type="text" name="email" value="{$_POST['email']}" /></td>
  </tr>
  <tr>
    <td class="body" align="left">Subject</td>
    <td class="body" align="center"><input type="text" name="subject" value="{$_POST['subject']}" /></td>
  </tr>
  <tr>
    <td class="body" align="left">Contact Reason</td>
    <td class="body" align="center"><select name="reason">
                                    <option value="Question" selected>Question</option>
                                    <option value="Comment">Comment</option>
                                    <option value="Suggestion">Suggestion</option>
                                    <option value="Bug">Bug Report</option>
                                    <option value="Other">Other</option>
                                    </select>
    </td>
  </tr>
  <tr>
    <td class="body" align="center" colspan="2"><textarea name="message" rows="8" cols="50">{$_POST['message']}</textarea></td>
  </tr>
  <tr>
    <td class="body" align="center" colspan="2"><img src="securimage_show.php"></td>
  </tr>
  <tr>
    <td class="body" align="left">Enter the text above</td>
    <td class="body" align="center"><input type="text" name="imagetext" /></td>
  </tr>
  <tr>
    <td class="body" align="center" colspan="2"><input type="submit" name="submit" value="Send Form" /></td>
  </tr>
</table>
</form>
EOD;
}

?>
