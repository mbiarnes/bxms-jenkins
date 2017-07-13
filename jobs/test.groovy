// Creates or updates a free style job.
def jobDefinition = job("${PRODUCT_NAME}-release-pipeline/${PRODUCT_NAME}-test") {

    // Label which specifies which nodes this job can run on.
    label("master")

    // Adds environment variables to the build.
    environmentVariables {
        groovy('''
            kieDateFormat = "7.1.0." + now.format("yyyyMMdd-HHmmss")
            ufDateFormat = "1.1.0." + now.format("yyyyMMdd-HHmmss")
            dashDateFormat = "0.7.0." + now.format("yyyyMMdd-HHmmss")
            erraiDateFormat = "4.0.1." + now.format("yyyyMMdd-HHmmss")
            return [A: kieDateFormat, B: ufDateFormat, C: dashDateFormat, D: erraiDateFormat]
        ''')
    }
    // Adds build steps to the jobs.
    steps {

        // Runs a shell script (defaults to sh, but this is configurable) for building the project.
        shell('echo "$A and $B and $C and $D"')
    }
}
