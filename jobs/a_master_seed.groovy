import org.jboss.bxms.jenkins.ReleasePipelineSeedJobBuilder
import org.jboss.bxms.jenkins.GeneralSeedJobBuilder

//Establish the parametize release pipeline
new ReleasePipelineSeedJobBuilder(
        product_name: "bxms64",
        ci_properties_file:"/jboss-prod/config/brms-64-ci.properties",
        cfg_file:"brms-64.cfg",
).build(this)

new ReleasePipelineSeedJobBuilder(
        product_name: "bxms70",
        ci_properties_file:"/jboss-prod/config/brms-ci.properties",
        cfg_file:"brms.cfg",
).build(this)

new ReleasePipelineSeedJobBuilder(
        product_name: "intpack-fuse63-bxms64",
        ci_properties_file:"/jboss-prod/config/intpack-fuse63-bxms64-ci.properties",
        cfg_file:"intpack-fuse63-bxms64.cfg",
).build(this)

new ReleasePipelineSeedJobBuilder(
        product_name: "bxms-test",
        ci_properties_file:"/jboss-prod/config/bxms-test-ci.properties",
        cfg_file:"brms-test.cfg",
).build(this)

new GeneralSeedJobBuilder(
        stream_name: "utility"
).build(this)

new GeneralSeedJobBuilder(
        stream_name: "codereview"
).build(this)
