<?php
/*
 * Tera_WURFL - PHP MySQL driven WURFL
 * 
 * Tera-WURFL was written by Steve Kamerman, Tera Technologies and is based on the
 * WURFL PHP Tools from http://wurfl.sourceforge.net/.  This version uses a MySQL database
 * to store the entire WURFL file to provide extreme performance increases.
 * 
 * @package tera_wurfl
 * @author Steve Kamerman, Tera Technologies (kamermans AT teratechnologies DOT net)
 * @version Beta 1.4.4 $Date: 2007/01/04 04:28:39 $
 * @license http://www.mozilla.org/MPL/ MPL Vesion 1.1
 * $Id: tera_wurfl_config.php,v 1.1.4.5.2.17 2007/01/04 04:28:39 kamermans Exp $
 * $RCSfile: tera_wurfl_config.php,v $
 * 
 * Based On: WURFL PHP Tools by Andrea Trasatti ( atrasatti AT users DOT sourceforge DOT net )
 *
 */
$branch = "Beta";
$version = "1.4.4";
/*
 * This is the configuration file for Tera-WURFL PHP class.
 * All configurable options are in this file.
 *
 * Defines used by this library:
 * 
 * -- Database options --
 * DB_HOST				string,	database server hostname or IP
 * DB_USER				string,	database username (needs SELECT,INSERT,DELETE,DROP,CREATE)
 * DB_PASS				string, database password
 * DB_SCHEMA			string, database schema (database name)
 * DB_TYPE				string, database table type (MyISAM, InnoDB, HEAP, etc...);
 * DB_DEVICE_TABLE		string, database table name for the WURFL
 * DB_PATCH_TABLE		string, database table name for the patch
 * DB_HYBRID_TABLE		string, database table name for the Hybrid of the WURFL and the patch
 * DB_MULTI_INSERT		boolean,use multiple inserts to speed DB updating
 * DB_MAX_INSERTS		integer,number of inserts per query
 * DB_EMPTY_METHOD		string, either DROP_CREATE or EMPTY; method for emptying tables.
 * DB_TEMP_EXT			string, extension that will be used for temporary tables like "mytablename_TEMP"
 * 
 * -- General options --
 * WURFL_DL_URL			string, full URL to the current WURFL
 * WURFL_CVS_URL		string, full URL to development (CVS) WURFL
 * WURFL_CONFIG			boolean,lets other file know the config is loaded
 * DATADIR				string,	where all data is stored (wurfl.xml, temp files, logs)
 * IMAGE_CHECKING		boolean,checks the IMAGE_DIR for an image that matches the device
 * IMAGE_DIR			string, relative path to the device images with trailing slash
 * WURFL_FILE			string, path and filename of wurfl.xml
 * WURFL_PARSER_FILE	string, path and filename of wurfl_parser.php
 * WURFL_CLASS_FILE		string, path and filename of wurfl_class.php
 * WURFL_PATCH_ENABLE	boolean,enables or disables the patch
 * WURFL_PATCH_FILE		string, optional patch file for WURFL
 * WURFL_LOG_FILE 		string, defines full path and filename for logging
 * LOG_LEVEL			integer, desired logging level. Use the same constants as for PHP logging
 *
 */

/**
 * Database hostname or IP Address
 */
define("DB_HOST","localhost");
/**
 * Database username
 */
define("DB_USER","root");
/**
 * Database password
 */
define("DB_PASS","");
/**
 * Database schema (the database name itself)
 */
define("DB_SCHEMA","mobile");
/**
 * Database type - you probably want to use either "InnoDB" or "MyISAM".  
 * In testing I have found MyISAM to be about 10% faster than InnoDB.  
 * Note: You can use any database type that your server supports
 */
define("DB_TYPE","MyISAM");
/**
 * The table you want to use to store the WURFL devices
 */
define("DB_DEVICE_TABLE","tera_wurfl_devices");
/**
 * The table you want to use to store the WURFL patch file devices
 */
define("DB_PATCH_TABLE","tera_wurfl_patch");
/**
 * The table you want to use to store the hybrid data (the merged data 
 * between the WURFL and the patch.  This is only used when WURFL_PATCH_ENABLE
 * is true and a patch is loaded.
 */
define("DB_HYBRID_TABLE","tera_wurfl_hybrid");
/**
 * The extension used for temporary tables.  These tables are used to allow the
 * class to rollback any changes that fail sanity checks
 */
define("DB_TEMP_EXT","_TEMP");
/**
 * Insert more than one record per query.  This will SIGNIFICANTLY increase the
 * speed of database updates.  See DB_MAX_INSERTS.
 */
define("DB_MULTI_INSERT",true);
/**
 * Number of inserts to use per query - too many will exceed the 'max_allowed_packet'
 * directive in the MySQL configuration file. Using a setting above 1000 wil probably
 * have a negative impact on performance because the queries will be too large.  If 
 * your database is not on the same server as your class file you may want to try
 * increasing this number to speed things up a bit since it would result in less queries.
 */
define("DB_MAX_INSERTS",500);
/**
 * Specify the method for emptying tables.  Either "DROP_CREATE" or "EMPTY".  As of 
 * version 1.3.0, "DROP_CREATE is HIGHLY recommended for stability, and EMPTY may result
 * in a MySQL warning or error while updating the database.
 */
define("DB_EMPTY_METHOD", "DROP_CREATE");
/**
 * The URL to download the current WURFL file from.
 * This was updated on November 16, 2006 v1.4.2
 * TODO: use compressed version to speed up download!
 */
define("WURFL_DL_URL","http://wurfl.sourceforge.net/wurfl.xml");
/**
 * The URL to download the current WURFL file from.
 * This was updated on December 26, 2006 v1.4.3
 */
define("WURFL_CVS_URL","http://wurfl.cvs.sourceforge.net/%2Acheckout%2A/wurfl/xml/wurfl.xml");
/**
 * ALWAYS set this to true - it is how the class knows the config file has been loaded
 */
define("WURFL_CONFIG", true);
/**
 * Where all data is stored (wurfl.xml, cache file, logs, etc)
 */
define("DATADIR", dirname(__FILE__).'/data/'); // needs to end with a slash!
/**
 * Try to find an image for the device, accessible like $this->device_image
 */
define("IMAGE_CHECKING", true);
/**
 * Reletive path to images with trailing slash
 */
define("IMAGE_DIR","device_pix/");
/**
 * Enable or disable the WURFL patch. This setting takes effect immediately - 
 * no database or patch update is required.
 */
define("WURFL_PATCH_ENABLE", false);
/**
 * Path and filename of your custom patch file.  You can use DATADIR."somefile"
 * if your patch file is in the DATADIR directory
 */
define("WURFL_PATCH_FILE", DATADIR.'wurfl.patch.xml');
/**
 * Path and filename of wurfl_parser.php
 */
define("WURFL_PARSER_FILE", dirname(__FILE__).'/admin/tera_wurfl_parser.php');
/*
 * Path and filename of wurfl_class.php
 */
define("WURFL_CLASS_FILE", dirname(__FILE__).'/tera_wurfl.php');
/**
 * Path and name of the local wurfl.xml file
 */
define("WURFL_FILE", DATADIR."wurfl.xml");
/**
 * Path and name of the log file
 */
define("WURFL_LOG_FILE", DATADIR."wurfl.log");
/**
 * Log errors at or above this level of severity.
 * Suggested log level for normal use: LOG_ERR or LOG_WARNING;
 * all options: LOG_INFO, LOG_NOTICE, LOG_WARNING, LOG_ERR
 * This directive is NOT a string value - it is a PHP Constant
 * http://us2.php.net/manual/en/reserved.constants.php
 */
define("LOG_LEVEL", LOG_WARNING);

// Variables below this line are used for internal use only and should not be modified.
define("CLASS_DIRNAME", dirname(__FILE__));
//error_reporting(E_ALL);
?>