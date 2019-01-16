package anon.trollegle;

import java.util.Hashtable;
import java.util.function.Predicate; 
import java.util.Enumeration;
import java.util.Arrays;

public class Polish<S> { // S stands for structure type

    private Hashtable<String, Predicate<S>> labels = new Hashtable<String, Predicate<S>>();
    
    public Predicate<S> get(String token) {
      return labels.get(token);
    }
    
    public void put(String token, Predicate<S> meaning) {
      labels.put(token, meaning);
    }
    
    public void put(String token, String[] expression) throws IllegalArgumentException {
      put(token, makeCondition(expression));
    }
    
    public Enumeration<String> tokens() {
       return labels.keys();
    }
    
    // This method is used to recursively build up Predicates with the meaning given by the expression args.
    // In other words, it parses a Polish expression and returns the associated Predicate Function.
    // See https://en.wikipedia.org/wiki/Polish_notation
    // We use Polish notation because it's simple to parse, and doesn't require parentheses or commas.
    // ...like I'm going to write an infix parser, no way! :P
    // These Predicates are used as selectors in queries and values in $[]'s.
    // 
    // args is an array of tokens comprising the expression
    private Predicate<S> makeCondition(String[] args) throws IllegalArgumentException { // Polish parser
      // this code checks that the expression is well-formed
      if (count(args) != -1) throw new IllegalArgumentException("not well formed");
      for (int i = 0; i < args.length - 1; i++) 
        if (count(Arrays.copyOfRange(args, 0, i)) < 0) throw new IllegalArgumentException("not well formed");
      
      // this code recursively parses the expression
      // TODO: Isolate this in a helper method so the above check doesn't run each time.
      if (labels.get(args[0]) != null) 
        return labels.get(args[0]);
      // A mathematical theorem for Polish notation says that at each position there is one and only one subexpression.
      // We choose the terms s.t. the above condition holds, so they are subexpressions
      // This implies that they are *the* subexpressions that we need to parse.
      if (args[0].equals("!")) {
        String[] term1 = Arrays.copyOfRange(args, 1, args.length);
        return makeCondition(term1).negate();
      }
      // I wanted to make a helper method to isolate the code for parsing expressions p Tau Rho with p binary, 
      // but Java is so hard. :/ Hence why this form is repeated four times.
      if (args[0].equals("&&")) {
        int i = 2;
        String[] term1 = Arrays.copyOfRange(args, 1, i);
        while (count(term1) >= 0) {
          i++;
          term1 = Arrays.copyOfRange(args, 1, i);
        }
        String[] term2 = Arrays.copyOfRange(args, i, args.length);
        return makeCondition(term1).and(makeCondition(term2));
      }
      if (args[0].equals("||")) {
        int i = 2;
        String[] term1 = Arrays.copyOfRange(args, 1, i);
        while (count(term1) >= 0) {
          i++;
          term1 = Arrays.copyOfRange(args, 1, i);
        }
        String[] term2 = Arrays.copyOfRange(args, i, args.length);
        return makeCondition(term1).or(makeCondition(term2));
      }
      if (args[0].equals("->")) {
        int i = 2;
        String[] term1 = Arrays.copyOfRange(args, 1, i);
        while (count(term1) >= 0) {
          i++;
          term1 = Arrays.copyOfRange(args, 1, i);
        }
        String[] term2 = Arrays.copyOfRange(args, i, args.length);
        return (makeCondition(term1).negate()).or(makeCondition(term2));
      }
      if (args[0].equals("<->")) {
        int i = 2;
        String[] term1 = Arrays.copyOfRange(args, 1, i);
        while (count(term1) >= 0) {
          i++;
          term1 = Arrays.copyOfRange(args, 1, i);
        }
        String[] term2 = Arrays.copyOfRange(args, i, args.length);
        return (makeCondition(term1).negate().and(makeCondition(term2).negate())).or(makeCondition(term1).and(makeCondition(term2)));
      }
      throw new IllegalArgumentException("this will never throw, hopefully");
    }
    
    // This helper method is used to check whether a Polish expression is well-formed.
    // A mathematical theorem says an expression Tau is well formed if and only if 
    // count(Tau) = -1 and count(Rho) >= 0 for each proper initial segment of Tau.
    private int count(String[] args) throws IllegalArgumentException {
      int count = 0;
      for (int i = 0; i < args.length; i ++) {
        if (args[i].equals("||") || args[i].equals("&&")  || args[i].equals("<->")  || args[i].equals("->")) count += 2-1;
        else if (args[i].equals("!")) count += 1-1;
        else if (labels.get(args[i]) != null) count += 0-1;
        else throw new IllegalArgumentException("unknown token `" + args[i] + "'");
      }
      return count;
    }
}