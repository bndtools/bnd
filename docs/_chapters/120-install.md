---
order: 120
title: How to install bnd
layout: default
---

bnd is not a single product, it is a library (bndlib) used in many different software build environments. It runs inside Maven, ant, gradle, Eclipse, sbt, and maybe one day in Intellij. To install bnd, you will have to install these tools. 

<div>
<ul class="property-index">

{% for tool in site.tools %}<li><a href="{{ tool.url | prepend: site.baseurl }}">{{tool.title}}</a> {{tool.summary}}</li>
{% endfor %}

</ul>
</div>

## Command Line
That said, there is also a command line version of bnd, providing an easy way to try out its many features. You can install bnd through [brew][1] on MacOS.

You can also run bnd command as executable jar, which can be downloaded from [JFrog][7]:

```bash
# Install bnd CLI
curl -Lk -o ~/biz.aQute.bnd.jar \
	https://bndtools.jfrog.io/artifactory/update-latest/biz/aQute/bnd/biz.aQute.bnd/7.2.1/biz.aQute.bnd-7.2.1.jar

# create alias for easy use via 'bnd'
alias bnd='java -jar ~/biz.aQute.bnd.jar'

# display bnd version to verify installation
bnd version

# Run commands
bnd <command>

# Example
bnd help
```


## Libraries

The binaries are available on JFrog:

- [Releases and Release Candidates (RC)](https://bndtools.jfrog.io/artifactory/update-latest/biz/aQute/bnd/biz.aQute.bnd/)
- [SNAPSHOT releases](https://bndtools.jfrog.io/bndtools/libs-snapshot/biz/aQute/bnd/biz.aQute.bnd/) (e.g. the latest from current master branch)


If you are a maven user, you can find many versions in central. The coordinates are:

		<dependency>
			<groupId>biz.aQute.bnd</groupId>
			<artifactId>biz.aQute.bndlib</artifactId>
			<version>7.1.0</version>
		</dependency>


## Source Code
bnd is maintained at [github][3]. If you want to change the code, just clone it and modify it. In general we accept pull requests, and often even highly appreciate them.

## Manual
The manual is also on [github][5]. If you see an improvement, do not hesitate to clone the repo and create a pull request. Improvements are bug corrections but we also like short articles about how to do do something with bnd.

## Communication Settings

If you're behind a firewall that requires proxies or you use repositories that require authentication see [-connection-settings][6].

[1]: https://formulae.brew.sh/formula/bnd
[3]: https://github.com/bndtools/bnd
[4]: https://bndtools.jfrog.io/bndtools/libs-snapshot
[5]: https://github.com/bndtools/bnd/tree/master/docs
[6]: /instructions/connection_settings.html
[7]: https://bndtools.jfrog.io/bndtools/libs-snapshot/biz/aQute/bnd/biz.aQute.bnd/
