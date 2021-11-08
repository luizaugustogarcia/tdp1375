# TDP 1375

Algorithm for sorting by transpositions based on an algebraic approach with guarantee of approximation ratio of 1.375 for all permutations in the Symmetric Group S_n (not only for the subset of the simple permutations). For the details on the algorithm, please refer to the paper "A new 1.375-approximation algorithm for Sorting By Transpositions" by L. A. G. Silva, L. A. B. Kowada, N. R. Rocco and M. E. M. T. Walter (preprint available [here](https://arxiv.org/abs/2001.11570)).

This project requires Maven.

To execute the program that generates the case analysis which is the base of the correctness proof of the algorithm, run the command

`mvn exec:java -Dexec.mainClass="br.unb.cic.tdp.proof.ProofGenerator" -Dexec.args="<first_arg>"`

where the first argument indicates the directory which the proof will be generated into.

The case analysis generated by the command above is hosted [here](http://tdp1375proof.s3-website.us-east-2.amazonaws.com/).

To execute the proposed algorithm, run the command

`mvn exec:java -Dexec.mainClass="br.unb.cic.tdp.Silvaetal" -Dexec.args="<first_arg>"`

where the first argument is the permutation to be sorted, e.g, `0,4,8,3,7,2,6,1,5,9,14,13,12,11,10`. Note that, before executing the algorithm itself, the program has to load into memory all cases generated in the case analysis. This demands a significant amount of memory (~20GB) and takes about 1 minute to complete in an Intel i7™ vPro 8th Gen computer with 48GB of RAM. Therefore, we suggest, before running the algorithm, to increase the memory available to at least 20GB, by setting the environment variable MAVEN_OPTS="-Xmx20G".
