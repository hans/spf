from collections import namedtuple
from functools import reduce
from pathlib import Path
import re
import sys


SPLIT = "train"
DATA_PATH = Path("/om/data/public/jgauthie/CLEVR_v1.0/questions_functionalized.%s.txt" % SPLIT)

PREDS_PATH = Path(__file__).parents[0] / "resources" / "geo.preds.ont"
with PREDS_PATH.open("r") as f:
  pred_types = dict([tuple(line.strip().split(":")) for line in f if ":" in line])

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
      return self.name

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


def process_sexpr(line):
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


if __name__ == "__main__":
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
