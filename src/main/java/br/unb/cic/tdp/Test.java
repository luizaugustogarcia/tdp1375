package br.unb.cic.tdp;

import br.unb.cic.tdp.base.CommonOperations;
import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.PermutationGroups;
import org.apache.commons.lang.time.StopWatch;

public class Test {
    public static void main(String[] args) {
        var pi = Cycle.create("0,6,5,3,2,1,8,7,4,9,14,13,12,11,10");
        var stopWatch = new StopWatch();
        stopWatch.start();
        var s = new Silvaetal();
        stopWatch.stop();
        s.sort(pi);
        System.out.println(stopWatch.getTime() / 1000 / 60);
    }
}
