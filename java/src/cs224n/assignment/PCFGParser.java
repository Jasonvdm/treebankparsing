package cs224n.assignment;

import cs224n.ling.Tree;
import java.util.*;
import cs224n.util.PriorityQueue;
import cs224n.util.Pair;


/**
 * The CKY PCFG Parser you will implement.
 */
public class PCFGParser implements Parser {
    private Grammar grammar;
    private Lexicon lexicon;
    private HashMap<String, Integer> nontermIndices = new HashMap<String, Integer>();

    public void train(List<Tree<String>> trainTrees) {
        for (int i=0;i < trainTrees.size(); i++) {
            Tree<String> tree = TreeAnnotations.annotateTree(trainTrees.get(i));
            trainTrees.set(i, tree);
        }
        lexicon = new Lexicon(trainTrees);
        grammar = new Grammar(trainTrees);
        System.out.println("#########" + grammar.toString());
    }

    public Tree<String> getBestParse(List<String> sentence) {
        Set<String>nonterms = getAllNonTerms();
        double[][][] score = new double[sentence.size() + 1][sentence.size() + 1][nonterms.size()];
        Pair back[][][] = new Pair[sentence.size() + 1][sentence.size() + 1][nonterms.size()];

        for (int i=0; i < sentence.size(); i++){
            for (String a:nonterms){
                score[i][i+1][nontermIndices.get(a)] = scoreTagging(sentence.get(i),a);
            }

            boolean added = true;

            while(added) {
                added = false;
                for (String b:nonterms){
                    for (Grammar.UnaryRule rule : List<UnaryRule> Grammar.getUnaryRulesByChild(a)) {
                        String a = rule.getParent();

                        int indexA = nontermIndices.get(a);
                        int indexB = nontermIndices.get(b);

                        if (score[i][i+1][indexB] > 0){
                            double prob = rule.getScore();
                            if (prob > score[i][i+1][indexA]) {
                                score[i][i+1][indexA] = prob;
                                back[i][i+1][indexA] = b;
                            }
                        }
                    } 
                }
            }
        }
        return null;
    }

    private Set<String> getAllNonTerms(){
        HashSet<String> nonterms = new HashSet<String>();
        for(String s:lexicon.getAllTags()){
            nonterms.add(s);
        }
        PriorityQueue<String> pqueue = new PriorityQueue<String>();
        for(String s: nonterms){
            pqueue.add(s,1);
        }
        while(!pqueue.isEmpty()){
            String symbol = pqueue.next();
            for (Grammar.BinaryRule s:grammar.getBinaryRulesByLeftChild(symbol)){
                String parent = s.getParent();
                if(!nonterms.contains(parent)){
                    nonterms.add(parent);
                    pqueue.add(parent,1);
                }
            }
            for (Grammar.BinaryRule s:grammar.getBinaryRulesByRightChild(symbol)){
                String parent = s.getParent();
                if(!nonterms.contains(parent)){
                    nonterms.add(parent);
                    pqueue.add(parent,1);
                }
            }
            for (Grammar.UnaryRule s:grammar.getUnaryRulesByChild(symbol)){
                String parent = s.getParent();
                if(!nonterms.contains(parent)){
                    nonterms.add(parent);
                    pqueue.add(parent,1);
                }
            }
        }
        int index = -1;
        for(String a: nonterms){
            index++;
            nontermIndices[a] = index;
        }
        return nonterms;
    }
}
