package org.jboss.bxms.jenkins

import java.util.logging.Logger

/**
 * Turns cfg requirement relationship into oneline steps using Kahn Topological Sorting algorithm.
 *
 * The input directed graph has to be acyclic, otherwise IllegalArgumentException is thrown.
 */
class DependencyGraphSorter {

    private static final Logger LOG = Logger.getLogger(DependencyGraphSorter.class.getName())

    static List<String> kahnTopological(Map<String,String[]> packagesMap) {
        LOG.fine("Computing topological sort of the following graph: " + packagesMap)

        ArrayList<String> resultAL=new ArrayList<String>()
        Map packagesMapIndegree=new HashMap()
        ArrayList<String> zeroIndegreeArr=new ArrayList<String>()
        int allEdges=0;
        for (String keyName : packagesMap.keySet()) {
            if (packagesMap.get(keyName).size()==0) {
                zeroIndegreeArr.add(keyName)

            }
            allEdges+=packagesMap.get(keyName).size()
            packagesMapIndegree.put(keyName,packagesMap.get(keyName).size())
        }

        while(zeroIndegreeArr.size()>0){
            String current=zeroIndegreeArr.get(0)
            zeroIndegreeArr.remove(0)
            resultAL.add(current)
            ArrayList<String> currentPkgChild =new ArrayList<String>()
            for (Map.Entry<String, String[]> entry : packagesMap.entrySet()) {
                int flag=0
                for (ele in entry.getValue()) {
                    if(ele.matches(current)){
                        flag=1
                        break
                    }
                }
                if (flag ==1) {
                    currentPkgChild.add(entry.getKey())
                }
            }
            for (int i=0;i<currentPkgChild.size() ;i++ ) {
                int indegree=packagesMapIndegree.get(currentPkgChild.get(i))-1
                allEdges=allEdges-1
                packagesMapIndegree.put(currentPkgChild.get(i),indegree)
                if (indegree == 0) {
                    zeroIndegreeArr.add(currentPkgChild.get(i))
                }
            }
        }

        if (allEdges!=0) {
            throw new IllegalArgumentException("There are circles or edges to missing nodes in buildrequires map, remaining edges: "
                    + allEdges + ", the graph in-degree map:\n" + packagesMapIndegree)
        }
        return resultAL;
    }

}
