%map = %(default => %(
		sitename => 'default', 
		rootdirectory => './sites/',
		siteport => '8080',
	));

%__SITES__ = %map;
printf(%__SITES__);
