$().ready(function(){
	$.getJSON( "/releases/index.json", function( data ) {
		data.forEach(release => {
			$(".releases .dropdown-content").append(
				$('<a href="' + release.url + '">' + release.name + '</a>')
			);
		});
	});
});
