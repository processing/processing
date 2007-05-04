<?php
$PAGE_TITLE = "Mobile Processing";

require 'header.inc.php';
?>
<div class="column2x" style="width: 300px; clear: left">
    <img src="exhibition/images/header.png"><br>
<br>
<br>
<a href="http://huah.net/snakeonaplane/" target="_new"><img src="exhibition/images/snake.png"></a><br>
<br>
<table width="200" border="0" cellspacing="0" cellpadding="0">
  <tr>
    <td>
<b><a href="http://huah.net/snakeonaplane/" target="_new">SNAKE on a plane!</a></b>
    </td>
  </tr>
  <tr height="5">
  </tr>
  <tr>
    <td>
<b>by Matthias Shapiro and Jason Alderman</b><br>
    </td>
  </tr>
</table><br>
<br />
<a href="http://todbot.com/blog/2006/09/12/roombactrl-drive-your-roomba-with-your-cell-phone/" target="_new"><img src="exhibition/images/roombactrl.png"></a><br>
<br>
<table width="200" border="0" cellspacing="0" cellpadding="0">
  <tr>
    <td>
<b><a href="http://todbot.com/blog/2006/09/12/roombactrl-drive-your-roomba-with-your-cell-phone/" target="_new">RoombaCtrl</a></b>
    </td>
  </tr>
  <tr height="5">
  </tr>
  <tr>
    <td>
<b>by todbot</b><br>
    </td>
  </tr>
</table><br>
<br>
<br>
<a href="learning/"><img src="images/examples.png"></a><br>
<br>
<br>
<table border="0" cellspacing="0" cellpadding="0">
  <tr>
    <td>
<a href="learning/example.php?name=yahoosonar"><img src="images/examples/yahoosonar.png"></a>
    </td>
    <td width="1"></td>
    <td>
<a href="learning/example.php?name=bluescan"><img src="images/examples/bluescan.png"></a>
    </td>
    <td width="1"></td>
    <td>
<a href="learning/example.php?name=photoslider"><img src="images/examples/photoslider.png"></a>
    </td>
  </tr>
  <tr>
    <td>
<a href="learning/example.php?name=tictactoe"><img src="images/examples/tictactoe.png"></a>
    </td>
    <td width="1"></td>
    <td>
    </td>
    <td width="1"></td>
    <td>
    </td>
  </tr>
</table>
</div>
<div class="column" style="width: 400px">
    <br>
    Mobile Processing is an open source programming environment for people who want to design and prototype software for mobile phones. It is based on and shares the same design goals as the open source <a href="http://processing.org/">Processing</a> project. Sketches programmed using Mobile Processing run on Java Powered mobile devices.<br>
    <br>
    <br>
    <br>
    <img src="images/updates.png"><br>
    <br>
    <br>
<?php
$fp = fopen("discourse/Boards/news.txt", 'r');
if ($fp) {
  $i = 0;
  while (!feof($fp) && ($i < 3)) {
    $line = fgets($fp);
    $tokens = explode("|", $line);
    $id = $tokens[0];

    $fp2 = fopen("discourse/Messages/". $id .".txt", 'r');
    $line = fgets($fp2);    
    $tokens = explode("|", $line);
    $line = null;
    fclose($fp2);

    $ts = strtotime($tokens[3]);
    echo "<i>". date("j M Y", $ts) ."</i><br />";
    echo $tokens[0] ."<br />";
    echo substr($tokens[8], 0, strpos($tokens[8], "<br>")) ."<br />";
    echo "<a href=\"discourse/YaBB.cgi?board=news;action=display;num=". $id ."\">Read more...</a><br /><br />";
    $tokens = null;
    $i++;
  }
  fclose($fp);
}
?>
    <br />
    <a href="discourse/YaBB.cgi?board=news">Previous Updates</a> \ <a href="updates.xml"><img src="images/feed.gif"> RSS</a><br />
    <br />
    <br />
    <img src="images/happenings.png"><br>
    <br>
    <br>
    <i>28 Apr 2007</i><br>
Mobile Processing Lecture at FLISOL2007 by Marlon J. Manrique.<br /><a href="http://installfest.info/FLISOL2007/Colombia/Manizales">More info</a><br />
    <br />
    <i>6 May 2007</i><br>
    Mobile Processing Workshop at Mobilized! NYC by Francis Li.<br /><a href="http://www.mobilizednyc.org/">More info</a><br>
    <br>
</div>
<?php
require 'footer.inc.php';
?>