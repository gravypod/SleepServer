%__MIMI__ = %(
	css => 'text/css',
	htm => 'text/html',
	html => 'text/html',
	xml => 'text/xml',
	java => 'text/x-java-source => text/java',
	txt => 'text/plain',
	asc => 'text/plain',
	gif => 'image/gif',
	jpg => 'image/jpeg',
	jpeg => 'image/jpeg',
	png => 'image/png',
	mp3 => 'audio/mpeg',
	m3u => 'audio/mpeg-url',
	mp4 => 'video/mp4',
	ogv => 'video/ogg',
	flv => 'video/x-flv',
	mov => 'video/quicktime',
	swf => 'application/x-shockwave-flash',
	js => 'application/javascript',
	pdf => 'application/pdf',
	doc => 'application/msword',
	ogg => 'application/x-ogg',
	zip => 'application/octet-stream',
	exe => 'application/octet-stream',
	class => 'application/octet-stream',
	sl => 'application/sl',
);
printf("Mimi types set");

@__INDEXFILES__ = @(
	'index.html',
	'index.htm',
	'index.sl'
);
printf("Index files set");

require("./sites/default.sl");

printf("Loading site configs");

printf("Sheveron 8 encoded");