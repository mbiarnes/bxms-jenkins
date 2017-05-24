job('sample-init_release') {

    description('This is release initialization job.')

    preBuildSteps {
        enviornamentVariables {
            propertiesFile('${HOME}/brms-64-jenkins-ci.properties')
            keepBuildVariables(true)
        }
    }


    label('pvt-static')

    wrappers {
        preBuildCleanup()
    }

    scm {
        git {
            url('https://code.engineering.redhat.com/gerrit/integration-platform-config.git/')
            branch('master')
        }
    }

    publishers {
        createJiraIssue {
            projectKey('IPBRPMS')
            testDescription('${product_name} ${product_version} ${release_milestone} Release')
            assignee('mw-prod-ci')
        }
    }

    steps {
        shell('echo "Hello!"')

        progressJiraIssues {
            jqlSearch('Summary ~ "${product_name} ${product_version} ${release_milestone} Release"')
            workflowActionName('To Do')
            comment('Comment')
        }
    }
}