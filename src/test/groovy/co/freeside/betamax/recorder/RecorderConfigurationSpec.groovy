package co.freeside.betamax.recorder

import co.freeside.betamax.Recorder
import spock.lang.*
import static co.freeside.betamax.Recorder.*
import static co.freeside.betamax.TapeMode.*

class RecorderConfigurationSpec extends Specification {

	@Shared String tmpdir = System.properties.'java.io.tmpdir'

	void 'recorder gets default configuration if not overridden and no properties file exists'() {
		given:
		def recorder = new Recorder()

		expect:
		recorder.tapeRoot == new File(DEFAULT_TAPE_ROOT)
		recorder.proxyPort == 5555
		recorder.defaultMode == READ_WRITE
		recorder.proxyTimeout == DEFAULT_PROXY_TIMEOUT
		recorder.ignoreHosts == []
		!recorder.ignoreLocalhost
		!recorder.sslSupport
		recorder.useProxy
	}

	void 'recorder configuration is overridden by map arguments'() {
		given:
		def recorder = new Recorder(
				tapeRoot: new File(tmpdir, 'tapes'),
				proxyPort: 1337,
				defaultMode: READ_ONLY,
				proxyTimeout: 30000,
				ignoreHosts: ['localhost'],
				ignoreLocalhost: true,
				sslSupport: true,
				useProxy: false
		)

		expect:
		recorder.tapeRoot == new File(tmpdir, 'tapes')
		recorder.proxyPort == 1337
		recorder.defaultMode == READ_ONLY
		recorder.proxyTimeout == 30000
		recorder.ignoreHosts == ['localhost']
		recorder.ignoreLocalhost
		recorder.sslSupport
		!recorder.useProxy
	}

	void 'recorder picks up configuration from properties'() {
		given:
		def properties = new Properties()
		properties.setProperty('betamax.tapeRoot', "$tmpdir/tapes")
		properties.setProperty('betamax.proxyPort', '1337')
		properties.setProperty('betamax.defaultMode', 'READ_ONLY')
		properties.setProperty('betamax.proxyTimeout', '30000')
		properties.setProperty('betamax.ignoreHosts', 'localhost,127.0.0.1')
		properties.setProperty('betamax.ignoreLocalhost', 'true')
		properties.setProperty('betamax.sslSupport', 'true')
		properties.setProperty('betamax.useProxy', 'false')

		and:
		def recorder = new Recorder(properties)

		expect:
		recorder.tapeRoot == new File(tmpdir, 'tapes')
		recorder.proxyPort == 1337
		recorder.defaultMode == READ_ONLY
		recorder.proxyTimeout == 30000
		recorder.ignoreHosts == ['localhost', '127.0.0.1']
		recorder.ignoreLocalhost
		recorder.sslSupport
		!recorder.useProxy
	}

	void 'recorder picks up configuration from properties file'() {
		given:
		def propertiesFile = new File(tmpdir, 'betamax.properties')
		def properties = new Properties()
		properties.setProperty('betamax.tapeRoot', "$tmpdir/tapes")
		properties.setProperty('betamax.proxyPort', '1337')
		properties.setProperty('betamax.defaultMode', 'READ_ONLY')
		properties.setProperty('betamax.proxyTimeout', '30000')
		properties.setProperty('betamax.ignoreHosts', 'localhost,127.0.0.1')
		properties.setProperty('betamax.ignoreLocalhost', 'true')
		properties.setProperty('betamax.sslSupport', 'true')
		properties.setProperty('betamax.useProxy', 'false')
		propertiesFile.withWriter { writer ->
			properties.store(writer, null)
		}

		and:
		Recorder.classLoader.addURL(new File(tmpdir).toURL())

		and:
		def recorder = new Recorder()

		expect:
		recorder.tapeRoot == new File(tmpdir, 'tapes')
		recorder.proxyPort == 1337
		recorder.defaultMode == READ_ONLY
		recorder.proxyTimeout == 30000
		recorder.ignoreHosts == ['localhost', '127.0.0.1']
		recorder.ignoreLocalhost
		recorder.sslSupport
		!recorder.useProxy

		cleanup:
		propertiesFile.delete()
	}

	void 'recorder picks up configuration from groovy config script'() {
		given:
		def configFile = new File(tmpdir, 'BetamaxConfig.groovy')
		configFile.withWriter { writer ->
			writer << '''\
				betamax {
					tapeRoot = new File(System.properties.'java.io.tmpdir', 'tapes')
					proxyPort = 1337
					defaultMode = co.freeside.betamax.TapeMode.READ_ONLY
					proxyTimeout = 30000
					ignoreHosts = ['localhost', '127.0.0.1']
					ignoreLocalhost = true
					sslSupport = true
					useProxy = false
				}
			'''
		}

		and:
		Recorder.classLoader.addURL(new File(tmpdir).toURL())

		and:
		def recorder = new Recorder()

		expect:
		recorder.tapeRoot == new File(tmpdir, 'tapes')
		recorder.proxyPort == 1337
		recorder.defaultMode == READ_ONLY
		recorder.proxyTimeout == 30000
		recorder.ignoreHosts == ['localhost', '127.0.0.1']
		recorder.ignoreLocalhost
		recorder.sslSupport
		!recorder.useProxy

		cleanup:
		configFile.delete()
	}

	void 'constructor arguments take precendence over a properties file'() {
		given:
		def propertiesFile = new File(tmpdir, 'betamax.properties')
		def properties = new Properties()
		properties.setProperty('betamax.tapeRoot', "$tmpdir/tapes")
		properties.setProperty('betamax.proxyPort', '1337')
		properties.setProperty('betamax.defaultMode', 'READ_ONLY')
		properties.setProperty('betamax.proxyTimeout', '30000')
		properties.setProperty('betamax.ignoreHosts', 'localhost,127.0.0.1')
		properties.setProperty('betamax.ignoreLocalhost', 'true')
		properties.setProperty('betamax.sslSupport', 'true')
		properties.setProperty('betamax.useProxy', 'false')
		propertiesFile.withWriter { writer ->
			properties.store(writer, null)
		}

		and:
		Recorder.classLoader.addURL(new File(tmpdir).toURL())

		and:
		def recorder = new Recorder(
				tapeRoot: new File('test/fixtures/tapes'),
				proxyPort: 1234,
				defaultMode: WRITE_ONLY,
				proxyTimeout: 10000,
				ignoreHosts: ['github.com'],
				ignoreLocalhost: false,
				sslSupport: false,
				useProxy: true
		)

		expect:
		recorder.tapeRoot == new File('test/fixtures/tapes')
		recorder.proxyPort == 1234
		recorder.defaultMode == WRITE_ONLY
		recorder.proxyTimeout == 10000
		recorder.ignoreHosts == ['github.com']
		!recorder.ignoreLocalhost
		!recorder.sslSupport
		recorder.useProxy

		cleanup:
		propertiesFile.delete()
	}

	@Issue('https://github.com/robfletcher/betamax/issues/56')
	void 'default tape mode is set correctly on tape'() {
		given:
		def recorder = new Recorder(defaultMode: READ_ONLY)

		when:
		recorder.insertTape('foo', [mode: DEFAULT])

		then:
		recorder.tape.isReadable()
		!recorder.tape.isWritable()
	}

}
