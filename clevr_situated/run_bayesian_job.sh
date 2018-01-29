#!/usr/bin/bash

idx=${SLURM_ARRAY_JOB_ID}.${SLURM_ARRAY_TASK_ID}
template=experiments/template/run/shapecolor.template.exp

# Prepare template file.
export expfile=${template}.${idx}
sed "s/<idx>/$idx/g" < $template > $expfile

singularity exec --bind /om/scratch/Sun/jgauthie --bind /om/data/public/jgauthie ~/imgs/node-with-jdk8.simg ./run_bayesian.sh
