package anon.trollegle;

import java.util.function.Function; 

import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class Template {
  public static String replace(String template, String regex, Function<String, String> replacement) {
    Pattern p = Pattern.compile(regex);
    Matcher match = p.matcher(template);
    
    String result = "";
    int index = 0;
    
    while (match.find()) {
      result += template.substring(index, match.start());
      result += replacement.apply(match.group(1));
      index = match.end();
    }
    
    result += template.substring(index);
    
    return result;
  }
}