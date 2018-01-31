#!/usr/bin/bash

idx=${SLURM_ARRAY_JOB_ID}.${SLURM_ARRAY_TASK_ID}
exp=${exp:-shapecolor}
modeltype=${modeltype:-hier}
template=experiments/template/run/${exp}.template.exp

# Prepare template file.
export expfile=${template}.${idx}
sed "s/<idx>/$idx/g" < $template | sed "s/<model>/$modeltype/g" > $expfile

singularity exec --bind /om/scratch/Tue/jgauthie --bind /om/data/public/jgauthie ~/imgs/node-with-jdk8.simg ./run_bayesian.sh
