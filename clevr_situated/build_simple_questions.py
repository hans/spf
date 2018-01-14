"""
Using a CLEVR scene dataset, build a dataset of simple reference-game questions.
"""

from argparse import ArgumentParser
from collections import defaultdict
import itertools
import json
import random


def yield_base_references(scene):
  for i, obj in enumerate(scene["objects"]):
    yield "the %s" % obj["shape"], i


def yield_1adj_references(scene):
  for i, obj in enumerate(scene["objects"]):
    for prop in ["material", "color", "size"]:
      for base_noun in [obj["shape"], "object"]:
        yield "the %s %s" % (obj[prop], base_noun), i


REFERENCE_FNS = [yield_base_references, yield_1adj_references]
REFERENCE_FNS = {fn.__name__: fn for fn in REFERENCE_FNS}


def make_questions(scene, reference_fns):
  """
  Given a CLEVR scene, generate simple questions.
  """

  # Find unique referents.
  referents = defaultdict(set)
  for reference_fn in reference_fns:
    for utt, obj_idx in reference_fn(scene):
      referents[utt].add(obj_idx)

  # Only yield questions which have a unique referent.
  questions = [(scene["image_index"], utt, next(iter(referents)))
               for utt, referents in referents.items()
               if len(referents) == 1]
  return questions


def main(args):
  reference_fns = args.reference or ["yield_1adj_references"]
  reference_fns = [REFERENCE_FNS[fn] for fn in reference_fns]

  with open(args.scenes_path, "r") as scenes_f:
    scene_data = json.load(scenes_f)
  scenes = scene_data["scenes"]

  questions = list(itertools.chain.from_iterable(
    (make_questions(scene, reference_fns)) for scene in scenes))
  random.shuffle(questions)

  enumerator = range(args.limit) if args.limit > 0 else itertools.count()

  out_data = {
      "info": {"split": "simple"},
      "questions": [
        {
          "question_index": i,
          "image_index": image_index,
          "answer": {"type": "object", "index": obj_index},
          "question": question
        } for i, (image_index, question, obj_index) in zip(enumerator, questions)
        ]
      }

  with open(args.out_path, "w") as out_f:
    json.dump(out_data, out_f)


if __name__ == '__main__':
  p = ArgumentParser()
  p.add_argument("scenes_path")
  p.add_argument("out_path")
  p.add_argument("--limit", default=0, type=int)
  p.add_argument("--reference", choices=REFERENCE_FNS.keys(), action="append")

  main(p.parse_args())
