from pathlib import Path
import re
import sys


SPLIT = "train"
DATA_PATH = Path("/om/data/public/jgauthie/CLEVR_v1.0/questions_functionalized.%s.txt" % SPLIT)

PREDS_PATH = Path(__file__).parents[0] / "resources" / "geo.preds.ont"
with PREDS_PATH.open("r") as f:
  pred_types = dict([tuple(line.strip().split(":")) for line in f if ":" in line])

token_re = re.compile(r"(?<=[(\s])([\w_]+)(?=[)\s])")


def process_sexpr(line):
  line = line.replace("exist_", "exists")
  # Add types.
  try:
    line = token_re.sub(lambda match: "%s:%s" % (match.group(1), pred_types[match.group(1)]), line)
  except KeyError as e:
    print(line)
    raise e
  return line


def process_sentence(line):
  return line.lower().replace("?", "").replace(";", " SEMI")


def finalize(sentence, sexpr):
  print(sentence)
  print(sexpr)
  print()


with DATA_PATH.open("r") as data_f:
  limit = int(sys.argv[1]) if len(sys.argv) > 1 else 5
  n = 0

  sentence, sexpr = None, None
  for line in data_f:
    if n == limit:
      break

    line = line.strip()
    if sentence is not None and line.startswith("("):
      sexpr = process_sexpr(line)
      finalize(sentence, sexpr)
      n += 1

      sentence, sexpr = None, None
    elif sentence is None and line:
      sentence = process_sentence(line)
    elif not line:
      continue
    else:
      raise ValueError("Unexpected line %r" % line)
