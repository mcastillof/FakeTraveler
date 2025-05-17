package cl.coders.faketraveler.domain;

public class TypeParser {
    public static Double parseDouble(String text){
        if (text.matches("\\d+\\.\\d+")) {
            return Double.parseDouble(text);
        }else {
            return null;
        }
    }
    public static Integer parseInteger(String text){
        if (text.matches("\\d+")) {
            return Integer.parseInt(text);
        }else {
            return null;
        }
    }
}
