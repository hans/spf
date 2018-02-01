from argparse import ArgumentParser
from pprint import pprint

import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
import seaborn as sns
import scipy.stats as st


sns.set(color_codes=True)


def main(args):
  results = []
  for file in args.files:
    file = file.strip()
    results_i = pd.read_table(file, index_col=0)
    results_i['run'] = file
    results_i = results_i.set_index('run', append=True).reorder_levels(['run', 'i'])
    results.append(results_i)

  results = pd.concat(results).reset_index()

  # Trim time series.
  trim_time = 200
  results = results[results.i <= trim_time]

  # Pad time series to maximum time.
  max_time = min(results.agg({"i": "max"})[0], trim_time)
  runs = results.groupby("run")
  last_scores = {run: vals.loc[vals.i.idxmax()] for run, vals in runs}
  to_append = []
  for run, vals in runs:
    last_row = vals.loc[vals.i.idxmax()]
    last_time = last_row["i"]

    if last_row["f1"] != 1.0:
      # Don't pad crashed runs.
      continue

    for i in range(last_time + 1, max_time + 1):
      padding_el = last_row.copy()
      padding_el["i"] = i
      to_append.append(padding_el)

  if to_append:
    results = results.append(to_append, ignore_index=True)

  #########

  results = results.melt(id_vars=['i', 'run'], value_vars=['precision', 'recall', 'p(shape|N/N)'])

  if args.img_out:
    sns.tsplot(data=results, time='i', unit='run', condition='variable', value='value')

    plt.tight_layout()
    plt.savefig(args.img_out)

  if args.tsv_out:
    if not args.condition:
      raise ValueError("must provide --condition to output tsv")
    results["condition"] = args.condition
    results.to_csv(args.tsv_out, sep="\t", columns=["i", "condition", "run", "variable", "value"],
                   header=False, mode="a")


if __name__ == '__main__':
  p = ArgumentParser()

  p.add_argument("files", action="store", nargs="+")
  p.add_argument("--img-out")
  p.add_argument("--condition")
  p.add_argument("--tsv-out")

  main(p.parse_args())
