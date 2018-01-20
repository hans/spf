"""
Using a CLEVR scene dataset, build a dataset of simple reference-game questions.
"""

from argparse import ArgumentParser
from collections import defaultdict
import itertools
import json
import random


PROPERTY_TYPES = frozenset(["material", "color", "size", "shape"])


def make_obj_answer(idx):
  return (("type", "object"), ("index", idx))


def yield_base_questions(scene):
  for i, obj in enumerate(scene["objects"]):
    yield "the %s" % obj["shape"], make_obj_answer(i)

def yield_1adj_questions(scene):
  for i, obj in enumerate(scene["objects"]):
    for prop in PROPERTY_TYPES - set(["shape"]):
      for base_noun in [obj["shape"], "object"]:
        yield "the %s %s" % (obj[prop], base_noun), make_obj_answer(i)

def yield_attr_questions(scene):
  for i, obj in enumerate(scene["objects"]):
    for query_prop in ["color", "material", "size"]:
      for mod_prop in PROPERTY_TYPES - set([query_prop, "shape"]):
        for base_noun in [obj["shape"], "object"]:
          yield "what %s is the %s %s" % (query_prop, obj[mod_prop], base_noun), \
              obj[query_prop]

QUESTION_FNS = [yield_base_questions, yield_1adj_questions, yield_attr_questions]
QUESTION_FNS = {fn.__name__: fn for fn in QUESTION_FNS}


def make_questions(scene, question_fns):
  """
  Given a CLEVR scene, generate simple questions.
  """

  # Find questions with unique answers.
  answers = defaultdict(set)
  for question_fn in question_fns:
    for utt, ans in question_fn(scene):
      answers[utt].add(ans)

  # Only yield questions which have a unique referent.
  questions = [(scene["image_index"], utt, next(iter(answers_i)))
               for utt, answers_i in answers.items()
               if len(answers_i) == 1]
  return questions


def main(args):
  question_fns = args.question or ["yield_1adj_questions"]
  question_fns = [QUESTION_FNS[fn] for fn in question_fns]

  with open(args.scenes_path, "r") as scenes_f:
    scene_data = json.load(scenes_f)
  scenes = scene_data["scenes"]

  questions = list(itertools.chain.from_iterable(
    (make_questions(scene, question_fns)) for scene in scenes))
  random.shuffle(questions)

  enumerator = range(args.limit) if args.limit > 0 else itertools.count()

  out_data = {
      "info": {"split": "simple"},
      "questions": [
        {
          "question_index": i,
          "image_index": image_index,
          "answer": dict(answer) if isinstance(answer, tuple) else answer,
          "question": question,
        } for i, (image_index, question, answer) in zip(enumerator, questions)
        ]
      }

  with open(args.out_path, "w") as out_f:
    json.dump(out_data, out_f)


if __name__ == '__main__':
  p = ArgumentParser()
  p.add_argument("scenes_path")
  p.add_argument("out_path")
  p.add_argument("--limit", default=0, type=int)
  p.add_argument("--question", choices=QUESTION_FNS.keys(), action="append")

  main(p.parse_args())
