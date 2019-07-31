package org.jboss.bxms.jenkins

import ca.szc.configparser.Ini

/**
 * Loads the component build dependency map from different types of objects (resource, InputStream, URL).
 */
class DependencyGraphUtils {

    static Map<String, String[]> loadPackageMapFromResource(Class resourceLoader, String resourceName) {
        return loadPackageMapFromStream(resourceLoader.class.getResourceAsStream(resourceName))
    }

    static Map<String, String[]> loadPackageMapFromURL(String urlString) {
        return loadPackageMapFromStream(new URL(urlString).openStream())
    }

    static Map<String, String[]> loadPackageMapFromStream(InputStream inputStream) {
        BufferedReader configReader = new BufferedReader(new InputStreamReader(inputStream))
        try {
            return buildPackageMap(configReader)
        } finally {
            configReader.close()
        }
    }

    private static Map<String, String[]> buildPackageMap(Reader configReader) {
        Ini _ini_cfg = new Ini().read(configReader)
        Map<String,Map<String,String>> sections = _ini_cfg.getSections()
        Map<String, String[]> packagesMap = new HashMap()
        for (String section_name : sections.keySet())
        {
            Map<String, String> section=sections.get(section_name)
            if ((!section.containsKey("config_type")) || (section.containsKey("config_type") && section.get("config_type").equals("bom-builder")) )
            {
                if(section.containsKey("buildrequires")){
                    if(section.get("buildrequires").length()!=0){
                        String[] requiresArr=section.get("buildrequires").split(" ")
                        packagesMap.put(section_name,requiresArr)
                    } else {
                        packagesMap.put(section_name,"")
                    }
                } else {
                    packagesMap.put(section_name,"")
                }
            }
        }
        return packagesMap
    }


}
