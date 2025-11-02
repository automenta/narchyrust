#!/bin/bash

keep_files=(
    "inh.nal"
    "sim.nal"
    "diff.nal"
    "diff.goal.nal"
    "set.compose.nal"
    "set.decompose.nal"
    "set.guess.nal"
    "analogy.anonymous.conj.nal"
    "analogy.anonymous.impl.nal"
    "analogy.mutate.nal"
    "cond.decompose.nal"
    "cond.decompose.must.nal"
    "cond.decompose.might.nal"
    "cond.decompose.would.nal"
    "cond.decompose.wouldve.nal"
    "contraposition.nal"
    "conversion.nal"
    "impl.syl.nal"
    "impl.syl.cond.nal"
    "impl.syl.combine.nal"
    "impl.strong.nal"
    "impl.strong.cond.nal"
    "impl.compose.nal"
    "impl.decompose.self.nal"
    "impl.decompose.specific.nal"
    "impl.recompose.nal"
)

for file in resources/*.nal; do
    should_keep=false
    for keep_file in "${keep_files[@]}"; do
        if [[ "$file" == "resources/$keep_file" ]]; then
            should_keep=true
            break
        fi
    done

    if [ "$should_keep" = false ]; then
        rm "$file"
    fi
done
