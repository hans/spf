"""
Process programs in a supervised CLEVR question dataset.

Modifications:

  1. Reorder filter_* calls so that their order matches the sentence surface order.
  2. Factor filter_*, query_*, equal_* calls into generic filter, query, equal which
     attend to properties according to a new abstract argument.

"""

from argparse import ArgumentParser
from collections import namedtuple
from functools import reduce
import json
from pathlib import Path
import re
import sys


# Type used to indicate attribute type, e.g. shape:a, size:a, etc.
ABSTRACT_ATTRIBUTE_TYPE = "a"

token_split_re = re.compile(r"([()]|\s+)")
token_re = re.compile(r"(?<=[(\s])([\w_]+)(?=[)\s])")


class Node(object):
  """sexpr node"""

  def __init__(self, name, children=None, type=None):
    self.name = name
    self.children = children if children is not None else []
    self.type = type

  def visit(self, fn):
    """pre-order tree traversal with in-place operations"""
    fn(self)
    for child in self.children:
      child.visit(fn)

  @property
  def _typed_name(self):
    if self.type is None:
      return self.name
    else:
      return "%s:%s" % (self.name, self.type)

  def __str__(self):
    if self.children:
      return "(%s %s)" % (self._typed_name, " ".join(str(child) for child in self.children))
    else:
      return self._typed_name

  __repr__ = __str__


def parse_sexpr(sexpr):
  # parse sexpr into a tree
  stack, cur_node = [], None
  for token in token_split_re.split(sexpr):
    if token == '(':
      if cur_node is not None:
        stack.append(cur_node)
      cur_node = None
    elif token == ')':
      old_cur = cur_node
      try:
        cur_node = stack.pop()
      except IndexError:
        cur_node = None
      else:
        cur_node.children.append(old_cur)
    else:
      token = token.strip()
      if not token:
        continue

      if cur_node is None:
        # reading a function call
        cur_node = Node(token)
      else:
        cur_node.children.append(Node(token))

  assert not stack, 'Stack should be empty'
  assert old_cur is not None, 'expected accumulator to be non-null. unbalanced parens?'

  return old_cur


def process_sexpr(line, pred_types, factor_attrs):
  tree = parse_sexpr(line)

  # reverse sequences of filter_* calls
  def inner(node, filter_chain, filter_chain_parent, parent=None):
    if node.name.startswith("filter"):
      idx_in_parent = None
      if parent is not None:
        idx_in_parent = parent.children.index(node)

      filter_chain.append((node.name, node.children[1], idx_in_parent))
      filter_chain_parent = filter_chain_parent or parent

      inner(node.children[0], filter_chain, filter_chain_parent, node)
      return
    elif filter_chain:
      # Build a new filter subtree with reversed vertical order.
      def reduce_fn(child_node, next_filter):
        filter_type, filter_arg, _ = next_filter
        return Node(filter_type, [child_node, filter_arg])
      filter_subtree = reduce(reduce_fn, filter_chain, node)

      orig_idx = filter_chain[0][2]
      filter_chain_parent.children[orig_idx] = filter_subtree

      filter_chain_parent = None
      filter_chain = []

    for child in node.children:
      inner(child, filter_chain[:], filter_chain_parent, node)

  inner(tree, [], None, None)

  # per-node updates
  def visitor(node):
    if node.name == "exist_":
      node.name = "exists"
    elif factor_attrs and node.name.startswith(("filter", "equal", "query", "same")):
      fn, attribute = node.name.split("_")
      node.name = fn
      if fn != "equal":
        node.children.insert(0, Node(attribute, type=ABSTRACT_ATTRIBUTE_TYPE))

    # Add types.
    node.type = pred_types[node.name]

  tree.visit(visitor)

  return str(tree)


def process_sentence(line):
  return line.lower().replace("?", "").replace(";", " SEMI")


def finalize(sentence, sexpr):
  print(sentence)
  print(sexpr)
  print()


def convert_program_to_sexpr(program_struct):
  """
  Convert a CLEVR program struct into a sexpr string.
  """
  def inner(p):
    if p["function"] == "scene":
      return "scene"
    ret = "(%s %s" % ("exist_" if p["function"] == "exist" else p["function"],
                      " ".join(inner(program_struct[x]) for x in p["inputs"]))

    if p["value_inputs"]:
      ret += " " + " ".join(map(str, p["value_inputs"]))
    ret += ")"
    return ret

  return inner(program_struct[-1])


def main(args):
  with open(args.preds_path, "r") as f:
    pred_types = dict([tuple(line.strip().split(":")) for line in f if ":" in line])

  with open(args.data_path, "r") as data_f, open(args.out_path, "w") as out_f:
    data = json.load(data_f)
    new_data = {"info": data["info"], "questions": []}

    count = 0
    for i, question in enumerate(data["questions"]):
      sentence = process_sentence(question["question"])

      if args.max_len > 0 and len(sentence.strip().split()) > args.max_len:
        continue

      question["question"] = sentence

      # Convert program into a sexpr
      sexpr = process_sexpr(convert_program_to_sexpr(question["program"]), pred_types,
                            args.factor_attrs)

      del question["program"]
      question["program_sexpr"] = sexpr

      new_data["questions"].append(question)
      count += 1

      if args.output_format == "plain":
        out_f.write(question["question"])
        out_f.write("\n")
        out_f.write(question["program_sexpr"])
        out_f.write("\n\n")

      if args.limit > 0 and count >= args.limit:
        break

    if args.output_format == "json":
      json.dump(new_data, out_f)


if __name__ == '__main__':
  p = ArgumentParser()
  p.add_argument("data_path")
  p.add_argument("out_path")
  p.add_argument("--preds_path", default=str(Path(__file__).parents[0] / "resources" / "geo.preds.ont"))
  p.add_argument("--limit", default=5, type=int)
  p.add_argument("--max_len", default=0, type=int)
  p.add_argument("--factor_attrs", default=False, action="store_true")
  p.add_argument("--output_format", choices=["json", "plain"], default="json")

  main(p.parse_args())

