---
order: 120
title: How to install bnd
layout: default
---

bnd is not a single product, it is a library (bndlib) used in many different software build environments. It runs inside Maven, ant, gradle, Eclipse, sbt, and maybe one day in Intellij. To install bnd, you will have to install these tools. 

<div>
<ul class="property-index">

{% for tool in site.tools %}<li><a href="{{ tool.url | prepend: site.github.url }}">{{tool.title}}</a> {{tool.summary}}</li>
{% endfor %}

</ul>
</div>

## Command Line
*REPOSITORY www.jpm4j.org IS DEFUNCT. THIS TOPIC SHOULD BE REMOVED OR UPDATED.*
That said, there is also a command line version of bnd, providing an easy way to try out its many features. You can install bnd through [jpm][1]. You first have to install jpm, fortunately, this is well documented at [jpm install][1]. Installing jpm has the other advantage that it provides some useful commands to work with a repository that has all of maven central. 

With jpm installed, you can now install the latest version of bnd as follows:

	$ sudo jpm install bnd@*
	$ bnd version
	2.4.0.201406271227
	$

## Libraries
The binaries are available on [Cloudbees][4]. The latest version can be found at:

	https://bndtools.ci.cloudbees.com/job/bnd.master/lastSuccessfulBuild/artifact/

However, Cloudbees currently also contains the previously released libraries.
	
If you are a maven user, you can find many version in central. The coordinates are:

		<dependency>
			<groupId>biz.aQute.bnd</groupId>
			<artifactId>biz.aQute.bndlib</artifactId>
			<version>....</version>
		</dependency>


## Source Code
bnd is maintained at [github][3]. If you want to change the code, just clone it and modify it. In general we accept pull requests, and often even highly appreciate them.

## Manual
The manual is also on [github][5]. If you see an improvement, do not hesitate to clone the repo and create a pull request. Improvements are bug corrections but we also like short articles about how to do do something with bnd.

## Communication Settings

If you're behind a firewall that requires proxies or you use repositories that require authentication see [-connection-settings].

[1]: http://www.jpm4j.org
[2]: http://jpm4j.org/#!/md/install
[3]: https://github.com/bndtools/bnd
[4]: https://bndtools.ci.cloudbees.com/job/bnd.master/lastSuccessfulBuild/artifact/
[5]: https://github.com/bndtools/bnd.manual
[6]: /instructions/conditionalpackage.html
[-connection-settings]: /instructions/connection-settings
