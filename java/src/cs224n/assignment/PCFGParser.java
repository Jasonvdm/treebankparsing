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
    Set<String>nonterms;

    public void train(List<Tree<String>> trainTrees) {
        for (int i=0;i < trainTrees.size(); i++) {
            Tree<String> tree = TreeAnnotations.annotateTree(trainTrees.get(i));
            trainTrees.set(i, tree);
        }
        lexicon = new Lexicon(trainTrees);
        grammar = new Grammar(trainTrees);
    }

    public Tree<String> getBestParse(List<String> sentence) {
        nonterms = getAllNonTerms();
        HashMap<String,Double>[][] score = new HashMap[sentence.size()][sentence.size()];
        HashMap<String,Triplet<Integer, String, String>>[][] back = new HashMap[sentence.size()][sentence.size()];
        for(int i=0; i < sentence.size(); i++){
            for(int j = 0; j < sentence.size(); j++){
                score[i][j] = new HashMap<String,Double>();
                back[i][j] = new HashMap<String,Triplet<Integer, String, String>>();
            }
        }
        for (int i=0; i < sentence.size(); i++){
            for (String a:nonterms){
                String word = sentence.get(i);
                if(!Double.isNaN(lexicon.scoreTagging(word,a))){
                    score[i][i].put(a,lexicon.scoreTagging(word,a));
                    back[i][i].put(a, new Triplet<Integer, String, String>(-2,a, word));
                }
            }
            addUnaryRule(score, back, i, i);
        }

        for (int span=2; span<=sentence.size(); span++){
            for (int begin=0; begin <= sentence.size() - span; begin++){
                int end = begin + span - 1;
                for (int split = begin; split < end; split++){
                    ArrayList<String>rightTerms = new ArrayList<String>();
                    for(String right:score[split+1][end].keySet()){
                        rightTerms.add(right);
                    }
                    for (String nterm:rightTerms){ 
                        for (Grammar.BinaryRule rule : grammar.getBinaryRulesByRightChild(nterm)){
                            String a = rule.getParent();
                            String b = rule.getLeftChild();
                            String c = rule.getRightChild();

                            if(score[begin][split].get(b) == null || score[begin][split].get(b) == 0) continue;

                            double prob = score[begin][split].get(b) * score[split+1][end].get(c) * rule.getScore();
                            if (score[begin][end].get(a) == null || prob > score[begin][end].get(a)) {
                                score[begin][end].put(a, prob);
                                back[begin][end].put(a, new Triplet<Integer, String, String>(split, b, c));
                            }
                        }
                    }
                    addUnaryRule(score, back, begin, end);
                }
            }
        }
        return TreeAnnotations.unAnnotateTree(buildTree(0,sentence.size()-1,"ROOT",back));
    }

    // public Tree<String> buildTree(double[][][] score, Triplet<Integer, String, String>[][][] back){
    //     return constructTreeNode(i,j,"S", score, back);
    // }

    public Tree<String> buildTree(int i,int j,String label, HashMap<String,Triplet<Integer, String, String>>[][] back){
        Triplet<Integer, String, String> rule = back[i][j].get(label);
        if(rule.getFirst() == -2){
            Tree<String> leaf = new Tree<String>(rule.getThird());
            ArrayList<Tree<String>> children = new ArrayList<Tree<String>>();
            children.add(leaf);
            Tree<String> preterminal = new Tree<String>(label, children);
            return preterminal;
        }
        else if(rule.getFirst() == -1){
            String rightLabel = rule.getThird();
            Tree<String> rChild = buildTree(i,j,rightLabel,back);
            ArrayList<Tree<String>> children = new ArrayList<Tree<String>>();
            children.add(rChild);
            Tree<String> root = new Tree<String>(label, children);
            return root; 
        }
        else{
            int split = rule.getFirst();
            String leftLabel = rule.getSecond();
            String rightLabel = rule.getThird();
            Tree<String> lChild = buildTree(i,split,leftLabel,back);
            Tree<String> rChild = buildTree(split+1,j,rightLabel,back);
            ArrayList<Tree<String>> children = new ArrayList<Tree<String>>();
            children.add(lChild);
            children.add(rChild);
            Tree<String> root = new Tree<String>(label, children);
            return root; 
        }
    }

    private void addUnaryRule(HashMap<String,Double>[][] score, HashMap<String,Triplet<Integer, String, String>>[][] back, int begin, int end) 
    {
        boolean added = true;

            while(added) {
                added = false;
                ArrayList<String> terms = new ArrayList<String>();
                for(String b: score[begin][end].keySet()){
                    terms.add(b);
                }
                for (String b:terms){
                    for (Grammar.UnaryRule rule : grammar.getUnaryRulesByChild(b)) {
                        String a = rule.getParent();

                        if (score[begin][end].get(b) > 0){
                            double prob = rule.getScore()*score[begin][end].get(b);
                            if (score[begin][end].get(a) == null || prob > score[begin][end].get(a)) {
                                score[begin][end].put(a, prob);
                                back[begin][end].put(a, new Triplet<Integer, String, String>(-1, a, b));
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
        return nonterms;
    }
}
