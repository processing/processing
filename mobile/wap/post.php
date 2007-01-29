<?php
require_once '../mobile/db.inc.php';

/*
//// debugging output, showing each submitted key
foreach ($_POST as $k => $v) {
    error_log("post key='". $k ."'");
}
*/

//// check all parameters, and explicity set missing ones to NULL
$params = array('timezones', 'width', 'height', 'colors', 'fullWidth', 'fullHeight', 'alpha', 'bluetooth', 'image2', 'messaging', 'phone', 'sound', 'video_(playback)', 'video_(snapshot)', 'xml', 'microedition_configuration', 'microedition_profiles', 'microedition_locale', 'microedition_encoding', 'microedition_platform', 'microedition_hostname', 'microedition_commports', 'wireless_messaging_sms_smsc', 'wireless_messaging_mms_mmsc', 'microedition_media_version', 'supports_mixing', 'supports_audio_capture', 'supports_video_capture', 'supports_recording', 'audio_encodings', 'video_encodings', 'video_snapshot_encodings', 'streamable_contents', 'contenttypes');
foreach ($params as $p) {
    if (is_null($_POST[$p])) {
        $_POST[$p] = "NULL";
    }
    //// also check for booleans and convert to 0/1
    if (strcmp(trim($_POST[$p]), "true") == 0) {
        $_POST[$p] = 1;
    } else if (strcmp(trim($_POST[$p]), "false") == 0) {
        $_POST[$p] = 0;
    }
}

$link = db_connect();

//// delete any previous submission
if ($_POST['id'] != 0) {
    $query = "SELECT id FROM profile_summary WHERE downloadId=". $_POST['id'];
    $result = mysql_query($query);
    $numrows = mysql_num_rows($result);
    if ($numrows > 0) {
        $ids = array();
        for ($i = 0; $i < $numrows; $i++) {
	  $ids[] = mysql_result($result, $i);
	}
	$ids = implode(", ", $ids);
	$query = "DELETE FROM profile_display WHERE id IN (". $ids .")";
	mysql_query($query);
	$query = "DELETE FROM profile_libraries WHERE id IN (". $ids .")";
	mysql_query($query);
	$query = "DELETE FROM profile_microedition WHERE id IN (". $ids .")";
	mysql_query($query);
	$query = "DELETE FROM profile_messaging WHERE id IN (". $ids .")";
	mysql_query($query);
	$query = "DELETE FROM profile_mmapi WHERE id IN (". $ids .")";
	mysql_query($query);
	$query = "DELETE FROM profile_summary WHERE downloadId=". $_POST['id'];
	mysql_query($query);
    }
    /*
old mysql 3 on processing.org doesn't support multi-table delete syntax...

    $query = "DELETE FROM profile_summary, profile_display, profile_libraries, profile_microedition, profile_messaging, profile_mmapi USING profile_summary, profile_display, profile_libraries, profile_microedition, profile_messaging, profile_mmapi WHERE profile_summary.downloadId=". $_POST['id'] ." AND profile_display.id=profile_summary.id AND profile_libraries.id=profile_summary.id AND profile_microedition.id=profile_summary.id AND profile_messaging.id=profile_summary.id AND profile_mmapi.id=profile_summary.id";
    mysql_query($query);
    */
}

//// insert new submission into db
$query = "INSERT INTO profile_summary (downloadId, useragent, timezones) VALUES (". $_POST['id'] .", '". $_POST['useragent'] ."', '". $_POST['timezones'] ."')";
mysql_query($query);
$id = mysql_insert_id();

$query = "INSERT INTO profile_display (id, width, height, colors, fullWidth, fullHeight, alpha) VALUES (". $id .", ". $_POST['width'] .", ". $_POST['height'] .", ". $_POST['colors'] .", ". $_POST['fullWidth'] .", ". $_POST['fullHeight'] .", ". $_POST['alpha'] .")";
mysql_query($query);

$query = "INSERT INTO profile_libraries (id, bluetooth, image2, messaging, phone, sound, videoplayback, videosnapshot, xml) VALUES (". $id .", ". $_POST['bluetooth'] .", ". $_POST['image2'] .", ". $_POST['messaging'] .", ". $_POST['phone'] .", ". $_POST['sound'] .", ". $_POST['video_(playback)'] .", ". $_POST['video_(snapshot)'] .", ". $_POST['xml'] .")";
mysql_query($query);

$query = "INSERT INTO profile_messaging (id, smsc, mmsc) VALUES (". $id .", '". $_POST['wireless_messaging_sms_smsc'] ."', '". $_POST['wireless_messaging_mms_mmsc'] ."')";
mysql_query($query);

$query = "INSERT INTO profile_microedition (id, configuration, profiles, locale, encoding, platform, hostname, commports) VALUES (". $id .", '". $_POST['microedition_configuration'] ."', '". $_POST['microedition_profiles'] ."', '". $_POST['microedition_locale'] ."', '". $_POST['microedition_encoding'] ."', '". $_POST['microedition_platform'] ."', '". $_POST['microedition_hostname'] ."', '". $_POST['microedition_commports'] ."')";
mysql_query($query);

$query = "INSERT INTO profile_mmapi (id, version, mixing, audiocapture, videocapture, recording, audioencodings, videoencodings, snapencodings, streamable, contenttypes) VALUES (". $id .", '". $_POST['microedition_media_version'] ."', ". $_POST['supports_mixing'] .", ". $_POST['supports_audio_capture'] .", ". $_POST['supports_video_capture'] .", ". $_POST['supports_recording'] .", '". $_POST['audio_encodings'] ."', '". $_POST['video_encodings'] ."', '". $_POST['video_snapshot_encodings'] ."', '". $_POST['streamable_contents'] ."', '". $_POST['contenttypes'] ."')";
mysql_query($query);

?>Success
