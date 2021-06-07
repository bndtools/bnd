$().ready(function(){
	$.getJSON( "{{ '/releases/index.json' | prepend: site.baseurl }}", function( data ) {
		data.forEach(release => {
			$(".releases .dropdown-content").append(
				$('<a href="' + release.url + '">' + release.name + '</a>')
			);
		});
	});
});
