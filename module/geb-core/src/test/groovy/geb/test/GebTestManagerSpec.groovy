/*
 * Copyright 2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package geb.test

import geb.Browser
import geb.Configuration
import geb.ConfigurationLoader
import org.openqa.selenium.remote.DesiredCapabilities
import spock.lang.AutoCleanup
import spock.lang.Specification

import static geb.test.RemoteWebDriverWithExpectations.DEFAULT_IGNORED_COMMANDS

class GebTestManagerSpec extends Specification {

    Configuration configuration = new ConfigurationLoader().conf

    @AutoCleanup("stop")
    CallbackAndWebDriverServer callbackAndWebDriverServer = new CallbackAndWebDriverServer(configuration)

    RemoteWebDriverWithExpectations driver

    @Delegate
    GebTestManager gebTestManager

    def setup() {
        callbackAndWebDriverServer.start()

        driver = new RemoteWebDriverWithExpectations(
            callbackAndWebDriverServer.webdriverUrl, DesiredCapabilities.htmlUnit(), DEFAULT_IGNORED_COMMANDS - 'quit'
        )
        configuration.driver = driver
        gebTestManager = new GebTestManagerBuilder()
            .withBrowserCreator {
                new Browser(configuration)
            }
            .build()

        beforeTestClass(getClass())
        beforeTest(getClass(), specificationContext.currentIteration.name)
        browser
    }

    def cleanup() {
        driver.checkAndResetExpectations()
        afterTestClass()
    }

    def "by default driver not is quit when browser is being reset"() {
        when:
        afterTest()
        driver.checkAndResetExpectations()

        then:
        notThrown(RemoteWebDriverWithExpectations.UnexpectedCommandException)

        cleanup:
        driver.quit()
        driver.clearRecordedCommands()
    }

    def "driver is quit when browser is being reset when configured to do so"() {
        given:
        configuration.quitDriverOnBrowserReset = true

        when:
        afterTest()

        then:
        driver.quitExecuted()
    }
}
