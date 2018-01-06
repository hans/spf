"""
Using a CLEVR scene dataset, build a dataset of simple reference-game questions.
"""

from argparse import ArgumentParser
from collections import defaultdict
import itertools
import json
import random


def make_questions(scene):
  """
  Given a CLEVR scene, generate simple questions.
  """

  # Find unique referents.
  referents = defaultdict(list)
  for i, obj in enumerate(scene["objects"]):
    for prop in ["material", "color", "size"]:
      for base_noun in [obj["shape"], "object"]:
        referents[obj[prop], base_noun].append(i)

  # Only yield questions which have a unique referent.
  questions = [(scene["image_index"], "the %s %s" % q, referents[0])
               for q, referents in referents.items()
               if len(referents) == 1]
  return questions


def main(args):
  with open(args.scenes_path, "r") as scenes_f:
    scene_data = json.load(scenes_f)
  scenes = scene_data["scenes"]

  questions = list(itertools.chain.from_iterable(map(make_questions, scenes)))
  random.shuffle(questions)

  out_data = {
      "info": {"split": "simple"},
      "questions": [
        {
          "question_index": i,
          "image_index": image_index,
          "answer": {"type": "object", "index": obj_index},
          "question": question
        } for i, (image_index, question, obj_index) in enumerate(questions)
        ]
      }

  with open(args.out_path, "w") as out_f:
    json.dump(out_data, out_f)


if __name__ == '__main__':
  p = ArgumentParser()
  p.add_argument("scenes_path")
  p.add_argument("out_path")

  main(p.parse_args())
