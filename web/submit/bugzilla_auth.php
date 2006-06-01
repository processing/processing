<?PHP

	/*
	 * 		just a quick check agains the bugs-db
	 *		to get rid of the nasty network-links-spammer.
	 *
	 *		fjenett - 2006-05-31 - mail@florianjenett.de
	 *
	 */



function user_auth ( $name, $pass )
{
	
	// name is actualy the email given when the user 
	// registered in yabb or bugzilla
	//

	if (    empty( $name ) || gettype($name) != "string"
	     || empty( $pass ) || gettype($pass) != "string" ) return false;
	
	if ( !get_magic_quotes_gpc() )
	{
		// should not really matter since quotes are 
		// not allowed in names or passes anyway
		
		$name = addslashes( $name );
		$pass = addslashes( $pass );
	}
	

	// keeping these inside the function,
	// so they will be cleared once it returns

	$bugz_serv =    'localhost';
	$bugz_db =      'bugs';
	$bugz_user =    'root';
	$bugz_pass =    'cOmet86';
		
	// connect to mysql
	//
	
	$mysql = mysql_connect( $bugz_serv, $bugz_user, $bugz_pass );
	if (!$mysql)
		die( basename(__FILE__).': Unable to connect to >> '.$bugz_serv.
			 ' '.$bugz_user."\n".mysql_error() );

	// select database
	//
	
	mysql_select_db( $bugz_db );
	
	
	// fetch name and pass
	//
	
	$sql = 'SELECT cryptpassword '.
		   'FROM profiles '.
		   'WHERE login_name=\''.$name.'\'';
	
	$result = mysql_fetch_array( mysql_query($sql) );
	
	if ( !$result )
		die( basename(__FILE__).': Problem with query:.'."\n".
			 $sql."\n".mysql_error() );

	
	// ok, now check pass
	//
	
	return ( crypt( $pass, $result['cryptpassword'] ) == $result['cryptpassword'] );
}

// testing it ..
//

/*
	$email = '';
	$pass = '';
	echo ( user_auth( $email , $pass ) ) ? 'OK' : 'NO-OK!';
	
*/

?>