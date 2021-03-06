# TDP 1375

Algorithm for sorting by transpositions based on an algebraic formalism with guarantee of approximation ratio of 1.375 for all permutations in the Symmetric Group S_n (not only for the subset of the simple permutations). For the details on the algorithm, please refer to the paper "An algebraic 1.375-approximation algorithm for the Transposition Distance Problem" by L. A. G. Silva, L. A. B. Kowada, N. R. Rocco and M. E. M. T. Walter (preprint available [here](https://arxiv.org/abs/2001.11570)).

This project requires Maven.

To execute the program that generates the case analysis which is the base of the correctness proof of the algorithm, run the command

*mvn exec:java -Dexec.mainClass="br.unb.cic.tdp.proof.ProofGenerator" -Dexec.args="<first_arg> <second_arg> <third_arg>"*

where the first argument indicates the directory which the proof will be generated into. The second argument is a boolean indicating whether or not the program will use branch and bound to sort the configurations when an already known sorting (found by [Elias and Hartman](https://ieeexplore.ieee.org/abstract/document/4015379)) is not found. Setting it to *false*, will make the execution very short. However, a significant larger number of cases will be generated. If set to *true*, then a third argument, indicating the number of processors used by the branch and bound sorting, must be provided. Observe that using branch and bound makes the program require much more time to run. In our experience, it takes about 5 days to run using 40 parallel Intel Xeon™ cores.

To execute the proposed algorithm, run the command

*mvn exec:java -Dexec.mainClass="br.unb.cic.tdp.Silvaetal" -Dexec.args="<first_arg>"*

where the first argument is the permutation to be sorted. Before calling the algorithm, the program has to load into memory all cases generated in the correctness proof. This demands a significant amount of memory. Thus, we suggest, before running the algorithm, to increase the memory available to at least 10GB, by setting the environment variable MAVEN_OPTS, using the following command

*export MAVEN_OPTS="-Xmx10G"*.
