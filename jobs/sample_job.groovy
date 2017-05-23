job('sample-job') {
    scm {
        git('https://github.com/kiegroup/drools')
    }
    triggers {
        scm('H/15 * * * *')
    }
    steps {
        maven('clean')
    }
}