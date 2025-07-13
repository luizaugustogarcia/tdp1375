package br.unb.cic.tdp.controller;

import br.unb.cic.tdp.base.Configuration;
import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
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

    public static void main(String[] args) {
        val spi = new MulticyclePermutation("(0 9 13 4)(1 3)(2 11 5 12)(6 10 8)(7 14)");
        val spiMirror = new MulticyclePermutation("(0 7)(1 5 14 10)(2 9 3 12)(4 8 6)(11 13)\n");
        System.out.println(new Configuration(spi.times(Cycle.of("(2 11 12)").getInverse())));
        System.out.println(new Configuration(spiMirror.times(Cycle.of("(3 4 13)").getInverse())));
    }

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
