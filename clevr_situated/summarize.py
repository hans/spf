from argparse import ArgumentParser

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
    results_i = results_i[results_i.index < 200]
    results_i['run'] = file
    results_i = results_i.set_index('run', append=True).reorder_levels(['run', 'i'])
    results.append(results_i)

  results = pd.concat(results)

  results = results.reset_index().melt(id_vars=['i', 'run'], value_vars=['precision', 'recall'])
  print(results)
  sns.tsplot(data=results, time='i', unit='run', condition='variable', value='value')

  plt.tight_layout()
  plt.savefig("test.png")


if __name__ == '__main__':
  p = ArgumentParser()

  p.add_argument("files", action="store", nargs="+")

  main(p.parse_args())
