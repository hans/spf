from argparse import ArgumentParser

import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
from scipy import stats
import seaborn as sns


sns.set(color_codes=True)

XLIM = (0, 100)
YLIM = (-0.1, 1.1)


def standard_error(xs):
  return xs.std() / np.sqrt(len(xs))


def main(args):
  results = pd.read_table(args.unified_tsv, header=None, names=["i", "condition", "run", "variable", "value"])
  results = results[results.i <= XLIM[1]]

  recall = results[results.variable == "recall"]
  # TODO plot with SEM rather than CI to stay consistent
  fig, _ = plt.subplots()
  ax = sns.tsplot(data=recall, time="i", unit="run", condition="condition", value="value", zorder=100)

  ax.set_xlabel("# examples")
  ax.set_ylabel("Recall")
  ax.set_xlim(XLIM)
  ax.set_ylim(YLIM)

  fig.tight_layout()
  fig.savefig("%s.learning.png" % args.img_out)

  ###############

  fig, ax = plt.subplots()
  results_model = results[results.condition == "model"]
  results_baseline = results[results.condition == "ccg"]
  results_diff = results_model.groupby("i").value.agg("mean") - results_baseline.groupby("i").value.agg("mean")

  results_diff.plot(y="value", ax=ax, legend=False)

  ax.set_xlabel("# examples")
  ax.set_ylabel("Difference in recall (model - CCG)")

  right_ax = ax.twinx()
  right_ax.set_ylabel("p(color|N/N)")
  posterior = results[results.condition != "ccg"]
  posterior = posterior[posterior.variable == "p(shape|N/N)"]
  posterior = posterior.groupby("i").value.agg(["mean", standard_error])
  posterior.plot(y="mean", ax=right_ax, color="#aaaaaa", zorder=1, legend=False)
  right_ax.fill_between(posterior.index, posterior["mean"] - posterior.standard_error, posterior["mean"] + posterior.standard_error, color="#ccccccaa")
  right_ax.set_ylim(0, 1)
  # Align axes.
  right_ax.set_yticks(np.linspace(right_ax.get_yticks()[0], right_ax.get_yticks()[-1], len(ax.get_yticks())))
  right_ax.grid(None)

  fig.tight_layout()
  fig.savefig("%s.overhyp.png" % args.img_out)


if __name__ == '__main__':
  p = ArgumentParser()

  p.add_argument("unified_tsv")
  p.add_argument("img_out")

  main(p.parse_args())
