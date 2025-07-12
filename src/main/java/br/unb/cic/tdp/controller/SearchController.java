package br.unb.cic.tdp.controller;

import br.unb.cic.tdp.base.Configuration;
import br.unb.cic.tdp.base.PivotedConfiguration;
import br.unb.cic.tdp.proof.ProofStorage;
import br.unb.cic.tdp.proof.SortOrExtend;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Arrays;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static br.unb.cic.tdp.proof.Extensions.permutationToJsArray;
import static br.unb.cic.tdp.proof.SortOrExtend.concatStreams;

@Controller
public class SearchController {

    @Autowired
    private ProofStorage proofStorage;

    public SearchController() {
    }

    @GetMapping("/search/{config}/{pivots}")
    public String handleSearch(final @PathVariable("config") String config, final @PathVariable("pivots") String pivots, final Model model) {
        val configuration = new Configuration(config);

        model.addAttribute("permutationToJsArray", permutationToJsArray(configuration.getSpi()));
        model.addAttribute("spi", configuration.getSpi());
        model.addAttribute("pivots", Arrays.stream(pivots.split(",")).map(Integer::parseInt).collect(Collectors.toCollection(TreeSet::new)));

        val sortOrExtend = new SortOrExtend(null, null, null, 0);

        val extensions = concatStreams(
                sortOrExtend.type1Extensions(configuration),
                sortOrExtend.type2Extensions(configuration),
                sortOrExtend.type3Extensions(configuration),
                sortOrExtend.type4Extensions(configuration),
                sortOrExtend.type5Extensions(configuration),
                sortOrExtend.type6Extensions(configuration),
                sortOrExtend.type7Extensions(configuration),
                sortOrExtend.type8Extensions(configuration),
                sortOrExtend.type9Extensions(configuration)
        )
                .map(pair -> {
                    val extension = Configuration.ofSignature(pair.getRight().getSignature().getContent());
                    val extensionPivots = sortOrExtend.sortingPivots(extension);
                    val canonical = PivotedConfiguration.of(extension, extensionPivots).getCanonical();
                    val canonicalLabel = canonical.toString();
                    val sorting = proofStorage.findSorting(canonical);
                    val description = pair.getLeft();
                    val color = sorting.isEmpty() ? "red" : "green";
                    val goodOrBad = sorting.isEmpty() ? "BAD EXTENSION" : "GOOD EXTENSION";
                    return new Extension(
                            extension.getSpi().toString(),
                            extensionPivots.toString(),
                            description,
                            color,
                            goodOrBad,
                            extension.hashCode(),
                            permutationToJsArray(extension.getSpi()),
                            canonicalLabel
                    );
                })
                .toList();

        model.addAttribute("extensions", extensions);

        return "case";
    }

    public record Extension(
            String extension,
            String pivots,
            String description,
            String color,
            String goodOrBad,
            int _hashCode,
            String permutationToJsArray,
            String canonical
    ) {
    }
}