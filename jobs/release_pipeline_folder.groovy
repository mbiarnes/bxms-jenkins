// Creates or updates a folder. Don't delete this files because this is required in src/test/*/JobScriptsSpec.groovy
folder("${PRODUCT_NAME}-release-pipeline") {

    // Sets a description for the folder.
    description("Sample Release Pipeline")
}
