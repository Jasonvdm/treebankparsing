package cs224n.assignment;

import cs224n.ling.Tree;
import java.util.*;
import cs224n.util.PriorityQueue;
import cs224n.util.Triplet;


/**
 * The CKY PCFG Parser you will implement.
 */
public class PCFGParser implements Parser {
    private Grammar grammar;
    private Lexicon lexicon;
    private HashMap<String, Integer> nontermIndices = new HashMap<String, Integer>();
    Set<String>nonterms;

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
        nonterms = getAllNonTerms();
        double[][][] score = new double[sentence.size() + 1][sentence.size() + 1][nonterms.size()];
        Triplet back[][][] = new Triplet[sentence.size() + 1][sentence.size() + 1][nonterms.size()];

        for (int i=0; i < sentence.size(); i++){
            for (String a:nonterms){
                score[i][i+1][nontermIndices.get(a)] = lexicon.scoreTagging(sentence.get(i),a);
                back[i][i+1][nontermIndices.get(a)] = new Triplet(-2,a, sentence.get(i));
            }
            addUnaryRule(score, back, i, i+1);
        }

        for (int span=2; span<sentence.size(); span++){
            for (int begin=0; begin < sentence.size() - span; begin++){
                int end = begin + span;

                for (int split = begin+1; split < end-1; split++){
                    for (String nterm:nonterms){
                        List<Grammar.BinaryRule> masterRules = grammar.getBinaryRulesByLeftChild(nterm);
                        masterRules.addAll(grammar.getBinaryRulesByRightChild(nterm)); 

                        for (Grammar.BinaryRule rule : masterRules){
                            String a = rule.getParent();
                            String b = rule.getLeftChild();
                            String c = rule.getRightChild();

                            int indexA = nontermIndices.get(a);
                            int indexB = nontermIndices.get(b);
                            int indexC = nontermIndices.get(c);

                            double prob = score[begin][split][indexB] * score[split][end][indexC] * rule.getScore();
                            if (prob > score[begin][end][indexA]) {
                                score[begin][end][indexA] = prob;
                                back[begin][end][indexA] = new Triplet(split, b, c);
                            }
                        }
                    }
                    addUnaryRule(score, back, begin, end);
                }
            }
        }


        return null;
    }

    private void addUnaryRule(double[][][] score, Triplet back[][][], int begin, int end) 
    {
        boolean added = true;

            while(added) {
                added = false;
                for (String b:nonterms){
                    for (Grammar.UnaryRule rule : grammar.getUnaryRulesByChild(b)) {
                        String a = rule.getParent();

                        int indexA = nontermIndices.get(a);
                        int indexB = nontermIndices.get(b);

                        if (score[begin][end][indexB] > 0){
                            double prob = rule.getScore();
                            if (prob > score[begin][end][indexA]) {
                                score[begin][end][indexA] = prob;
                                back[begin][end][indexA] = new Triplet(-1, a, b);
                                added = true;
                            }
                        }
                    } 
                }
            }
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
            nontermIndices.put(a, index);
        }
        return nonterms;
    }
}
