// Creates or updates a free style job.
def jobDefinition = job("${PRODUCT_NAME}-release-pipeline/${PRODUCT_NAME}-test") {

    // Label which specifies which nodes this job can run on.
    label("master")

    // Adds environment variables to the build.
    environmentVariables {
        groovy('''
def date = new Date().format( 'yyyyMMdd-hhMMss' )
def version = "7.1.0."
return [tag_name: version + date]
        ''')
    }
    // Adds build steps to the jobs.
    steps {

        // Runs a shell script (defaults to sh, but this is configurable) for building the project.
        shell('echo $tag_name')
    }
}
