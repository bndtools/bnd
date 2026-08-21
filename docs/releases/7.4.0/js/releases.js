$().ready(function(){
	$.getJSON( "/releases/7.4.0/index.json", function( data ) {
		data.forEach(release => {
			$(".releases .dropdown-content").append(
				$('<a href="' + release.url + '">' + release.name + '</a>')
			);
		});
	});
});
