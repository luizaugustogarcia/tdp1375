package br.unb.cic.tdp.controller;

import br.unb.cic.tdp.base.Configuration;
import lombok.val;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Arrays;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static br.unb.cic.tdp.base.PivotedConfiguration.of;
import static br.unb.cic.tdp.proof.Extensions.permutationToJsArray;

@Controller
public class EquivalenceController {

    @GetMapping("/equivalent/{config}/{pivots}")
    public String handleSearch(final @PathVariable("config") String config, final @PathVariable("pivots") String pivots, final Model model) {
        val pivotedConfiguration = of(
                new Configuration(config),
                Arrays.stream(pivots.split(",")).map(Integer::parseInt).collect(Collectors.toCollection(TreeSet::new))
        );

        model.addAttribute("permutationToJsArray", permutationToJsArray(pivotedConfiguration.getConfiguration().getSpi()));
        model.addAttribute("spi", pivotedConfiguration.getConfiguration().getSpi());
        model.addAttribute("pivots", pivotedConfiguration.getPivots());
        model.addAttribute("hashCode", pivotedConfiguration.getConfiguration().hashCode());

        val equivalent = pivotedConfiguration.getEquivalent().stream()
                .map(eq -> new Equivalent(
                        eq.getConfiguration().getSpi().toString(),
                        eq.getConfiguration().hashCode(),
                        eq.getPivots().toString(),
                        permutationToJsArray(eq.getConfiguration().getSpi())
                ))
                .toList();

        model.addAttribute("equivalent", equivalent);

        return "equivalent";
    }

    public record Equivalent(
            String description,
            int _hashCode,
            String pivots,
            String permutationToJsArray
    ) {
    }
}
