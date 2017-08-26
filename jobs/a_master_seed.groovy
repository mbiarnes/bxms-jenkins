import org.jboss.bxms.jenkins.ReleasePipelineSeedJobBuilder

new ReleasePipelineSeedJobBuilder(
        product_name: "bxms64",
        ci_properties_file:"brms-64-jenkins-ci.properties",
        cfg_file:"brms-64.cfg",
        ip_makefile:"Makefile.BRMS",
        product_root_component:"bxms-maven-repo-root",
        bpms_deliverable_list_file:"bpmsuite-64-deliverable.properties",
        repo_builder_script:"regen_bxms_64_repo_builder.sh",
).build(this)

new ReleasePipelineSeedJobBuilder(
        product_name: "bxms70",
        ci_properties_file:"brms-jenkins-ci.properties",
        cfg_file:"brms.cfg",
        ip_makefile:"Makefile.BRMS",
        product_root_component:"bxms-maven-repo-root",
        bpms_deliverable_list_file:"bpmsuite-deliverable.properties",
        repo_builder_script:"regen_bxms_repo_builder.sh",
).build(this)

new ReleasePipelineSeedJobBuilder(
        product_name: "intpack-fuse63-bxms64",
        ci_properties_file:"intpack-fuse63-bxms64-jenkins-ci.properties",
        cfg_file:"intpack-fuse63-bxms64.cfg",
        ip_makefile:"Makefile.IntPack",
        product_root_component:"fuse-integration",
        bpms_deliverable_list_file:"",
        repo_builder_script:"regen_bxms_intpack_repo_builder.sh",
).build(this)

folder("utility")
job('utility/utility-seed') {
    logRotator {
        numToKeep 8
    }
    scm {
        // Adds a Git SCM source.
        git {

            // Adds a remote.
            remote {

                // Sets the remote URL.
                url("ssh://jb-ip-tooling-jenkins@code.engineering.redhat.com:22/bxms-jenkins")
            }

            // Specify the branches to examine for changes and to build.
            branch("master")
        }
    }
    label("pvt-static")
    triggers {
        scm 'H/5 * * * *'
    }
    steps {

        dsl {
            external 'streams/utility/*.groovy'
            additionalClasspath 'src/main/groovy'

            // Specifies the action to be taken for job that have been removed from DSL scripts.
            removeAction('DELETE')
            // Specifies the action to be taken for views that have been removed from DSL scripts.
            removeViewAction('DELETE')
        }

        triggers {
            upstream('a-master-seed', 'SUCCESS')
        }
    }

}