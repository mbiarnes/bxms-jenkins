package org.jboss.bxms.jenkins

import groovy.test.GroovyAssert
import org.junit.Test

class DependencyGraphSorterTest {

    @Test
    public void testSimple() {
        Map<String,String[]> packagesMap = [ "first" : [ "second" ], "second": [ "third" ], "third": [] ]
        List<String> expected = [ "third", "second", "first" ]

        List<String> sorted = DependencyGraphSorter.kahnTopological(packagesMap)
        GroovyAssert.assertArrayEquals("Unexpected sort result", expected.toArray(), sorted.toArray())
    }

    @Test
    public void testRHDMMasterNightlyCfg() {
        Map<String,String[]> packagesMap = DependencyGraphUtils.loadPackageMapFromResource(DependencyGraphSorterTest.class,
                "/org/jboss/bxms/jenkins/rhdm-master-nightly-dev.cfg")
        List<String> expected = [ "rhdm-build-bom", "rhba-license-builder", "rhba-common",
                                  "rhdm", "rhdm-installer", "rhdm-maven-repo-root"
                                ]

        List<String> sorted = DependencyGraphSorter.kahnTopological(packagesMap)
        GroovyAssert.assertArrayEquals("Unexpected sort result", expected.toArray(), sorted.toArray())
    }

    @Test
    public void testRHPAMMasterNightlyCfg() {
        Map<String,String[]> packagesMap = DependencyGraphUtils.loadPackageMapFromResource(DependencyGraphSorterTest.class,
                "/org/jboss/bxms/jenkins/rhpam-master-nightly-dev.cfg")
        List<String> expected = [ "rhpam-build-bom", "droolsjbpm-build-bootstrap", "errai", "izpack", "kie-soup",
                                  "lienzo-core", "kie-uberfire-extensions", "installer-commons", "droolsjbpm-knowledge",
                                  "lienzo-tests", "appformer", "drools", "jbpm", "optaplanner", "rhba-boms", "droolsjbpm-integration",
                                  "optashift-employee-rostering", "kie-wb-playground", "jbpm-work-items", "openshift-drools-hacep",
                                  "kie-wb-common", "drools-wb", "jbpm-designer", "optaplanner-wb", "jbpm-wb", "kie-wb-distributions",
                                  "rhba-license-builder", "rhba-common", "rhpam", "rhpam-installer", "rhpam-maven-repo-root"
                                ]

        List<String> sorted = DependencyGraphSorter.kahnTopological(packagesMap)
        GroovyAssert.assertArrayEquals("Unexpected sort result", expected.toArray(), sorted.toArray())
    }
}
