def job_d = """"This job should create a jobautomatically check and update jenkins-PME-tool"""
folder ("utility")
job('utility/bxms-pme-update'){
   description("$job_d")
   // check daily 
   triggers{
        cron('@daily')
   }
   label('volumn-node')
   
   // build steps 
   steps{
        // shell script to check latest version of PME and update accordingly
        shell('''#!/bin/bash
echo "Checking maven-metadata.xml ..."
if ! wget http://download-node-02.eng.bos.redhat.com/brewroot/repos/jb-bxms-6.4-build/latest/maven/org/commonjava/maven/ext/pom-manipulation-ext/maven-metadata.xml;then
	echo "Download failed"
    exit 1
fi
version=$(grep -oP '(?<=latest>)[^<]+' "./maven-metadata.xml")
echo "Latest version online is $version"
echo "In ext:"
cd /mnt/jboss-prod/tools/maven-3.3.9-prod/lib/ext/
if ls|grep "pom-manipulation-ext-$version.jar";then
	echo "Find the latest version"
else
    echo "Does not find the latest version"
	echo "Clearing previous version..."
    find |grep "pom-manipulation-ext"|grep ".jar"| xargs rm -f
	if wget http://download-node-02.eng.bos.redhat.com/brewroot/repos/jb-bxms-6.4-build/latest/maven/org/commonjava/maven/ext/pom-manipulation-ext/"$version"/pom-manipulation-ext-"$version".jar;then
	    echo "Latest version has downloaded"
    else
        exit 1
    fi
fi
echo "finished"'''
)
    }
   // clear workspace 
    wrappers {
        preBuildCleanup()
    }
}

