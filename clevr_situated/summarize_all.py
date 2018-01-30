from argparse import ArgumentParser

import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
import pandas as pd
import seaborn as sns


sns.set(color_codes=True)


def main(args):
  results = pd.read_table(args.unified_tsv, header=None, names=["i", "condition", "run", "variable", "value"])
  results = results[results.variable == "recall"]

  sns.tsplot(data=results, time="i", unit="run", condition="condition", value="value")

  plt.tight_layout()
  plt.savefig(args.img_out)


if __name__ == '__main__':
  p = ArgumentParser()

  p.add_argument("unified_tsv")
  p.add_argument("img_out")

  main(p.parse_args())
