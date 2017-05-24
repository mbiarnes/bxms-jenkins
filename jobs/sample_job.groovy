job('sample-job') {

    description('This is sample job.')

    label('master')

    wrappers {
        preBuildCleanup()
    }

    steps {
        shell('echo "Hello!"')
    }
}