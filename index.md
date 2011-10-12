---
title: Home
layout: index
version: 1.0
dev-version: 1.1-SNAPSHOT
---

## Introduction

Betamax is a record/playback proxy for testing JVM applications that access external HTTP resources such as [web services][webservices] and [REST][rest] APIs. The project was inspired by the [VCR][vcr] library for Ruby.

Testing code that accesses HTTP services can be awkward. 3rd party downtime, network availability and resource constraints (such as the Twitter API's [rate limit][twitterratelimit]) can affect the reliability of your tests. You can always write custom _stub_ web server code and configure the application to connect to a different URI when running tests but this requires some up front time and may mean reinventing the wheel the next time a similar situation is encountered.

Betamax aims to solve these problems by intercepting HTTP connections initiated by your application and returning _recorded_ responses.

The first time a test annotated with `@Betamax` is run any HTTP traffic is recorded to a _tape_ and subsequent runs will play back the recorded HTTP response from the tape without actually connecting to the external server.

Betamax works with JUnit and [Spock][spock]. Although it is written in [Groovy][groovy] Betamax can be used to test applications written in any JVM language so long as HTTP connections are made in a way that respects Java's `http.proxyHost` and `http.proxyPort` system properties.

Tapes are stored to disk as [YAML][yaml] files and can be modified (or even created) by hand and committed to your project's source control repository so they can be shared by other members of your team and used by your CI server. Different tests can use different tapes to simulate various response conditions. Each tape can hold multiple request/response interactions. An example tape file can be found [here][tapeexample].

## Versions

The current stable version of Betamax is _{{ page.version }}_.

The current development version of Betamax is _{{ page.dev-version}}_.

## Installation

Stable versions of Betamax are available from the Maven central repository. Stable and development versions are available from the [Sonatype OSS Maven repository][sonatype]. To install with your favourite build system see below:

### Gradle

To use Betamax in a project using [Gradle][gradle] add the following to your `build.gradle` file:

	dependencies {
	    ...
	    testCompile "com.github.robfletcher:betamax:{{ page.version }}"
	    ...
	}


### Grails

To use Betamax in a [Grails][grails] app add the following to your `grails-app/conf/BuildConfig.groovy` file:

	repositories {
	    ...
	    mavenCentral()
	    ...
	}
	dependencies {
	    ...
	    test "com.github.robfletcher:betamax:{{ page.version }}"
	    ...
	}

### Maven

To use Betamax in a project using [Maven][maven] add the following to your `pom.xml` file:

	<dependencies>
	  ...
	  <dependency>
	    <scope>test</scope>
	    <groupId>com.github.robfletcher</groupId>
	    <artifactId>betamax</artifactId>
	    <version>{{ page.version }}</version>
	  </dependency>
	  ...
	</dependencies>

## Usage

To use Betamax you just need to annotate your JUnit test or [Spock][spock] specifications with `@Betamax(tape="tape_name")` and include a `betamax.Recorder` Rule.

### JUnit

	import betamax.Betamax;
	import betamax.Recorder;
	import org.junit.*;

	public class MyTest {

	    @Rule public Recorder recorder = new Recorder();

	    @Betamax(tape="my tape")
	    @Test
	    public void testMethodThatAccessesExternalWebService() {

	    }
	}

### Spock

	import betamax.Betamax
	import betamax.Recorder
	import org.junit.*
	import spock.lang.*

	class MySpec extends Specification {

	    @Rule Recorder recorder = new Recorder()

	    @Betamax(tape="my tape")
	    def "feature that accesses external web service"() {

	    }
	}

### Recording and playback

Betamax will record to the current tape when it intercepts any HTTP request that does not match anything that is already on the tape. If a matching recorded interaction _is_ found then the proxy does not forward the request to the target URI but instead returns the previously recorded response to the client.

### Matching requests

By default recorded interactions are matched based on the _method_ and _URI_ of the request. For most scenarios this is adequate. However, you can modify the matching behaviour by specifying a _match_ argument on the `@Betamax` annotation. Any combination of instances of the `betamax.MatchRule` enum can be used. If multiple rules are used then only a recorded interaction that matches all of them will be played back. `MatchRule` options are:

`method`
: the request method, _GET_, _POST_, etc.

`uri`
: the full URI of the request target. This includes any query string.

`body`
: the request body. This can be useful for testing connections to RESTful services that accept _POST_ data.

`host`
: the host of the target URI. For example the host of `http://search.twitter.com/search.json` is `search.twitter.com`.

`path`
: the path of the target URI. For example the host of `http://search.twitter.com/search.json` is `/search.json`.

`port`
: the port of the target URI.

`query`
: the query string of the target URI.

`fragment`
: the fragment of the target URI. i.e. anything after a `#`.

`headers`
: the request headers. If this rule is used then _all_ headers on the intercepted request must match those on the previously recorded request.

### Tape modes

Betamax supports three different read/write modes for tapes. The tape mode is set by adding a `mode` argument to the `@Betamax` annotation.

`READ_WRITE`
: This is the default mode. If the proxy intercepts a request that matches a recording on the tape then the recorded response is played back. Otherwise the request is forwarded to the target URI and the response recorded.

`READ_ONLY`
: The proxy will play back responses from tape but if it intercepts an unknown request it will not forward it to the target URI or record anything, instead it responds with a `403: Forbidden` status code.

`WRITE_ONLY`
: The proxy will always forward the request to the target URI and record the response regardless of whether or not a matching request is already on the tape. Any existing recorded interactions will be overwritten.

### Ignoring certain hosts

Sometimes you may need to have Betamax ignore traffic to certain hosts. A typical example would be if you are using Betamax when end-to-end testing a web application using something like _[HtmlUnit][htmlunit]_ - you would not want Betamax to intercept connections to _localhost_ as that would mean traffic between _HtmlUnit_ and your app was recorded and played back!

In such a case you can simply configure the `ignoreHosts` property of the `betamax.Recorder` object. The property accepts a list of hostnames or IP addresses. These can include wildcards at the start or end, for example `"*.mydomain.com"`.

If you need to ignore connections to _localhost_ you can simply set the `ignoreLocalhost` property to `true`.

## Compatibility

### Apache HttpClient 4.x

By default [Apache _HttpClient_][httpclient] takes no notice of Java's HTTP proxy settings. The Betamax proxy can only intercept traffic from HttpClient if the client instance is set up to use a [`ProxySelectorRoutePlanner`][proxyselector]. When Betamax is not active this will mean HttpClient traffic will be routed via the default proxy configured in Java (if any).

In a dependency injection context such as a [Grails][grails] app you can just inject a proxy-configured _HttpClient_ instance into your class-under-test.

#### Configuring HttpClient 4.x

	DefaultHttpClient client = new DefaultHttpClient();
	HttpRoutePlanner routePlanner = new ProxySelectorRoutePlanner(
	    client.getConnectionManager().getSchemeRegistry(),
	    ProxySelector.getDefault()
	);
	client.setRoutePlanner(routePlanner);

### Groovy HTTPBuilder

[Groovy _HTTPBuilder_][httpbuilder] and its [_RESTClient_][restclient] variant are wrappers around _HttpClient_ so the same proxy configuration needs to be applied.

#### Configuring HTTPBuilder

	def http = new HTTPBuilder("http://groovy.codehaus.org")
	http.client.routePlanner = new ProxySelectorRoutePlanner(
	    http.client.connectionManager.schemeRegistry,
	    ProxySelector.default
	)

_HTTPBuilder_ also includes a [_HttpURLClient_][httpurlclient] class which needs no special configuration as it uses a `java.net.URLConnection` rather than _HttpClient_.

### Apache HttpClient 3.x

_HttpClient_ 3.x does not take any notice of Java's HTTP proxy settings and does not have the `ProxySelectorRoutePlanner` facility that _HttpClient_ 4.x does. This means you must set the host and port of the Betamax proxy on the _HttpClient_ instance explicitly.

#### Configuring HttpClient 3.x

	HttpClient client = new HttpClient();
	ProxyHost proxy = new ProxyHost("localhost", 5555);
	client.getHostConfiguration().setProxyHost(proxy);

## Configuration

The `Recorder` class has some configuration properties that you can override:

`tapeRoot`
: the base directory where tape files are stored. Defaults to `src/test/resources/betamax/tapes`.

`proxyPort`
: the port the Betamax proxy listens on. Defaults to `5555`.

`proxyTimeout`
: the number of milliseconds before the proxy will give up on a connection to the target server. A value of zero means the proxy will wait indefinitely. Defaults to `5000`.

`defaultMode`
: the default _TapeMode_ applied to an inserted tape when the _mode_ argument is not present on the <code>@Betamax</code> annotation.

`ignoreHosts`
: a list of hosts that will be ignored by the Betamax proxy. Any requests made to these hosts will proceed normally.

`ignoreLocalhost`
: if set to `true` the Betamax proxy will ignore connections to local addresses. This is equivalent to setting `ignoreHosts` to `["localhost", "127.0.0.1", InetAddress.localHost.hostName, InetAddress.localHost.hostAddress]`.

If you have a file called `BetamaxConfig.groovy` or `betamax.properties` somewhere in your classpath it will be picked up by the `Recorder` class.

### Example _BetamaxConfig.groovy_ script

	betamax {
	    tapeRoot = new File("test/fixtures/tapes")
	    proxyPort = 1337
	    proxyTimeout = 30000
	    defaultMode = TapeMode.READ_ONLY
		ignoreHosts = ["localhost", "127.0.0.1"]
		ignoreLocalhost = true
	}

### Example _betamax.properties_ file

	betamax.tapeRoot=test/fixtures/tapes
	betamax.proxyPort=1337
    betamax.proxyTimeout=30000
	betamax.defaultMode=READ_ONLY
	betamax.ignoreHosts=localhost,127.0.0.1
	betamax.ignoreLocalhost=true

## Caveats

### Security

Betamax is a testing tool and not a spec-compliant HTTP proxy. It ignores _any_ and _all_ headers that would normally be used to prevent a proxy caching or storing HTTP traffic. You should ensure that sensitive information such as authentication credentials is removed from recorded tapes before committing them to your app's source control repository.

## About

### License

[Apache Software Licence, Version 2.0][licence]

### Authors

Rob Fletcher [github][github] [twitter][twitter] [ad-hockery][adhockery]

### Issues

Please raise issues on Betamax's [GitHub issue tracker][issues]. Forks and pull requests are more than welcome.

### Download

You can download this project in either [zip](http://github.com/robfletcher/betamax/zipball/master) or [tar](http://github.com/robfletcher/betamax/tarball/master) formats.

You can also clone the project with [Git][git] by running:

	$ git clone git://github.com/robfletcher/betamax

### Dependencies

Betamax depends on the following libraries (you will need them available on your test classpath in order to use Betamax):

* [Groovy 1.7+][groovy]
* [Apache HttpClient][httpclient]
* [Jetty 7][jetty]
* [SnakeYAML][snakeyaml]
* [JUnit 4][junit]
* [Apache log4j][log4j]

If your project gets dependencies from a [Maven][maven] repository these dependencies will be automatically included for you.

### Acknowledgements

Betamax is inspired by the [VCR][vcr] library for Ruby written by Myron Marston. Porting VCR to Groovy was suggested to me by [Jim Newbery][jim].

The documentation is built with [Jekyll][jekyll], [Skeleton][skeleton], [LESS][less], [Modernizr][modernizr], [jQuery][jquery] & [Google Code Prettify][prettify]. The site header font is [Play][playfont] by Jonas Hecksher.

## Examples

Betamax's GitHub repository includes [an example Grails application][grailsexample].

[adhockery]:http://adhockery.blogspot.com/ (Ad-Hockery)
[git]:http://git-scm.com
[github]:http://github.com/robfletcher (Rob Fletcher on GitHub)
[gradle]:http://www.gradle.org/
[grails]:http://grails.org/
[grailsexample]:https://github.com/robfletcher/betamax/tree/master/examples/grails-betamax
[groovy]:http://groovy.codehaus.org/
[htmlunit]:http://htmlunit.sourceforge.net/
[httpbuilder]:http://groovy.codehaus.org/modules/http-builder/
[httpclient]:http://hc.apache.org/httpcomponents-client-ga/httpclient/index.html
[httpurlclient]:http://groovy.codehaus.org/modules/http-builder/doc/httpurlclient.html
[issues]:https://github.com/robfletcher/betamax/issues
[jekyll]:http://jekyllrb.com/
[jetty]:http://www.eclipse.org/jetty/
[jquery]:http://jquery.com/
[jim]:http://tinnedfruit.com/
[junit]:http://www.junit.org/
[less]:http://lesscss.org/
[licence]:http://www.apache.org/licenses/LICENSE-2.0.html
[log4j]:http://logging.apache.org/log4j/1.2/
[maven]:http://maven.apache.org/
[modernizr]:http://www.modernizr.com/
[playfont]:http://www.fontsquirrel.com/fonts/play
[prettify]:http://code.google.com/p/google-code-prettify/
[proxyselector]:http://hc.apache.org/httpcomponents-client-ga/httpclient/apidocs/org/apache/http/impl/conn/ProxySelectorRoutePlanner.html
[rest]:http://en.wikipedia.org/wiki/Representational_state_transfer
[restclient]:http://groovy.codehaus.org/modules/http-builder/doc/rest.html
[skeleton]:http://www.getskeleton.com/
[snakeyaml]:http://www.snakeyaml.org/
[sonatype]:https://oss.sonatype.org/content/repositories/snapshots/
[spock]:http://spockframework.org/
[tapeexample]:https://github.com/robfletcher/betamax/blob/master/src/test/resources/betamax/tapes/smoke_spec.yaml
[twitter]:http://twitter.com/rfletcherEW (@rfletcherEW on Twitter)
[twitterratelimit]:https://dev.twitter.com/docs/rate-limiting
[webservices]:http://en.wikipedia.org/wiki/Web_service
[vcr]:http://relishapp.com/myronmarston/vcr
[yaml]:http://yaml.org/