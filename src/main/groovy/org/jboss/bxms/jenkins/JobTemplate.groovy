package org.jboss.bxms.jenkins


class JobTemplate {
    static void addCommonConfiguration(job, CI_PROPERTIES_FILE, PRODUCT_NAME) {

        job.with {

            // Label which specifies which nodes this job can run on.
            label("pvt-static")

            // Adds environment variables to the build.
            environmentVariables {

                // Adds environment variables from a properties file.
                propertiesFile("\${HOME}/${CI_PROPERTIES_FILE}")

                // Inject Jenkins build variables and also environment contributors and build variable contributors provided by other plugins.
                keepBuildVariables(true)

                // Injects Jenkins system variables and environment variables defined as global properties and as node properties.
                keepSystemVariables(true)

                // Prepare repository build script
                if (PRODUCT_NAME == "intpack17") {

                    // Adds an environment variable to the build.
                    env("COMBINATION", "fuse63-bxms64")
                }
            }

            // Adds pre/post actions to the job.
            wrappers {

                // Deletes files from the workspace before the build starts.
                preBuildCleanup()

                // Adds timestamps to the console log.
                timestamps()
            }
        }
    }
}
