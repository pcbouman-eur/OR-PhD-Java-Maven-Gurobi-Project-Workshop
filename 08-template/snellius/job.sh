#!/bin/bash
#SBATCH --job-name=cflp-batch
#SBATCH --partition=rome          # check: sinfo / SURF user docs
#SBATCH --cpus-per-task=4
#SBATCH --time=01:00:00
#SBATCH --output=slurm-%j.out

module load 2024
module load Java/21.0.5
export GRB_LICENSE_FILE=$HOME/gurobi.lic

cd $HOME/cflp
# --threads defaults to $SLURM_CPUS_PER_TASK
java -DLOG_DIR=logs -jar cflp.jar batch instances/ --solver mip --time-limit 300 -e mip-baseline
