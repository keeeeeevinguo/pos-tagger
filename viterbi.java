import java.io.*;
import java.util.*;
/**
 * Speech tagging method using viterbi algorithm and hidden markov model
 *
 * @author Kevin Guo, Dartmouth CS 10, F22
 */
public class viterbi {

    //store tags and word frequency
    private Map<String, HashMap<String,Double>> stor;
    //store transitions and frequency
    private Map<String, HashMap<String, Double>> trans;
    //store tags and word probability
    private Map<String, HashMap<String,Double>> storLog;
    //store transitions and probabilty
    private Map<String, HashMap<String, Double>> transLog;

    public viterbi(){
        stor = new HashMap<>();
        trans = new HashMap<>();
        storLog=new HashMap<>();
        transLog=new HashMap<>();

        trans.put("#", new HashMap<>());
        trans.get("#").put("normal",0.0);

    }

    //process input as two maps one line at a time - one for transitions and one for words/tags
    public void reader(String tags, String sentence){
        String[]words=sentence.split(" ");
        String[]t=tags.split(" ");
        double temp;
        //account for edge case of beginning of sentence
        if (trans.get("#").containsKey(t[0])) {
            temp = trans.get("#").get(t[0]);
        } else {
            temp = 0.0;
        }
        double tempTot=trans.get("#").get("normal");
        trans.get("#").put("normal",tempTot+1);
        trans.get("#").put(t[0],temp+1);
        for (int i = 0; i < t.length; i++) {
            //Count frequency of use of each word as each type of tag
            if (stor.containsKey(t[i])) {
                if (stor.get(t[i]).containsKey(words[i])) {
                    temp = stor.get(t[i]).get(words[i]);

                } else {
                    temp = 0.0;
                }
                stor.get(t[i]).put(words[i], temp + 1);
                Double nor=stor.get(t[i]).get("normal");
                stor.get(t[i]).put("normal",nor+1);
            } else {
                stor.put(t[i], new HashMap<>());
                stor.get(t[i]).put(words[i], 1.0);
                stor.get(t[i]).put("normal",1.0);
            }
            //Count frequency of each transition
            if(i<t.length-1){
                if(!trans.containsKey(t[i])) {
                    trans.put(t[i], new HashMap<>());
                    trans.get(t[i]).put("normal",0.0);
                }
                if (trans.get(t[i]).containsKey(t[i + 1])) {
                    temp = trans.get(t[i]).get(t[i + 1]);
                } else {
                    temp = 0.0;
                }
                Double numT=trans.get(t[i]).get("normal");
                trans.get(t[i]).put("normal",numT+1);
                trans.get(t[i]).put(t[i+1],temp+1);
            }

        }
    }

    //create map of transitions and words/tags with ln(probability) value instead of frequency
    public void probabilify(Map<String, HashMap<String,Double>> stor,Map<String, HashMap<String, Double>> trans){
        //Convert each frequency to the natural log of its probability
        for(String i:stor.keySet()){
            double n=stor.get(i).get("normal");
            storLog.put(i,new HashMap<>());
            for(String z:stor.get(i).keySet()) {
                if (!z.equals("normal")) {
                    double x = stor.get(i).get(z);
                    double prob=Math.log(x/n);
                    storLog.get(i).put(z,prob);
                }
            }
        }
        //Convert each frequency to the natural log of its probability
        for(String i:trans.keySet()){
            double n=trans.get(i).get("normal");
            transLog.put(i,new HashMap<>());
            for(String z:trans.get(i).keySet()) {
                if (!z.equals("normal")) {
                    double x = trans.get(i).get(z);
                    double prob=Math.log(x/n);
                    transLog.get(i).put(z,prob);
                }
            }
        }
    }

    //create tag for input based on viterbi's algorithm
    public ArrayList<String> viterbi(String sentence) {
        ArrayList<Map<String, String>> path = new ArrayList<>(); //Track every tag-tag transition evaluated in viterbi
        Map<String, Double> scores = new HashMap<>(); //Keep track of scores for current depth of evaluation
        sentence=sentence.toLowerCase();
        sentence="# "+sentence;
        String[] words=sentence.split(" ");
        //Add initial score
        scores.put("#",0.0);
        Double score=0.0;
        for (int i = 0; i < words.length-1; i++) {
            path.add(new HashMap<>());
        }

        for (int j = 0; j < words.length-1; j++) {
            HashMap<String, Double> tempScores = new HashMap<>();
            for (String z: scores.keySet()) {
                if(transLog.containsKey(z)) {
                    for (String i : transLog.get(z).keySet()) {
                        if (!i.equals(words[j])) {
                            if (storLog.get(i).containsKey(words[j + 1])) {
                                score = scores.get(z) + storLog.get(i).get(words[j + 1]) + transLog.get(z).get(i);
                            } else {
                                score = scores.get(z) + -20 + transLog.get(z).get(i);
                            }
                            if (!tempScores.containsKey(i)) {
                                tempScores.put(i, score);
                                path.get(j).put(i, z);
                            } else if (tempScores.get(i) < score) {
                                tempScores.put(i, score);
                                path.get(j).put(i, z);
                            }
                        }
                    }
                }
            }
            scores=tempScores;
        }
        String best="";
        double max= -Double.MAX_VALUE;
        for (String i:scores.keySet()) {
            if(scores.get(i)>max){
                max=scores.get(i);
                best=i;
            }
        }
        ArrayList<String> x=new ArrayList<>();
        x.add(best);
        String curr=best;
        for (int i = path.size()-1; i>=1; i--) {
            curr=path.get(i).get(curr);
            x.add(0,curr);
        }
        return x;
    }

    //take user input sentence and return tags for sentence
    public void consoleTest() throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Input sentence");
        String sentence = reader.readLine();
        System.out.println(viterbi(sentence));
    }

    //take file input of sentences and correct tags, return tags determined through viterbi and accuracy of tags
    public void fileTest() throws IOException {
        BufferedReader inputSen = new BufferedReader(new FileReader("texts/brown-test-sentences.txt"));
        BufferedReader inputTag = new BufferedReader(new FileReader("texts/brown-test-tags.txt"));
        BufferedWriter writer=new BufferedWriter(new FileWriter("outputs/brown-test-viterbi-tags.txt"));
        try{
            String sentence;
            String tags;
            int correct=0;
            int wrong=0;
            while ((sentence = inputSen.readLine())!=null) {
                tags=inputTag.readLine();
                sentence=sentence.toLowerCase();
                ArrayList<String> testTag=viterbi(sentence);

                String[] correctTag=tags.split(" ");
                for (int i = 0; i < correctTag.length; i++) {
                    writer.write(testTag.get(i)+" ");
                    if(testTag.get(i).equals(correctTag[i])){
                        correct++;
                    }
                    else{
                        wrong++;
                    }
                }
                writer.newLine();
            }

            System.out.println("Correct tags: "+correct+", Wrong tags: "+wrong);
        }
        finally {

        }
    }

    //main method for testing
    public static void main(String[] args) throws IOException {
        BufferedReader input1 = new BufferedReader(new FileReader("texts/brown-train-sentences.txt"));
        BufferedReader input2 = new BufferedReader(new FileReader("texts/brown-train-tags.txt"));

        //Hard coded testing graph made using part 3 of PD7
        Map<String, HashMap<String,Double>> hardStor=new HashMap<>();
        Map<String, HashMap<String, Double>> hardTrans=new HashMap<>();
        hardStor.put("CNJ",new HashMap<>());
        hardStor.put("N",new HashMap<>());
        hardStor.put("NP",new HashMap<>());
        hardStor.put("V",new HashMap<>());

        hardStor.get("CNJ").put("and",0.0);
        hardStor.get("N").put("cat",-0.8754687374);
        hardStor.get("N").put("dog",-0.8754687374);
        hardStor.get("N").put("watch",-1.791759469);
        hardStor.get("NP").put("chase",0.0);
        hardStor.get("V").put("chase",-1.504077397);
        hardStor.get("V").put("get",-2.197224577);
        hardStor.get("V").put("watch",-0.4054651081);

        hardTrans.put("#",new HashMap<>());
        hardTrans.put("CNJ",new HashMap<>());
        hardTrans.put("N",new HashMap<>());
        hardTrans.put("NP",new HashMap<>());
        hardTrans.put("V",new HashMap<>());

        hardTrans.get("#").put("N",-0.3364722366);
        hardTrans.get("#").put("N",-1.252762968);
        hardTrans.get("CNJ").put("N",-1.098612289);
        hardTrans.get("CNJ").put("NP",-1.098612289);
        hardTrans.get("CNJ").put("V",-1.098612289);
        hardTrans.get("N").put("CNJ",-1.386294361);
        hardTrans.get("N").put("V",-0.2876820725);
        hardTrans.get("NP").put("V",0.0);
        hardTrans.get("V").put("CNJ",-2.197224577);
        hardTrans.get("N").put("CNJ",-0.4054651081);
        hardTrans.get("N").put("NP",-1.504077397);


        viterbi x=new viterbi();
        try {
            String sentence;
            String tags;


            while ((sentence = input1.readLine())!=null) { //While there are still characters left to be read
                tags=input2.readLine();
                sentence=sentence.toLowerCase();
                x.reader(tags,sentence);
            }
               x.probabilify(x.stor,x.trans);
                x.fileTest();



        }
        finally{

        }


    }
}
