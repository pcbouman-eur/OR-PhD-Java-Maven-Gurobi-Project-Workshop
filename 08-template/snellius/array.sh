#!/bin/bash
# One task per instance file: sbatch --array=0-$(( $(ls instances/*.instance.json* | wc -l) - 1 )) array.sh
#SBATCH --job-name=cflp-array
#SBATCH --partition=rome
#SBATCH --cpus-per-task=4
#SBATCH --time=00:15:00
#SBATCH --output=slurm-%A_%a.out

module load 2024
module load Java/21.0.5
export GRB_LICENSE_FILE=$HOME/gurobi.lic

cd $HOME/cflp
FILES=(instances/*.instance.json*)
INSTANCE=${FILES[$SLURM_ARRAY_TASK_ID]}

# Two treatments per instance, same experiment, different label/tag
java -jar cflp.jar solve "$INSTANCE" -e cooling --solver sa --t-start 0.5 -l sa-cold -t start=cold
java -jar cflp.jar solve "$INSTANCE" -e cooling --solver sa --t-start 2.0 -l sa-warm -t start=warm
