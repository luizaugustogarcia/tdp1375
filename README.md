# tdp1375
Algorithm for sorting by transpositions based on an algebraic approach with guarantee of approximation ratio of 1.375, conceived as part of the Ph.D. thesis of Luiz Silva.

This project requires Maven to be compiled.

The class "br.unb.cic.tdp.Silvaetal" implements the algorithm proposed by Silva et al (article not yet published).

To run the algorithm:

1. Copy the content of "tdp1375/deps/" folder to your .m2 folder
2. Uncompress the file "cases.tar.gz"
3. Run the main method of "br.unb.cic.tdp.Silvaetal" providing as parameters: the folder into which the file "cases.tar.gz" was decompressed (first parameter), then the permutation to sort (last parameter), e.g., 0,8,7,6,5,4,3,2,1.

The package "br.unb.cic.tdp.proof" contains the programs responsible to generate all the cases employed by the proposed algorithm. To generate the cases corresponding to the desimplifications of the catalog created by Elias and Hartman (A 1.375-approximation algorithm for sorting by transpositions), it is necessary to download and decompress the file [SBT1375_proof.tar.gz](https://www.dropbox.com/s/kug9x7nguyeskyk/sbt1375_proof.tar.gz?dl=0). The path for this catalog on the localhost must be provided in the classes "br.unb.cic.tdp.proof.Desimplify*.java".
