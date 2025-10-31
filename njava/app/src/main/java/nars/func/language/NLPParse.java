//package nars.func.language;
//
//import edu.stanford.nlp.simple.Sentence;
//import jcog.WTF;
//import nars.$;
//import nars.Term;
//
//import java.util.LinkedHashMap;
//import java.util.regex.Pattern;
//
///**
// *  parsing mode that wraps sub-parses in narsese like:
// *     (`x is y` = = > `a is not b or c`)
// *     (`this sentence is false` --> `statement that refers to itself`)
// *
// * then if you save the parse you can also re-apply it in output filtering
// * in case there are multiple inputs that resolve to the same narsese, it can save all and choose one
// * parsing sentence fragments will be more predictable than always trying to parse entire sentences
// * and having to guess that it will pick the right narsese is infeasible at this level
// * sentence fragments could be nouns, verb phrases, gerunds etc
// * entire sentences or paragraphs could also be parsed of course
// * its a blend of narsese and NL, can be called 'natural narsese'
// * maybe also should include macros for common multi-compound constructions
// */
//class NLPParse {
//
//    /**
//     * matches any text within backtick quotes
//     */
//    private static final Pattern backticks = Pattern.compile("`(.+?)`");
//
//    /**
//     * order corresponds to pattern variables
//     */
//    final LinkedHashMap<String, Term> subExpr = new LinkedHashMap();
//
//    final String input;
//    final Term output;
//
//    NLPParse(String input) {
//        this.input = input;
//        if (input.contains("`")) {
//            var m = backticks.matcher(input);
//            while (m.find()) {
//                String innerX = m.group(1); // group 1 is the text within the backtick quotes
//                var innerY = parseNL(innerX);
//                var prev = subExpr.put(innerX, innerY.term());
//                if (prev != null) throw new WTF();
//                input = input.replaceAll("\\`" + innerX + "\\`", $.varPattern(subExpr.size()).toString());
//                m = backticks.matcher(input); //re-match incase multiple have been replaced, we avoid creating a new variable
//            }
//            output = replace(parseNarsese(input), subExpr);
//        } else {
//            output = parseNL(input);
//        }
//    }
//
//    @Override
//    public String toString() {
//        return "input=" + input + "\noutput=" + output + "\nsubExpr=" + subExpr;
//    }
//
//    private Term replace(Term x, LinkedHashMap<String, Term> subExpr) {
//        int k = 1;
//        Term xt = x.term();
//        for (var e : subExpr.entrySet()) {
//            xt = xt.replace($.varPattern(k), e.getValue());
//            k++;
//        }
//        //return x instanceof Task ? SpecialTermTask.proxy((NALTask) x, xt) : xt;
//        return xt;
//    }
//
//    /** TODO handle both Terms and Tasks */
//    private Term parseNarsese(String input) {
//        return $.$$$(input);
//    }
//
//    private Term parseNL(String input) {
//        return NLPInput.node(new Sentence(input).parse().skipRoot());
//    }
//}
