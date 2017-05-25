job("sample-init-release") {

    description("This is release initialization job. This job is responsible for preparation of brms-64-jenkins-ci.properties file.")

    label("pvt-static")

    wrappers {
        preBuildCleanup()
    }

    triggers {
        ciBuildTrigger {

            // The name of the Message Provider that was configured in the global settings.
            selector('CI_TYPE=\'brms-64-releaseci-brew-trigger\'')
            // JMS selector to choose messages that will fire the trigger.
            providerName("default")

        }
    }

    multiscm {
        git {
            remote {
                url("https://code.engineering.redhat.com/gerrit/integration-platform-config.git/")
            }
            branch("master")
        }

        git {
            remote {
                url("https://code.engineering.redhat.com/gerrit/integration-platform-tooling.git/")
            }
            branch("master")
        }
    }

    steps {
        shell('integration-platform-tooling/utility/init-releaseci-properties.sh integration-platform-config/brms-64.cfg')
    }
}