#!/bin/bash
#SBATCH --job-name=cflp
#SBATCH --partition=rome          # check: sinfo / SURF user docs
#SBATCH --cpus-per-task=4
#SBATCH --time=00:10:00
#SBATCH --output=slurm-%j.out

# Java from the module system (check names with: module avail Java)
module load 2024
module load Java/21.0.5

# Only the license is configured outside the jar
export GRB_LICENSE_FILE=$HOME/gurobi.lic

# Let Gurobi use exactly the cores Slurm gave us (default: all cores of the node!)
java -jar $HOME/cflp/cflp.jar 50 200 1 $SLURM_CPUS_PER_TASK
