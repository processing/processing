USE mobile;

CREATE TABLE links (
	id		int 		auto_increment primary key,
	name		varchar(255),
	title		varchar(255),
	url		varchar(255),
	submitted	bigint
);

