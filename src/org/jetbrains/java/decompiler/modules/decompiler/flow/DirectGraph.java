// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.decompiler.modules.decompiler.flow;

import org.jetbrains.java.decompiler.modules.decompiler.exps.Exprent;
import org.jetbrains.java.decompiler.modules.decompiler.stats.DummyExitStatement;
import org.jetbrains.java.decompiler.util.collections.VBStyleCollection;

import java.util.*;


public class DirectGraph {

  public final VBStyleCollection<DirectNode, String> nodes = new VBStyleCollection<>();

  public final List<DirectNode> extraNodes = new ArrayList<>();

  public DirectNode first;

  // negative if branches (recorded for handling of && and ||)
  public final HashMap<String, String> mapNegIfBranch = new HashMap<>();

  // nodes, that are exception exits of a finally block with monitor variable
  public final HashMap<String, String> mapFinallyMonitorExceptionPathExits = new HashMap<>();


  public void sortReversePostOrder() {
    Deque<DirectNode> res = new ArrayDeque<>();
    addToReversePostOrderListIterative(this.first, res);

    // Include unreachable nodes in the graph structure
    if (res.size() != this.nodes.size()) {
      Set<DirectNode> a = new HashSet<>(this.nodes);
      Set<DirectNode> b = new HashSet<>(res);
      a.removeAll(b);
      a.removeIf(s -> s.statement instanceof DummyExitStatement);

      // FIXME: addFirst is bad! this will mess with the graph structure! but it's needed to properly handle unreachable blocks in SSA, by making these blocks be processed first!
      for (DirectNode nd : a) {
        this.extraNodes.add(nd);
        res.addFirst(nd);
      }
    }

    this.nodes.clear();

    for (DirectNode node : res) {
      this.nodes.addWithKey(node, node.id);
    }
  }

  private static void addToReversePostOrderListIterative(DirectNode root, Deque<? super DirectNode> lst) {

    LinkedList<DirectNode> stackNode = new LinkedList<>();
    LinkedList<Integer> stackIndex = new LinkedList<>();

    HashSet<DirectNode> setVisited = new HashSet<>();

    stackNode.add(root);
    stackIndex.add(0);

    while (!stackNode.isEmpty()) {

      DirectNode node = stackNode.getLast();
      int index = stackIndex.removeLast();

      setVisited.add(node);

      for (; index < node.getSuccessors(DirectEdgeType.REGULAR).size(); index++) {
        DirectNode succ = node.getSuccessors(DirectEdgeType.REGULAR).get(index).getDestination();

        if (!setVisited.contains(succ)) {
          stackIndex.add(index + 1);

          stackNode.add(succ);
          stackIndex.add(0);

          break;
        }
      }

      if (index == node.getSuccessors(DirectEdgeType.REGULAR).size()) {
        lst.addFirst(node);

        stackNode.removeLast();
      }
    }
  }


  public boolean iterateExprents(ExprentIterator iter) {

    LinkedList<DirectNode> stack = new LinkedList<>();
    stack.add(first);

    HashSet<DirectNode> setVisited = new HashSet<>();

    while (!stack.isEmpty()) {

      DirectNode node = stack.removeFirst();

      if (setVisited.contains(node)) {
        continue;
      }
      setVisited.add(node);

      for (int i = 0; i < node.exprents.size(); i++) {
        int res = iter.processExprent(node.exprents.get(i));

        if (res == 1) {
          return false;
        }

        if (res == 2) {
          node.exprents.remove(i);
          i--;
        }
      }

      for (DirectEdge suc : node.getSuccessors(DirectEdgeType.REGULAR)) {
        stack.add(suc.getDestination());
      }
    }

    return true;
  }

  public boolean iterateExprentsDeep(ExprentIterator itr) {
    return iterateExprents(exprent -> {
      List<Exprent> lst = exprent.getAllExprents(true);
      lst.add(exprent);

      for (Exprent expr : lst) {
        int res = itr.processExprent(expr);
        if (res == 1 || res == 2) {
          return res;
        }
      }
      return 0;
    });
  }

  public interface ExprentIterator {
    // 0 - success, do nothing
    // 1 - cancel iteration
    // 2 - success, delete exprent
    int processExprent(Exprent exprent);
  }
}
