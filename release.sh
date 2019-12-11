#!/bin/sh
echo ""
echo "##############################################"
echo "  Now Releasing Design Time Governance (Community)"
echo "##############################################"
echo ""
read -p "Release Version: " RELEASE_VERSION
read -p "New Development Version: " DEV_VERSION
mvn -e --batch-mode -DreleaseVersion=$RELEASE_VERSION -DdevelopmentVersion=$DEV_VERSION -DignoreSnapshots=true clean release:prepare release:perform
