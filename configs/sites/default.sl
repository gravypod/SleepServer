%map = %(default => %(
		sitename => 'default', 
		serveraddress => 'home.gravypod.com',
		rootdirectory => './sites/',
		siteport => '8080',
	));

%__SITES__ = %map;
printf(%__SITES__);
