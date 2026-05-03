package de.monticore.tf;

import automata._ast.ASTTransition;
import com.google.common.base.Objects;
import de.monticore.ast.ASTNode;
import de.monticore.generating.templateengine.GlobalExtensionManagement;
import de.monticore.generating.templateengine.reporting.Reporting;
import de.monticore.tf.runtime.FastLookupList;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Add HWC that uses hashes on identifiers to restrict the search tree
 **/
@SuppressWarnings("unused")
public class AUTUnifyTransitionsMulti extends AUTUnifyTransitionsMultiTOP {

  public AUTUnifyTransitionsMulti(GlobalExtensionManagement glex, ASTNode astNode) {
    super(glex, astNode);
  }

  @Override
  protected void setupReporting() {
    // Disable reporting (performance issues in 7.9.0-SNAPSHOT
  }

  // Given the transition_1(to,from) == transition_2(to,from) constraints:
  // Define the hash function for (to,from)
  protected int getHashOf_transition_1_transition_2(ASTTransition node) {
    // reduce the memory overhead of Objects.hashCode(node.getFrom(), node.getTo())
    int result = 1;
    result = 31 * result + node.getFrom().hashCode();
    result = 31 * result + node.getTo().hashCode();
    return result;
  }

  @Override
  public void initializeFastLookupList() {
    super.initializeFastLookupList();

    /*
    Given the following pattern:
    ---
    automaton $_ {
      $A - $y > $B;
      $A - $x > $B;
    }
    ---
    We know that:
      * T1 and T2 share from:$A and to:$B  -> transition_1 hash (from,to)
      *                                    -> transition_2 hash (from,to)
      * T1 has input:$y                    -> transition_1 hash (input)
      * T2 has input:$x                    -> transition_2 hash (input)

      Right now we only support one hash function.
      Q1: Could we somehow support multiple hashes, i.e. (from,to) and (input) - in case input was fix?)
      Q2: Can we share the hash functions of object-types with the same fields?
      Q3: Can we hash more than just identifiers/names?
     */

    // As we know that transition_1 is the final search-tree item: only add the code for this object :)

    // Override the current transition_1 candidates with a FastLookupList that uses hash-bags to reduce the search tree
    this.transition_1_candidates_temp = new FastHashedLookupMap((FastLookupList<ASTNode>) this.transition_1_candidates_temp) {
      @Override
      public void reset() {
        super.reset();

        // Everytime the possible candidates left are reset to be all candidates,
        // we reduce all candidates (IFF transition_2 is present) to only be hash-matching ones
        if (transition_2_cand != null) {
          // In case we could use a hash: Only use the candidates list matching that hash
          setFromHash(transition_2_cand);
        }
      }

      @Override
      int getHash(ASTNode e) {
        return getHashOf_transition_1_transition_2((ASTTransition) e);
      }
    };


    // TODO: Can we somehow have the transition_2_candidates == transition_1_candidates and share a hash-map? (Q2 above)
  }

  // The following class can/should probably be moved to the runtime

  /**
   * A FastLookup list that by default delegates everything to its fallback of all candidates.
   * by calling {@link #setFromHash(ASTNode)}, this list instead delegates to the reduced search space (by calculating a hash from the given element)
   * Calling {@link #reset()} also changes the delegation back to the fallback (of all candidates)
   */
  static abstract class FastHashedLookupMap extends FastLookupList<ASTNode> {
    protected final FastLookupList<ASTNode> all_candidates; // fallback if no hash could be computed
    protected FastLookupList<ASTNode> currentHashed = null; // basically what the _temp list looks at
    protected Map<Integer, FastLookupList<ASTNode>> all_candidates_hashed;

    public FastHashedLookupMap(FastLookupList<ASTNode> all_candidates) {
      super(Collections.emptyList());
      this.all_candidates = all_candidates;

      this.recalculateHash();
    }

    /**
     * Provide a hash function for this hashed-lookup
     *
     * @param e the node to compute the hash from
     * @return the hash
     */
    abstract int getHash(ASTNode e);

    protected void setFromHash(ASTNode hashSource) {
      int hash = getHash(hashSource);
      this.currentHashed = this.all_candidates_hashed.get(hash);
    }

    protected void recalculateHash() {
      // use parallel streams
      Map<Integer, List<ASTNode>> hashMap = this.all_candidates.parallelStream()
          .collect(Collectors.groupingByConcurrent(this::getHash, Collectors.toList()));
      
      // and store everything as FastLookupLists
      all_candidates_hashed = hashMap.entrySet().stream().
              collect(Collectors.toMap(Map.Entry::getKey, e -> new FastLookupList<>(e.getValue())));

    }

    @Override
    public void reset() {
      currentHashed = null;
      all_candidates.reset();
    }

    // All other methods are adapted to the currentHashed view or all_candidates-fallback

    @Override
    public ASTNode get(int index) {
      if (currentHashed != null)
        return currentHashed.get(index);
      return all_candidates.get(index);
    }

    @Override
    public ASTNode remove(int index) {
      if (currentHashed != null)
        return currentHashed.remove(index);
      return all_candidates.remove(index);
    }

    @Override
    public int size() {
      if (currentHashed != null)
        return currentHashed.size();
      return all_candidates.size();
    }

    @Override
    public boolean isEmpty() {
      if (currentHashed != null)
        return currentHashed.isEmpty();
      return all_candidates.isEmpty();
    }

    @Override
    public FastLookupList<ASTNode> matchCopy() {
      if (currentHashed != null)
        return currentHashed.matchCopy();
      return all_candidates.matchCopy();
    }
  }

}
