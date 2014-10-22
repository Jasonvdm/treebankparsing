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
        double[][][] score = new double[sentence.size()][sentence.size()][nonterms.size()];
        Triplet<Integer, String, String> back[][][] = new Triplet[sentence.size()][sentence.size()][nonterms.size()];

        for (int i=0; i < sentence.size(); i++){
            for (String a:nonterms){
                String word = sentence.get(i);
                score[i][i][nontermIndices.get(a)] = lexicon.scoreTagging(word,a);
                if(Double.isNaN(score[i][i][nontermIndices.get(a)])){
                    score[i][i][nontermIndices.get(a)] = 0;
                }
                else { 

                    back[i][i][nontermIndices.get(a)] = new Triplet<Integer, String, String>(-2,a, word);
                }
            }
            addUnaryRule(score, back, i, i);
        }

        for (int span=2; span<=sentence.size(); span++){
            for (int begin=0; begin <= sentence.size() - span; begin++){
                int end = begin + span - 1;
                for (int split = begin; split < end; split++){

                    for (String nterm:nonterms){
                        ArrayList<Grammar.BinaryRule> masterRules = new ArrayList<Grammar.BinaryRule>();
                        masterRules.addAll(grammar.getBinaryRulesByLeftChild(nterm));
                        masterRules.addAll(grammar.getBinaryRulesByRightChild(nterm));
                        for (Grammar.BinaryRule rule : masterRules){
                            String a = rule.getParent();
                            String b = rule.getLeftChild();
                            String c = rule.getRightChild();

                            int indexA = nontermIndices.get(a);
                            int indexB = nontermIndices.get(b);
                            int indexC = nontermIndices.get(c);

                            double prob = score[begin][split][indexB] * score[split+1][end][indexC] * rule.getScore();
                            if (prob > score[begin][end][indexA]) {
                                score[begin][end][indexA] = prob;
                                back[begin][end][indexA] = new Triplet<Integer, String, String>(split, b, c);
                            }
                        }
                    }
                    addUnaryRule(score, back, begin, end);
                }
            }
        }

        
        Tree<String> s = buildTree(0,sentence.size()-1,"S",score, back, sentence);
        ArrayList<Tree<String>> children = new ArrayList<Tree<String>>();
        children.add(s);
        return new Tree<String>("ROOT", children);
    }

    // public Tree<String> buildTree(double[][][] score, Triplet<Integer, String, String>[][][] back){
    //     return constructTreeNode(i,j,"S", score, back);
    // }

    public Tree<String> buildTree(int i,int j,String label, double[][][] score, Triplet<Integer, String, String>[][][] back, List<String> sentence){
        Triplet<Integer, String, String> rule = back[i][j][nontermIndices.get(label)];
        if(rule.getFirst() == -2){
            Tree<String> leaf = new Tree<String>(sentence.get(i));
            ArrayList<Tree<String>> children = new ArrayList<Tree<String>>();
            children.add(leaf);
            Tree<String> preterminal = new Tree<String>(label, children);
            return preterminal;
        }
        else if(rule.getFirst() == -1){
            String rightLabel = rule.getThird();
            Tree<String> rChild = buildTree(i,j,rightLabel,score,back,sentence);
            ArrayList<Tree<String>> children = new ArrayList<Tree<String>>();
            children.add(rChild);
            Tree<String> root = new Tree<String>(label, children);
            return root; 
        }
        else{
            int split = rule.getFirst();
            String leftLabel = rule.getSecond();
            String rightLabel = rule.getThird();
            Tree<String> lChild = buildTree(i,split,leftLabel,score,back, sentence);
            Tree<String> rChild = buildTree(split+1,j,rightLabel,score,back, sentence);
            ArrayList<Tree<String>> children = new ArrayList<Tree<String>>();
            children.add(lChild);
            children.add(rChild);
            Tree<String> root = new Tree<String>(label, children);
            return root; 
        }
    }

    private void addUnaryRule(double[][][] score, Triplet[][][] back, int begin, int end) 
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
                            double prob = rule.getScore()*score[begin][end][indexB];
                            if (prob > score[begin][end][indexA]) {
                                score[begin][end][indexA] = prob;
                                back[begin][end][indexA] = new Triplet<Integer, String, String>(-1, a, b);
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
