package br.unb.cic.tdp.controller;

import br.unb.cic.tdp.base.CommonOperations;
import br.unb.cic.tdp.base.Configuration;
import br.unb.cic.tdp.proof.ProofStorage;
import br.unb.cic.tdp.proof.SortOrExtend;
import lombok.val;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Set;

import static br.unb.cic.tdp.proof.Extensions.permutationToJsArray;

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
        model.addAttribute("pivots", pivots.replace("(", "").replace(")", "").replace(" ", ","));

        val sortOrExtend = new SortOrExtend(null, null, null, 0);

        val extensions = sortOrExtend
                .type1Extensions(configuration)
                .map(pair -> {
                    val extension = Configuration.ofSignature(pair.getRight().getSignature().get().getContent());
                    val extensionPivots = sortOrExtend.sortingPivots(extension);
                    val canonicalPair = sortOrExtend.canonicalize(Pair.of(extension, extensionPivots));
                    val canonical = canonicalPair.getLeft().getSpi() + "#" + canonicalPair.getRight();
                    val sorting = proofStorage.findSorting(canonicalPair);
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
                            canonical
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