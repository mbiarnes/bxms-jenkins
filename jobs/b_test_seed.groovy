import org.jboss.bxms.jenkins.ReleasePipelineSeedJobBuilder
import org.jboss.bxms.jenkins.GeneralSeedJobBuilder

//This seed job is responsible for creating testing stream jobs and produce jobs for bxms-jenkins code review
new ReleasePipelineSeedJobBuilder(
        product_name: "bxms70",
        ci_properties_file:"brms-jenkins-ci.properties",
        cfg_file:"brms.cfg",
        ip_makefile:"Makefile.BRMS",
        product_root_component:"bxms-maven-repo-root",
        bpms_deliverable_list_file:"bpmsuite-deliverable.properties",
        repo_builder_script:"regen_bxms_repo_builder.sh",
).build(this)

new GeneralSeedJobBuilder(
        stream_name: "utility"
).build(this)
