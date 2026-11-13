#!/bin/bash
# One task per instance file: sbatch --array=0-$(( $(ls instances/*.instance.json | wc -l) - 1 )) array.sh
#SBATCH --job-name=cflp-array
#SBATCH --partition=rome
#SBATCH --cpus-per-task=4
#SBATCH --time=00:15:00
#SBATCH --output=slurm-%A_%a.out

module load 2024
module load Java/21.0.5
export GRB_LICENSE_FILE=$HOME/gurobi.lic

cd $HOME/cflp
FILES=(instances/*.instance.json)
java -jar cflp.jar solve "${FILES[$SLURM_ARRAY_TASK_ID]}" --solver mip --threads $SLURM_CPUS_PER_TASK -o solutions
