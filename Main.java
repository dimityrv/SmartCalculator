package calculator;

import java.math.BigInteger;
import java.util.*;
import java.util.regex.*;

public class Main {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        boolean cont = true;
        while (cont) {
            String line = scanner.nextLine();

            if (line != null && line.length() > 0 && line.trim().replace(" ","").length() > 0) {
                //processing commands
                if (line.startsWith("/")) {
                    switch (line.trim()) {
                        case "/help":
                            System.out.println("It calculates the expressions like these: "  +
                                    "*	4 + 6 - 8, 2 - 3 - 4, z+f * (a-b) and so on." +
                                    "It supports addition, substraction, multiplication, "+
                                    "divison and power operations. Variables and braces" +
                                    " are also supported. Enter '/exit' to terminate program." +
                                    "Enter '/vars' to show variables.");
                            break;

                        case "/vars":
                            System.out.println(showVariables());
                            break;

                        case "/exit":
                            cont = false;
                            break;

                        default:
                              System.out.println("Unknown command");
                    }
                    //processing expression
                } else {
                    try {
                        Expression expr = new Expression(line);
                        expr.eval(variables);
                        if(line.contains("=")){

                        }else
                        {
                            System.out.println(expr.getResult());
                        }
                    }  catch (NumberFormatException e) {
                        System.out.println("Numbers in an expression should be in range of Integer type : -2^31 ... 2^31-1, got: " + e.getMessage());
                    } catch (IllegalArgumentException e) {
                        System.out.println(e.getMessage());
                    }
                }
            }
        }
        System.out.println("Bye!");
    }

    //show assigned variables
    public static String showVariables() {
        if (variables.size() == 0) return "Variables are not set";
        StringBuilder result = new StringBuilder();
        result.append("Variables: ");
        for (String key : variables.keySet()) {
            result.append(key);
            result.append(" = ");
            result.append(variables.get(key));
            result.append(" ");
        }
        return result.toString();
    }


    private static Map<String, BigInteger> variables = new HashMap<String, BigInteger>();
}

//This class represents an expression
class Expression {

    //constructs a new Expression object with raw expression string
    public Expression(String rawLine) {
        try {

            infixLine = atomize(rawLine);

        } catch (IllegalArgumentException iae) {
            throw iae;
        }
    }

    public String getResult() {
        StringBuilder sb = new StringBuilder();
        if (expAssignsValueToVariable) {
            sb.append(assignedVariableName);
            sb.append(" = ");
            sb.append(result);
        } else {
            sb.append(result);
        }
        return sb.toString();
    }

    public void eval(Map<String, BigInteger> variables) {
        if (infixLine != null) {
            if (!expAssignsValueToVariable) {
                setResult(evaluateExpression(infixLine, variables));
            } else {

                List<ExPart> eval = infixLine.subList(2, infixLine.size());

                //in case we have a simple assignment
                if (eval.size() == 1) {
                    substLine = substituteVariables(eval, variables);
                    setResult(new BigInteger(substLine.get(0).getValue()));
                    assignedVariableName = infixLine.get(0).getValue();
                    variables.put(assignedVariableName, result);
                } else {
                    setResult(evaluateExpression(eval, variables));
                    assignedVariableName = infixLine.get(0).getValue();
                    variables.put(assignedVariableName, result);
                }
            }
        } else throw new IllegalArgumentException("Expression does not contain proper infix line ");
    }//eof eval

    private BigInteger evaluateExpression(List<ExPart> infixLine, Map<String, BigInteger> variables) {
        //substitute variables
        substLine = substituteVariables(infixLine, variables);

        //convert to Postfix form
        postfix = infixToPostfix(substLine);

        return calculatePostfix(postfix);
    }

    private void setResult(BigInteger val) {
        this.result = val;
    }


    private BigInteger calculatePostfix(List<ExPart> postfix) {
        if (postfix != null && postfix.size() > 0) {
            Deque<BigInteger> res = new LinkedList<BigInteger>();       //results stack
            res.addFirst(BigInteger.ZERO);                                      //add dummy value to allow unary operations
            for (ExPart word : postfix) {
                if (word.getType() == Type.DIGIT) {
                    res.addFirst(new BigInteger(word.getValue()));
                } else if (word.getType().isOperator() && res.size() >= 2) {
                    BigInteger operand1 = (BigInteger) res.removeFirst();
                    BigInteger operand2 = (BigInteger) res.removeFirst();
                    BigInteger result = BigInteger.ZERO;
                    if (word.getType() == Type.MINUS) {
                        result = operand2.subtract(operand1);
                    } else if (word.getType() == Type.PLUS) {
                        result = operand1.add(operand2);
                    } else if (word.getType() == Type.MULT) {
                        result = operand1.multiply(operand2);
                    } else if (word.getType() == Type.DIV) {
                        result = operand2.divide(operand1);
//                    } else if (word.getType() == Type.POW) {
//                        result = (BigInteger) Math.pow(operand2.modPow(),operand1);
//                    } else throw new IllegalArgumentException("Unsupported operation " + word);
                    } else throw new IllegalArgumentException("Invalid expression");
                    res.addFirst(result);
                } else throw new IllegalArgumentException("Can't process an expression");
            }
            return (BigInteger) res.removeFirst();
        } else throw new IllegalArgumentException("Expression is null or zero length");
    }

    /**
     * Converts infix expression to postfix notation.
     *
     * @param words arithmetic expression in infix notation
     * @return expression in postfix notation
     **/
    private static List<ExPart> infixToPostfix(List<ExPart> words) throws IllegalArgumentException {
        if (words != null && words.size() > 0) {
            List<ExPart> result = new ArrayList<ExPart>();
            Deque<ExPart> stack = new LinkedList<ExPart>();

            for (ExPart word : words){

                switch(word.getType()) {
                    case DIGIT:
                        result.add(word);
                        break;

                    case LEFT_PAR:
                        stack.addFirst(word);
                        break;

                    case RIGHT_PAR:
                        while(true) {
                            if (stack.isEmpty() || stack.getFirst().getType() == Type.LEFT_PAR) break;
                            result.add(stack.removeFirst());
                        }
                        if (!stack.isEmpty() && stack.getFirst().getType() == Type.LEFT_PAR) stack.removeFirst();
//                        else throw new IllegalArgumentException("Unsupported expression. Left bracket missing");
                        else throw new IllegalArgumentException("Invalid expression");
                        break;

                    case PLUS:
                    case MINUS:
                    case MULT:
                    case DIV:
                    case POW:

                        while (!stack.isEmpty() && stack.peek().getType() != Type.LEFT_PAR && word.getPriority() <= stack.getFirst().getPriority()){
                            result.add(stack.removeFirst());
                        }
                        stack.addFirst(word);
                        break;

                    default:
                        throw new IllegalArgumentException("Unsupported token " + word.getValue());
                } //eof Switch
            } //eof for

            while (!stack.isEmpty()) {
                result.add(stack.removeFirst());
            }

			/*
			for (ExPart ex : result) {
				System.out.print(ex.getValue());
			}
			System.out.println();
			*/
            return result;

        } else throw new IllegalArgumentException("Unsupported expression " + words.toString());
    }


    private List<ExPart> substituteVariables(List<ExPart> line, Map<String, BigInteger> variables) {
        List<ExPart> result = new ArrayList<ExPart>();
        for (ExPart ex : line) {
            if (ex.getType() == Type.VARIABLE) {
                if (variables.containsKey(ex.getValue())) {
                    ExPart nw = new ExPart(variables.get(ex.getValue()));
                    result.add(nw);
                } else throw new IllegalArgumentException("Undefined variable " + ex.getValue());
            } else {
                result.add(ex);
            }
        }
        return result;
    }


    private void addAtomized(ExPart ex, List<ExPart> list) {
        if (list == null) throw new IllegalArgumentException("Can't add word to a NULL list");
        if (ex == null) throw new IllegalArgumentException("Can't add a NULL expression");
        if (ex.getType() == Type.UNSUPPORTED) throw new IllegalArgumentException("Can't add a word of unsupported type: " + ex.getValue());

        if (list.size() > 0) {
            ExPart prev = list.get(list.size()-1);

            switch (ex.getType()) {
                case EQUALS:
                    if (expAssignsValueToVariable) {
                        throw new IllegalArgumentException("Expression can't contain more than one assignment");
                    } else if (prev.getType() == Type.VARIABLE){
                        expAssignsValueToVariable = true;
                    } else {
                        throw new IllegalArgumentException("Left side of an assignment operator should be a variable, got: \"" + prev.getValue() + ex.getValue() + "\"");
                    }
                    break;
                case VARIABLE:
                case DIGIT:
                    if (!prev.getType().isOperator()) {
                        throw new IllegalArgumentException("Illegal variable identifier or expression (left side of a variable or value should be an operator, got: \"" + prev.getValue() + ex.getValue() + "\"");
                    }
                    break;
            }

        }

        list.add(ex);
    }

    private List<ExPart> atomize(String line) {
        List<ExPart> result = new ArrayList<ExPart>();

        //dirty checks
        if (line == null) throw new IllegalArgumentException("Null line");
        Matcher matcher = ALLOWED_CHARS.matcher(line.trim().replace(" ",""));
        if (!matcher.matches()) throw new IllegalArgumentException("Unsupported expression: " + line);

        String[] words = line.trim().split(" ");

        //outer loop through character groups separated by spaces
        for (String word : words) {
            try {
                word = word.replace(" ","");
                //if we have only one symbol we can simply output it, after type determination
                if (word.length() == 1) {
                    Type curType = determineType(word.charAt(0));
                    if (curType == Type.UNSUPPORTED) {
                        throw new IllegalArgumentException("Unsupported expression: " + word);
                    } else {
                        addAtomized(new ExPart(word, curType), result);
                    }
                    //otherwise we proceed expression character by character
                } else if (word.length() > 1) {
                    List<ExPart> processedWords = normalizeWord(word);
                    for (ExPart ex : processedWords) {
                        addAtomized(ex, result);
                    }
                }
            } catch (IllegalArgumentException iae) {
                throw iae;
            }
        }
        return result;
    }

    private List<ExPart> normalizeWord(String word) {
        if (word == null) throw new IllegalArgumentException("Null word");

        char[] chars = word.toCharArray();
        List<ExPart> result = new ArrayList<ExPart>();

        boolean ongoing = false;
        StringBuilder current = new StringBuilder(word.length());
        Type curCharType = null;

        for (char c : chars) {
            if (!ongoing) {
                curCharType = determineType(c);
                if (curCharType == Type.UNSUPPORTED) {
                    throw new IllegalArgumentException("Unsupported char: " + c);
                } else {
                    current.append(c);
                    ongoing = true;
                }
            } else {
                Type newCharType = determineType(c);
                if (newCharType == Type.UNSUPPORTED) {
                    throw new IllegalArgumentException("Unsupported char: " + c);
                } else if (newCharType.isOfSameGroup(curCharType)) {
                    current.append(c);
                } else {
                    try {
                        ExPart ex = new ExPart(current.toString(), curCharType);
                        result.add(ex);
                        current.setLength(0);
                        current.append(c);
                        curCharType = newCharType;
                    } catch (IllegalArgumentException iae) {
                        throw iae;
                    }
                }
            }
        }

        result.add(new ExPart(current.toString(), curCharType));
        return result;

    }

    private char processOperator(String operator) {
        if (operator == null) throw new IllegalArgumentException("Unsupported operator NULL");

        //equals
        Matcher matcher = EQUALS_PATTERN.matcher(operator);
        if (matcher.matches()) {
            return '=';
        }

        //plus
        matcher = PLUS_PATTERN.matcher(operator);
        if (matcher.matches()) {
            return '+';
        }

        //plusminus
        matcher = PLUSMINUS_PATTERN.matcher(operator);
        if (matcher.matches()) {
            operator = operator.replace("+","");
        }

        //minus
        matcher = MINUS_PATTERN.matcher(operator);
        if (matcher.matches()) {
            if (operator.length() % 2 == 0) {
                return '+';
            } else {
                return '-';
            }
        }

        throw new IllegalArgumentException("Unsupported operator: " + operator);
    }

    private Type determineType(char c) {
        if (Character.isDigit(c)) {
            return Type.DIGIT;
        } else if (Character.isLetter(c)) {
            return Type.VARIABLE;
        } else if (c == '=' || c == '+' || c == '-' || c == '*' || c == '/' || c == '(' || c == ')' || c =='^') {
            switch(c) {
                case '=':
                    return Type.EQUALS;
                case '+':
                    return Type.PLUS;
                case '-':
                    return Type.MINUS;
                case '*':
                    return Type.MULT;
                case '/':
                    return Type.DIV;
                case '(':
                    return Type.LEFT_PAR;
                case ')':
                    return Type.RIGHT_PAR;
                case '^':
                    return Type.POW;
            }
        }
        return Type.UNSUPPORTED;
    }

    //possible types of expression parts
    enum Type {
        VARIABLE, DIGIT, PLUS, MINUS, EQUALS, MULT, DIV, LEFT_PAR, RIGHT_PAR, POW, UNSUPPORTED;

        boolean isOfSameGroup(Type other) {
            if (other == null || !(other instanceof Type)) return false;
            if (this == UNSUPPORTED || other == UNSUPPORTED) return false;

            if ((this == VARIABLE && other == VARIABLE) ||
                    (this == DIGIT && other == DIGIT) 		||
                    (this.isPlusMinus() && other.isPlusMinus()) ||
                    (this == MULT && other == MULT) ||
                    (this == DIV && other == DIV) ||
                    (this == POW && other == POW)) {
                return true;
            } else {
                return false;
            }
        }

        boolean isOperator() {
            return (this == PLUS || this == MINUS || this == EQUALS || this == MULT || this == DIV || this == LEFT_PAR || this == RIGHT_PAR || this == POW);
        }

        boolean isPlusMinus() {
            return (this == PLUS || this == MINUS);
        }

        int getPriority() {
            if (this == LEFT_PAR || this == RIGHT_PAR) return 3;
            if (this == POW) return 2;
            if (this == DIV || this == MULT) return 1;
            return 0;
        }

    }

    //represents a part of an expression
    class ExPart {

        ExPart(String expr, Type type) {
            if (type.isOperator() && expr.length() > 1) {
                try {
                    char oper = processOperator(expr);
                    type = determineType(oper);
                    expr = Character.toString(oper);
                } catch (IllegalArgumentException iae) {
                    throw iae;
                }
            }
            this.expr = expr;
            this.type = type;

        }

        ExPart(BigInteger expr) {
            this.expr = expr.toString();
            this.type = Type.DIGIT;

        }

        public int getPriority() {
            return this.type.getPriority();
        }

        public Type getType() {
            return this.type;
        }

        public String getValue() {
            return this.expr;
        }

        public void setType(Type type) {
            this.type = type;
        }

        public void setValue(String expr) {
            this.expr = expr;
        }

        private Type type;
        private String expr;
    }

    //stores "correct" expression in infix form
    private List<ExPart> infixLine;

    //stores expression with substituted vars
    private List<ExPart> substLine;

    //stores expression in postfix form
    private List<ExPart> postfix;

    //stores eval result
    private BigInteger result;

    private boolean expAssignsValueToVariable = false;
    private String assignedVariableName;

    //pattern which include all allowed chars in expression
    private static final Pattern ALLOWED_CHARS = Pattern.compile("^[a-zA-Z0-9+-=*/()^]*$");

    //operator patterns
    private static final Pattern EQUALS_PATTERN = Pattern.compile("^[=]*$");
    private static final Pattern PLUS_PATTERN = Pattern.compile("^[+]*$");
    private static final Pattern MINUS_PATTERN = Pattern.compile("^[-]*$");
    private static final Pattern PLUSMINUS_PATTERN = Pattern.compile("^[-+]*$");

}
