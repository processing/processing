USE mobile;

CREATE TABLE IF NOT EXISTS curated (
	id		int 		auto_increment primary key,
	name		varchar(255),       
	title		varchar(255),
	url		varchar(255),
	imgurl		varchar(255),
	mobileurl	varchar(255),
	mobileimgurl	varchar(255),
	jadurl		varchar(255),	
	description	text,
	submitted	bigint
);

CREATE TABLE IF NOT EXISTS links (
	id		int 		auto_increment primary key,
	name		varchar(255),
	title		varchar(255),
	url		varchar(255),
	submitted	bigint
);

CREATE TABLE IF NOT EXISTS downloads (
	id		int		auto_increment primary key,
	name		varchar(255),
	useragent	varchar(255),
	stamp		timestamp 	not null
);

CREATE TABLE IF NOT EXISTS profile_summary (
	id		int		auto_increment primary key,
	downloadId	int,
	useragent	varchar(255),
	timezones	text,
	stamp		timestamp	not null
);

CREATE TABLE IF NOT EXISTS profile_display (
	id		int		primary key,
	width		int,
	height		int,
	colors		int,
	fullWidth	int,
	fullHeight	int,
	alpha		int
);

CREATE TABLE IF NOT EXISTS profile_libraries (
	id 		int		primary key,
	bluetooth	bool,
	image2		bool,
	messaging	bool,
	phone		bool,
	sound		bool,
	videoplayback	bool,
	videosnapshot	bool,
	xml		bool
);

CREATE TABLE IF NOT EXISTS profile_microedition (
	id		int		primary key,
	configuration	varchar(255),
	profiles	varchar(255),
	locale		varchar(255),
	encoding	varchar(255),
	platform	varchar(255),
	hostname	varchar(255),
	commports	varchar(255)
);

CREATE TABLE IF NOT EXISTS profile_messaging (
	id		int		primary key,
	smsc		varchar(255),
	mmsc		varchar(255)
);

CREATE TABLE IF NOT EXISTS profile_mmapi (
	id		int		primary key,
	version		varchar(255),
	mixing		bool,
	audiocapture	bool,
	videocapture	bool,
	recording	bool,
	audioencodings	text,
	videoencodings	text,
	snapencodings	text,
	streamable	text,
	contenttypes	text
);
