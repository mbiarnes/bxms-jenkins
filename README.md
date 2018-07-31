BxMS DSL script:

BxMS DSL controlls Jenkins job and release pipelinefor BxMS product CI which
is in https://brms-bpms-jenkins.rhev-ci-vms.eng.rdu2.redhat.com/

All the job dsl are located in jobs/*.groovy

How to test your DSL script:

./gradlew test

JobScriptsSpec.grrovy will test (compile and deploy in embedded jenkins) all DSL files from the ./jobs and check for exceptions. All successful generated XML files will be written to ./build/debug-xml directory.
If your DSL contains automation generated API, you could add the testplugin in build.gradle and configure the maven repository url in it as well.
If your plugin isn't available in maven repo, a fatdir lib is also possible by specify
repositories {
    mavenCentral()
    jcenter()
    
    flatDir(dirs: 'lib')   
}


File structure description:
.
├── build
├── build.gradle
├── bxms-jenkins.iml
├── config                                           #Config file used in release
                                                        pipeline
│   ├── rhpam-deliverable.properties                  #rhpam deliverable file
│   ├── rhpam-pvt-config.yaml                         #pvt config
│   ├── rhpam-image-stream.json                   
│   ├── rhdm-deliverable.properties
│   ├── rhdm-pvt-config.yaml
│   ├── properties-mapping.template
│   ├── release-handover.template
│   └── release-notes.template
├── gradle
│   └── wrapper
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── gradle.properties
├── gradlew
├── jobs
│   ├── a_master_seed.groovy
│   ├── *..groovy
│── lib                                                 # For storage of other 
                                                          third-party plugins 
                                                          through flatDir. 
                                                          See build.gradle
├── LICENSE
├── README
├── src
│   ├── main
│   │   ├── groovy
│   │   ├── main.iml
│   │   └── resources
│   └── test
│       └── groovy
│              └── JobScriptsSpec.groovy   # Complie and generate XML files
│                                                          for all DSL script
│── gradle                                              # Gradle wrapper
│  
│── build.gradle                                        # Build file 
│  
│── gradle.properties                                   # Store Jenkins and DSL versions
└── streams
    ├── bxms
    │   ├── config
    │   ├── dsl
    │   ├── release-history
    │   └── release_pipeline_seq.cfg
    ├── bxms64
    │   ├── config
    │   ├── dsl
    │   └── release-history
    ├── bxms70la
    │   ├── config
    │   ├── dsl
    │   ├── release-history
    │   └── release_pipeline_seq.cfg
    ├── bxms-test
    │   ├── config
    │   ├── dsl
    │   ├── release-history
    │   └── release_pipeline_seq.cfg
    ├── codereview
    │   ├── pr_bxms7_assembly.groovy -> ../../jobs/pr_bxms7_assembly.groovy
    │   ├── pr_bxms_jenkins.groovy -> ../../jobs/pr_bxms_jenkins.groovy
    │   └── pr_bxms_licenses_builder.groovy -> ../../jobs/pr_bxms_licenses_builder.groovy
    ├── intpack-fuse63-bxms64
    │   ├── config
    │   ├── dsl
    │   └── release-history
    └── utility
        ├── utility_bxms_ci_message_monitor.groovy -> ../../jobs/utility_bxms_ci_message_monitor.groovy
        └── utility_pme_update.groovy -> ../../jobs/utility_pme_update.groovy


Reference:
The Gradle Test Harness forked from Jenkins Job DSL Gradle Example 
https://github.com/sheehan/job-dsl-gradle-example
