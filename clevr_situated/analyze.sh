#!/bin/bash

declare -a results
condition=$1
shift
for x in $@; do
    results+=( "${x}.all_results" )
    echo -e "i\trecall\tprecision\tf1" > ${x}.online_results
    echo "p(shape|N/N)" > ${x}.posterior_results

    grep -A 1 'Online test results' $x | awk 'BEGIN {OFS="\t"; i = 1} /metric/ {$1=$1; split($2,recall,"="); split($3,precision,"="); split($4,f1,"="); print i, recall[2], precision[2], f1[2]; i += 1}' >> ${x}.online_results
    grep 'N/N posterior' $x | cut -f2 | cut -d'=' -f2 >> ${x}.posterior_results

    paste ${x}.online_results ${x}.posterior_results > ${x}.all_results
done

python summarize.py --condition $condition --img-out summary.png --tsv-out summary.tsv ${results[@]}
