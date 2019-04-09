This directory contains ansible roles and data for doing cekit osbs builds of rhba images
from a Jenkins slave image. It is stored here because it references internal systems and configurations,
and so should not be public, but it is meant to be an addendum to https://github.com/kiegroup/kie-jenkins-scripts.git.
The kie-jenkins-scripts repo has a script called `add-osbs.sh` which clones this repo and copies the
files from rhba-osbs to approprite locations.

The structure of this directory should be maintained. It will be copied using rsync by `add-osbs.sh`, so any
files here will be merged into the ansible subdirectory in kie-jenkins-scripts. An ansible/data/rhba-osbs
has been created to contain all of the data for this particular feature, and an ansible/roles/rhba-osbs
has been created which will reference files from data/rhaba-osbs in its main.yml. 
