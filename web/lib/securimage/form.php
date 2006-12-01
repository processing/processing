<!--
This is a very simple form sending a username and password.
It demonstrates how you can integrate the image script into
your code.
By creating a new instance of the class and passing the 
user entered code as the only parameter [$obj = new securimage("usercode")],
 you can then immediately call $obj->checkCode() which will return
true if the code is correct, or false otherwise.
-->

<html>
<head>
  <title>Securimage Test Form</title>
</head>

<body>

<?php
if (empty($_POST)) { ?>
<form method="POST">
Username:<br />
<input type="text" name="username" /><br />
Password:<br />
<input type="text" name="password" /><br />

<img src="securimage_show.php"><br />
<input type="text" name="code" /><br />

<input type="submit" value="Submit Form" />
</form>

<?php
} else { //form is posted
  include("securimage.php");
  $img = new securimage();
  $valid = $img->check($_POST['code']);

  if($valid == TRUE) {
    echo "<center>Thanks, you entered the correct code.</center>";
  } else {
    echo "<center>Sorry, the code you entered was invalid.  <a href=\"javascript:history.go(-1)\">Go back</a> to try again.</center>";
  }
}

?>

</body>
</html>

