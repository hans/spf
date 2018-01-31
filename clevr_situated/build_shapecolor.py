from argparse import ArgumentParser
from collections import defaultdict
import json
import random

import numpy as np


SHAPES = ["s%i" % i for i in range(20)]
COLORS = ["c%i" % i for i in range(20)]
MATERIALS = ["rubber", "metal"]
SIZES = ["large", "small"]


def make_obj_answer(idx):
  return (("type", "object"), ("index", idx))


def yield_questions(scene):
  for i, obj in enumerate(scene["objects"]):
    yield "the %s %s" % (obj["color"], obj["shape"]), make_obj_answer(i)


def yield_scenes():
  i = 0
  while True:
    n_objects = np.random.randint(6)
    objects = [(random.choice(SHAPES), random.choice(COLORS))
               for _ in range(n_objects)]

    yield {
        "image_index": i,
        "objects": [{"shape": random.choice(SHAPES),
                     "color": random.choice(COLORS),
                     "material": random.choice(MATERIALS),
                     "size": random.choice(SIZES),
                     "rotation": 0.0,
                     "3d_coords": [0.0, 0.0, 0.0]}
                     for _ in range(n_objects)]
      }


    i += 1


def make_questions(scene):
  # Find questions with unique answers.
  answers = defaultdict(set)
  for utt, ans in yield_questions(scene):
    answers[utt].add(ans)

  questions = [(scene["image_index"], utt, next(iter(answers_i)))
               for utt, answers_i in answers.items()
               if len(answers_i) == 1]
  return questions


def main(args):
  scenes = []
  questions = []

  scene_iter = yield_scenes()
  while len(questions) < args.limit:
    scene = next(scene_iter)

    scenes.append(scene)
    questions.extend(make_questions(scene))

  questions = questions[:args.limit]

  out_questions = {
      "info": {"split": "simple"},
      "questions": [
        {
          "question_index": i,
          "image_index": image_index,
          "answer": dict(answer) if isinstance(answer, tuple) else answer,
          "question": question,
        } for i, (image_index, question, answer) in enumerate(questions)
        ]
      }

  out_scenes = {
      "info": {"split": "simple"},
      "scenes": scenes
      }

  with open(args.questions_path, "w") as out_f:
    json.dump(out_questions, out_f)
  with open(args.scenes_path, "w") as out_f:
    json.dump(out_scenes, out_f)


if __name__ == '__main__':
  p = ArgumentParser()

  p.add_argument("questions_path")
  p.add_argument("scenes_path")
  p.add_argument("--limit", type=int, default=10)

  main(p.parse_args())
