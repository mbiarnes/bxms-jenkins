// Creates or updates a folder.
folder("${PRODUCT_NAME}-release-pipeline") {

    // Sets a description for the folder.
    description("Sample Release Pipeline")

    if(${PRODUCT_NAME} == 'intpack17') {
        println("test!!!!")
    }
}