/// by: Tal Bogen   204585194 
//     Tom Goldman 204380836
//     Dani Medan  200572576

import java.util.*;

class homework3 {
    int a = 0; // commit test

    static int LAB = 0; // compiler variable for labels
    static int SWITCH = -1; // switch
    static int CASE = 0;
    static char SIDE = 'L';
    static int BREAK_HELPER = 0;
    static final int var_start = 5; // the beginning of the stack the we can start from.
    static int DYNAMIC_DEPTH = 0;
    static int STATIC_DEPTH = 0;
    static String CURRENT_FUNCTION;
    static HashMap<String, Func_frame> func_hash = new HashMap<>();
    static ArrayList<Integer> func_args_size = new ArrayList<>();
    static int CALL_FUNCTION = 0;
    static String CALL_FUNC_NAME;

    // Abstract Syntax Tree
    static final class AST { // creating class for the abstract syntax tree
        public final String value;
        public final AST left; // can be null
        public final AST right; // can be null

        private AST(String val, AST left, AST right) {
            value = val;
            this.left = left;
            this.right = right;
        }

        public static AST createAST(Scanner input) {
            if (!input.hasNext())
                return null;
            String value = input.nextLine();
            if (value.equals("~"))
                return null;
            return new AST(value, createAST(input), createAST(input));
        }
    }

    // creating the necessary information about the record
    static final class Record {
        ArrayList<Variable> rec_var_list = new ArrayList<>();

        public Record() {
        }

        public Record(Record other) {
            this.rec_var_list = new ArrayList<>();
            for (Variable o_var : other.rec_var_list) {
                this.rec_var_list.add(new Variable(o_var));
            }
        }
    }

    // creating the necessary information about the variable
    static final class Dimention {
        int d_low;
        int d_high;
        int d_size;
        int d_ixa;

        public Dimention() {
        }

        public Dimention(Dimention other) {
            this.d_low = other.d_low;
            this.d_high = other.d_high;
            this.d_size = other.d_size;
            this.d_ixa = other.d_ixa;
        }
    }

    // creating the necessary information about the array
    static final class Array {
        String type;
        int subpart = 0;
        int type_size;

        ArrayList<Dimention> dim_list = new ArrayList<>();

        public Array() {
        }

        public Array(Array other) {
            this.type = other.type;
            this.subpart = other.subpart;
            this.type_size = other.type_size;
            this.dim_list = new ArrayList<>();

            for (Dimention o_dim : this.dim_list) {
                this.dim_list.add(new Dimention(o_dim));
            }
        }
    }

    // creating the necessary information about the variable
    static final class Variable {
        String name;
        String type;
        String pointer_type = null;
        int addr;
        int size;
        String function_name;
        Array arr;
        Record rec;
        String reference_var;

        public Variable() {
        }

        public Variable(Variable other) {
            this.name = other.name;
            this.type = other.type;
            this.pointer_type = other.pointer_type;
            this.addr = other.addr;
            this.size = other.size;
            this.function_name = other.function_name;
            if (other.arr != null) {
                this.arr = new Array(other.arr);
            } else if (other.rec != null) {
                this.rec = new Record(other.rec);
            }
        }
    }

    // creating the necessary information about the function
    static final class Func_frame {
        String type;
        int func_static = 0;
        int current_argument = 0;
        int ssp = 5;
        int sep = 0;
        String papa_func = null;
        SymbolTable ST = new SymbolTable();
    }

    // finds a variable by a String
    private static Variable getVarbyName(ArrayList<Variable> list, String target) {
        Variable temp;

        for (Variable c_var : list) {
            if (c_var.name.equals(target)) {
                return c_var;
            } else if (c_var.type.equals("record")) {
                temp = getVarbyName(c_var.rec.rec_var_list, target);
                if (temp != null)
                    return temp;
            }
        }
        return null;
    }

    private static Func_frame findVarFunc(String var_name) {
        Func_frame func;

        for (String func_name : func_hash.keySet()) {
            func = func_hash.get(func_name);
            if (getVarbyName(func.ST.vars_list, var_name) != null) {
                return func;
            }
        }

        return null;
    }


    private static Variable printVarAdd(String target) {
        int jump = 0;
        int i;

        if (func_hash.containsKey(target)) {
            jump += func_hash.get(CURRENT_FUNCTION).func_static - func_hash.get(target).func_static;
            if (CALL_FUNCTION == 1) {
                System.out.println("ldc " + target);
                jump += 1;
                System.out.println("lda " + jump + " 0"); // loading the variable address
            } else {
                System.out.println("lda " + jump + " 0"); // loading the variable address
            }
            return null;
        }
        Func_frame func = findVarFunc(target);
        jump = func_hash.get(CURRENT_FUNCTION).func_static - func.func_static;

        for (i = 0; (i < func.ST.vars_list.size()) && ((target.compareTo(func.ST.vars_list.get(i).name)) != 0); ++i)
            ; // matching the name of the variable to it's address
        if (i != func.ST.vars_list.size()) {
            System.out.println("lda " + jump + " " + (func.ST.vars_list.get(i).addr)); // loading the variable address
            if (func.ST.vars_list.get(i).reference_var != null) {
                System.out.println("ind");
            }
        }
        return func.ST.vars_list.get(i);
    }

    // returns the size of a given variable
    private static int findVarSize(ArrayList<Variable> vars_list, String var_name) {
        int temp = 0;
        for (Variable cur_var : vars_list) {
            if (var_name.equals(cur_var.name))
                return cur_var.size;
            else if (cur_var.type.equals("record")) {
                temp = findVarSize(cur_var.rec.rec_var_list, var_name);
                if (temp != 1)
                    return temp;

            }
        }

        return 1;
    }

    // recursive, updates an array type variable
    private static int storeArrayData(AST tree, Variable var) {
        if (tree == null) {
            return 0;
        }

        switch (tree.value) {
            case ("array"):
                storeArrayData(tree.left, var);


                break;

            case ("rangeList"):
                storeArrayData(tree.right, var);

                // if only one variable, size is according to the type size. else, according to previous variable size*ixa variable
                if (var.arr.dim_list.size() == 1) {
                    var.arr.dim_list.get(var.arr.dim_list.size() - 1).d_ixa = var.arr.type_size;
                } else if (var.arr.dim_list.size() > 1) {
                    var.arr.dim_list.get(var.arr.dim_list.size() - 1).d_ixa = var.arr.dim_list.get(var.arr.dim_list.size() - 2).d_ixa * var.arr.dim_list.get(var.arr.dim_list.size() - 2).d_size;
                }
                storeArrayData(tree.left, var);
                break;
            case ("range"):
                if (tree.left != null && tree.left.left != null && tree.right != null && tree.right.left != null) {
                    Dimention cur_dim = new Dimention();
                    cur_dim.d_low = Integer.parseInt(tree.left.left.value);
                    cur_dim.d_high = Integer.parseInt(tree.right.left.value);
                    cur_dim.d_size = cur_dim.d_high - cur_dim.d_low + 1;
                    var.arr.dim_list.add(cur_dim);
                }
        }
        return 1;
    }

    // store sub variables into a record type variable
    private static int storeRecordData(AST tree, Variable var) {
        if (tree == null) {
            return 0;
        }
        switch (tree.value) {
            case ("record"):
                storeRecordData(tree.left, var);
                break;
            case ("declarationsList"):
                storeRecordData(tree.left, var);
                storeRecordData(tree.right, var);
                break;
            case ("var"):
                Variable inside_var = new Variable();
                var.rec.rec_var_list.add(inside_var);
                if ((tree.left != null) && (tree.left.left != null) && (tree.right != null)) // if the specific sons aren't empty
                {
                    storeVars(tree, var.rec.rec_var_list, inside_var);
                }
        }
        return 1;
    }

    // computes record size by sub variables
    private static int getRecordSize(Variable record) {
        int sum = 0;
        for (Variable c_var : record.rec.rec_var_list) {
            if (c_var.type.equals("record")) {
                sum = sum + getRecordSize(c_var);
            } else {
                sum = sum + c_var.size;
            }
        }
        return sum;
    }


    // store the variable according to its type
    private static int storeVars(AST tree, ArrayList<Variable> vars_list, Variable var) {
        var.name = (tree.left.left.value);
        var.type = (tree.right.value);

        if (tree.value.equals("byReference")) {
            var.type = "reference";
            if (tree.right.value.equals("pointer")) {
                var.pointer_type = tree.right.left.left.value;
                var.reference_var = "pointer";
            } else {
                var.reference_var = tree.right.left.value;
            }
            var.size = 1;

        } else {

            switch (var.type)  //store the variable total size
            {
                case ("identifier"):
                    if (func_hash.containsKey(tree.right.left.value)) {
                        var.type = "function";
                        var.function_name = tree.right.left.value;
                        var.size = 2;
                    } else {

                    }
                    break;
                case ("int"):
                case ("real"):
                case ("bool"):
                    var.size = 1;
                    break;
                case ("pointer"):
                    var.size = 1;
                    if (tree.right.left.value.equals("identifier")) {
                        var.pointer_type = tree.right.left.left.value;
                    } else {
                        var.pointer_type = tree.right.left.value;
                    }
                    break;
                case ("array"):
                    var.arr = new Array();
                    var.size = 1;
                    if (tree.right.right.value.equals("identifier")) {
                        var.arr.type = tree.right.right.left.value;
                    } else {
                        var.arr.type = tree.right.right.value;
                    }
                    var.arr.type_size = findVarSize(vars_list, var.arr.type);
                    if (storeArrayData(tree.right, var) == 0)
                        return 0;

                    Collections.reverse(var.arr.dim_list);

                    for (Dimention dim : var.arr.dim_list) {
                        var.size = var.size * dim.d_size;
                        var.arr.subpart = var.arr.subpart + dim.d_ixa * dim.d_low;
                    }
                    var.size *= var.arr.type_size;
                    break;
                case ("record"):
                    var.rec = new Record();
                    var.size = 0;
                    var.name = tree.left.left.value;
                    if (storeRecordData(tree.right, var) == 0)
                        return 0;
                    var.size = getRecordSize(var);
                    break;
                default:
                    //run over symbol table and get the variable size
                    int j;
                    for (j = 0; (j < vars_list.size()) && ((var.type.compareTo(vars_list.get(j).name)) != 0); ++j)
                        ;
                    if (j != vars_list.size())
                        var.size = vars_list.get(j).size;
            }
        }
        if (vars_list.size() == 1)
            var.addr = var_start;
        else {
            var.addr = vars_list.get(vars_list.size() - 2).addr + vars_list.get(vars_list.size() - 2).size;
        }

        return 1;
    }

    static final class SymbolTable {

        ArrayList<Variable> vars_list = new ArrayList<>();

        public static SymbolTable generateSymbolTable(AST tree) {
            if (tree == null) { // stop condition
                return null;
            }
            switch (tree.value) { // running on the AST
                case ("program"):
                case ("procedure"):
                case ("function"):
                    String last_function = CURRENT_FUNCTION;
                    func_hash.put(tree.left.left.left.value, new Func_frame());   //new func add to the hash. the key is the name
                    CURRENT_FUNCTION = tree.left.left.left.value;
                    func_hash.get(CURRENT_FUNCTION).papa_func = last_function;
                    func_hash.get(CURRENT_FUNCTION).func_static = STATIC_DEPTH;
                    func_hash.get(CURRENT_FUNCTION).type = tree.value;
                    func_hash.get(CURRENT_FUNCTION).ST = new SymbolTable();

                    generateSymbolTable(tree.left);
                    generateSymbolTable(tree.right);
                    CURRENT_FUNCTION = last_function;

                    break;
                case ("identifierAndParameters"):
                    generateSymbolTable(tree.right);
                    break;
                case ("content"):
                    generateSymbolTable(tree.left);
                    break;
                case ("scope"):
                    generateSymbolTable(tree.left);
                    ++STATIC_DEPTH;
                    generateSymbolTable(tree.right);
                    --STATIC_DEPTH;
                    break;
                case ("inOutParameters"):
                    generateSymbolTable(tree.left);
                    break;
                case ("functionsList"):
                    generateSymbolTable(tree.left);
                    generateSymbolTable(tree.right);
                    break;
                case ("declarationsList"):
                    generateSymbolTable(tree.left);
                    generateSymbolTable(tree.right);
                    break;
                case ("parametersList"):
                    generateSymbolTable(tree.left);
                    generateSymbolTable(tree.right);
                    break;
                case ("var"):
                case ("byReference"):
                case ("byValue"):
                    Func_frame func;
                    Variable var;
                    if ((tree.left != null) && (tree.left.left != null) && (tree.right != null)) // if the specific sons aren't empty
                    {
                        if (tree.right.value.equals("identifier") && !(func_hash.containsKey(tree.right.left.value))) {
                            //System.out.println("im here with "+ tree.right.left.value);
                            func = findVarFunc(tree.right.left.value);
                            var = new Variable(getVarbyName(func.ST.vars_list, tree.right.left.value));
                        } else {
                            var = new Variable(); // create a new instance of the variable and inserting the desired data from the AST to it.
                        }
                        func_hash.get(CURRENT_FUNCTION).ST.vars_list.add(var);
                        storeVars(tree, func_hash.get(CURRENT_FUNCTION).ST.vars_list, var);
                        func_hash.get(CURRENT_FUNCTION).ssp += var.size;
                    } // end if
                    break;
            } // end switch
            return null;
        } // end function

    } // end class

    // prints array
    private static Variable printArray(AST ast, Array array, SymbolTable symbolTable, int curr_dim) {
        Variable temp_arr = new Variable();
        int i;
        if (ast == null) { // stop condition
            return null;
        }
        if (ast.value.equals("identifier")) {
            Func_frame func = findVarFunc(ast.left.value);
            printVarAdd(ast.left.value);
            temp_arr = getVarbyName(func.ST.vars_list, ast.left.value);
            return temp_arr;
        }
        if (ast.value.equals("pointer")) {
            Variable helper = null;
            temp_arr = printArray(ast.left, null, symbolTable, 0);
            Func_frame func = findVarFunc(temp_arr.pointer_type);
            helper = getVarbyName(func.ST.vars_list, temp_arr.pointer_type);
            if (helper != null) {
                temp_arr = helper;
            }
            System.out.println("ind");
            return temp_arr;
        }

        if (ast.value.equals("record")) {
            temp_arr = printRecord(ast);
            return temp_arr;
        }

        if (ast.value.equals("array")) {
            Variable helper = null;
            char side_status = SIDE;
            SIDE = 'L';
            temp_arr = printArray(ast.left, null, symbolTable, 0);
            Func_frame func = findVarFunc(temp_arr.name);
            SIDE = 'R';
            printArray(ast.right, temp_arr.arr, symbolTable, temp_arr.arr.dim_list.size() - 1);
            System.out.println("dec " + temp_arr.arr.subpart);
            SIDE = side_status;
            if (SIDE == 'R') {
                System.out.println("ind");
            }
            func = findVarFunc(temp_arr.name);
            if (temp_arr.arr.type != null) {
                helper = getVarbyName(func.ST.vars_list, temp_arr.arr.type);
            }
            if (helper != null)
                temp_arr = helper;
        }

        if (ast.value.equals("indexList")) {
            printArray(ast.left, array, symbolTable, curr_dim - 1);
            if (ast.right != null && ast.right.left != null) {
                generatePCode(ast.right, symbolTable);
                System.out.println("ixa " + array.dim_list.get(curr_dim).d_ixa);

            }
        }
        return temp_arr;
    }

    // prints record
    private static Variable printRecord(AST tree) {

        int j = 0;
        char side_state = SIDE;
        SIDE = 'L';
        Variable helper;

        if (tree == null) { // stop condition
            return null;
        }


        if (tree.value.equals("pointer")) {
            helper = printRecord(tree.left);
            System.out.println("ind");
            if (helper.pointer_type != null) {
                Func_frame func = findVarFunc(helper.pointer_type);
                return getVarbyName(func.ST.vars_list, helper.pointer_type);
            }
            return helper;
        }


        if (tree.value.equals("identifier")) {
            Func_frame func = findVarFunc(tree.left.value);
            printVarAdd(tree.left.value);
            helper = getVarbyName(func.ST.vars_list, tree.left.value);
            return helper;
        }

        if (tree.value.equals("array")) {
            Variable temp = null;
            helper = printArray(tree, null, null, 0);
            Func_frame func = findVarFunc(helper.name);

            if (helper.arr != null) {
                temp = getVarbyName(func.ST.vars_list, helper.arr.type);
            }
            if (helper.pointer_type != null) {
                temp = getVarbyName(func.ST.vars_list, helper.pointer_type);
            }
            if (temp != null) {
                helper = temp;
            }
            return helper;
        }

        helper = printRecord(tree.left);
        j = 0;
        if (helper.rec != null)

            for (
                    Variable b_var : helper.rec.rec_var_list) {
                if (tree.right.left.value.equals(b_var.name)) {
                    helper = b_var;
                    break;
                }
                j += b_var.size;
            }
        System.out.println("inc " + j);
        if (side_state == 'R') {
            System.out.println("ind");
            SIDE = 'R';
        }
        return helper;
    }

    private static void printFunc_setup(String func_name) {
        System.out.println(func_name + ":");
        System.out.println("ssp " + func_hash.get(func_name).ssp);
        // SEP????????????????????????????????????????????
        System.out.println("ujp " + func_name + "_begin");
    }

    private static void printFunc(String func_name) {

    }

    private static void generatePCode(AST ast, SymbolTable symbolTable) {
        int i;
        if (ast == null) { // stop condition
            return;
        }
        switch (ast.value) {  // traversing on the AST
            case ("program"):
            case ("procedure"):
            case ("function"):
                String func_keeper = CURRENT_FUNCTION;
                CURRENT_FUNCTION = ast.left.left.left.value;
                printFunc_setup(CURRENT_FUNCTION);
                symbolTable = func_hash.get(CURRENT_FUNCTION).ST;
                generatePCode(ast.right, symbolTable);
                CURRENT_FUNCTION = func_keeper;
                break;
            case ("content"):
                generatePCode(ast.left, symbolTable);
                System.out.println(CURRENT_FUNCTION + "_begin:");
                generatePCode(ast.right, symbolTable);
                switch (func_hash.get(CURRENT_FUNCTION).type) {
                    case ("program"):
                        System.out.println("stp");
                        break;
                    case ("procedure"):
                        System.out.println("retp");
                        break;
                    case ("function"):
                        System.out.println("retf");
                        break;
                }
                break;
            case ("scope"):
                generatePCode(ast.right, symbolTable);
                break;
            case ("functionsList"):
                generatePCode(ast.left, symbolTable);
                generatePCode(ast.right, symbolTable);
                break;
            case ("argumentList"):
                generatePCode(ast.left, symbolTable);
                generatePCode(ast.right, symbolTable);
                break;
            case ("statementsList"):
                generatePCode(ast.left, symbolTable);
                generatePCode(ast.right, symbolTable);
                break;
            case ("assignment"):
                SIDE = 'L';
                generatePCode(ast.left, symbolTable);
                SIDE = 'R';
                generatePCode(ast.right, symbolTable);
                if (ast.right != null && ((ast.right.value.compareTo("pointer") == 0)))
                    System.out.println("ind");
                System.out.println("sto");
                break;
            case ("identifier"):
                Variable helper_ident;
                int jump;
                int temp;

                if (ast.left != null) {
                    helper_ident = printVarAdd(ast.left.value);
                    if (CALL_FUNCTION == 1) {
                        temp = func_args_size.get(func_args_size.size() - 1);
                        func_args_size.remove(func_args_size.size() - 1);
                        if (func_hash.containsKey(ast.left.value)) {
                            temp = temp + 2;
                        } else if (helper_ident.arr != null || helper_ident.rec != null) {
                            if (func_hash.get(CALL_FUNC_NAME).ST.vars_list.get(func_hash.get(CALL_FUNC_NAME).current_argument).reference_var == null) {
                                System.out.println("movs " + helper_ident.size);
                                temp=temp +helper_ident.size;
                            } else {
                                temp = temp + 1;
                            }
                        } else if (helper_ident.function_name != null) {
                            System.out.println("movs 2");
                            temp = temp + 2;
                        } else if (helper_ident.reference_var != null) {
                            System.out.println("ind");
                            temp = temp + 1;
                        } else {
                            if (func_hash.get(CALL_FUNC_NAME).ST.vars_list.get(func_hash.get(CALL_FUNC_NAME).current_argument).reference_var == null) {
                                System.out.println("ind");
                            }
                            temp = temp + 1;
                        }
                        ++(func_hash.get(CALL_FUNC_NAME).current_argument);
                        int var_list_size = func_hash.get(CALL_FUNC_NAME).ST.vars_list.size()  ;
                        if (func_hash.get(CALL_FUNC_NAME).current_argument == var_list_size){
                            func_hash.get(CALL_FUNC_NAME).current_argument = 0;
                        }
                        func_args_size.add(temp);
                    } else if (SIDE == 'R') {
                        System.out.println("ind");
                    }


                }
                break;
            case ("constInt"): // loading const variables
                if (ast.left != null)
                    System.out.println("ldc " + ast.left.value);
                if (CALL_FUNCTION == 1) {
                    temp = func_args_size.get(func_args_size.size() - 1);
                    func_args_size.remove(func_args_size.size() - 1);
                    ++temp;
                    func_args_size.add(temp);
                }
                break;
            case ("constReal"):
                if (ast.left != null)
                    System.out.println("ldc " + ast.left.value);
                if (CALL_FUNCTION == 1) {
                    temp = func_args_size.get(func_args_size.size() - 1);
                    func_args_size.remove(func_args_size.size() - 1);
                    ++temp;
                    func_args_size.add(temp);
                }
                break;
            case "true":
                System.out.println("ldc 1");
                break;
            case "false":
                System.out.println("ldc 0");
                break;
            case ("plus"): // for all this cases we need first to load the operands
            case ("minus"):
            case ("multiply"):
            case ("divide"):
            case ("or"):
            case ("and"):
            case ("equals"):
            case ("notEquals"):
            case ("lessThan"):
            case ("greaterThan"):
            case ("lessOrEquals"):
            case ("greaterOrEquals"):
                char side_status = SIDE;
                SIDE = 'R';

                generatePCode(ast.left, symbolTable); // loading left operand

                generatePCode(ast.right, symbolTable); // loading right operand

                SIDE = side_status;
                if (ast.value.compareTo("plus") == 0)
                    System.out.println("add");  // matching the pcode command to the common command
                else if (ast.value.compareTo("minus") == 0) System.out.println("sub");
                else if (ast.value.compareTo("multiply") == 0) System.out.println("mul");
                else if (ast.value.compareTo("divide") == 0) System.out.println("div");
                else if (ast.value.compareTo("or") == 0) System.out.println("or");
                else if (ast.value.compareTo("and") == 0) System.out.println("and");
                else if (ast.value.compareTo("equals") == 0) System.out.println("equ");
                else if (ast.value.compareTo("notEquals") == 0) System.out.println("neq");
                else if (ast.value.compareTo("lessThan") == 0) System.out.println("les");
                else if (ast.value.compareTo("greaterThan") == 0) System.out.println("grt");
                else if (ast.value.compareTo("lessOrEquals") == 0) System.out.println("leq");
                else if (ast.value.compareTo("greaterOrEquals") == 0) System.out.println("geq");

                if (CALL_FUNCTION == 1) {
                    temp = func_args_size.get(func_args_size.size() - 1);
                    func_args_size.remove(func_args_size.size() - 1);
                    --temp;
                    func_args_size.add(temp);
                }

                break;
            case ("negative"): // all these cases need 1 operand
            case ("not"):
            case ("print"):
                SIDE = 'R';

                generatePCode(ast.left, symbolTable); // loading the operand

                if (ast.value.compareTo("negative") == 0)
                    System.out.println("neg"); // matching the pcode command to the common command
                else if (ast.value.compareTo("not") == 0) System.out.println("not");
                else if (ast.value.compareTo("print") == 0) System.out.println("print");
                break;
            case ("if"): // setting labels for if-else statements
                int la = LAB++;

                generatePCode(ast.left, symbolTable);

                System.out.println("fjp L" + la);

                if (ast.left != null && (ast.right.value.compareTo("else") == 0)) { // if-else
                    int lb = LAB++;
                    ast = ast.right;
                    generatePCode(ast.left, symbolTable);
                    System.out.println("ujp L" + lb);
                    System.out.println("L" + (la) + ":");
                    generatePCode(ast.right, symbolTable);
                    System.out.println("L" + (lb) + ":");
                } else if (ast.left != null) { // only if
                    generatePCode(ast.right, symbolTable);
                    System.out.println("L" + la + ":");
                }

                break;
            case ("while"):
                int lc = LAB++;// setting labels for while statement
                int ld = LAB++;
                BREAK_HELPER = ld;
                System.out.println("L" + (lc) + ":");

                generatePCode(ast.left, symbolTable);
                System.out.println("fjp L" + ld);

                generatePCode(ast.right, symbolTable);
                System.out.println("ujp L" + lc);
                System.out.println("L" + (ld) + ":");
                break;
            case ("pointer"):
                generatePCode(ast.left, symbolTable);
                System.out.println("ind");
                break;
            case ("record"):
                printRecord(ast);
                break;
            case ("array"):
                printArray(ast, null, symbolTable, 0);
                break;
            case ("switch"):
                SWITCH = SWITCH + 1;
                int current_switch = SWITCH;

                generatePCode(ast.left, symbolTable);
                System.out.println("neg");
                System.out.println("ixj end_switch" + current_switch);

                generatePCode(ast.right, symbolTable);

                while (CASE != 0) {
                    int switch_state = SWITCH;
                    System.out.println("ujp case_" + switch_state + "_" + CASE);
                    CASE = CASE - 1;
                }
                System.out.println("end_switch" + current_switch + ":");
                break;

            case ("caseList"):

                generatePCode(ast.left, symbolTable);

                generatePCode(ast.right, symbolTable);
                break;
            case ("case"):
                int switch_state = SWITCH;
                CASE = CASE + 1;
                System.out.println("case_" + switch_state + "_" + CASE + ":");

                generatePCode(ast.right, symbolTable);
                System.out.println("ujp end_switch" + switch_state);
                break;
            case ("break"):
                System.out.println("ujp L" + BREAK_HELPER);
                BREAK_HELPER--;
                break;
            case ("call"):
                int jump_call = 0;
                int callfunc_status = CALL_FUNCTION;
                CALL_FUNCTION = 1;
                CALL_FUNC_NAME = ast.left.left.value;

                // get function
                if (func_hash.containsKey(CALL_FUNC_NAME)) {
                    jump_call = 1;
                    jump_call += func_hash.get(CURRENT_FUNCTION).func_static - func_hash.get(CALL_FUNC_NAME).func_static;
                    func_args_size.add(0);
                    System.out.println("mst " + jump_call);
                    generatePCode(ast.right, symbolTable);
                    System.out.println("cup " + func_args_size.get(func_args_size.size() - 1) + " " + CALL_FUNC_NAME);

                    func_args_size.remove(func_args_size.size() - 1);
                }
                // get variable function
                else {
                    Func_frame func = findVarFunc(CALL_FUNC_NAME);

                    jump_call += func.func_static - func_hash.get(CURRENT_FUNCTION).func_static;
                    for (i = 0; (i < func.ST.vars_list.size()) && ((CALL_FUNC_NAME.compareTo(func.ST.vars_list.get(i).name)) != 0); ++i)
                        ; // matching the name of the variable to it's address
                    func_args_size.add(0);
                    CALL_FUNC_NAME = func_hash.get(CURRENT_FUNCTION).ST.vars_list.get(i).function_name;

                    System.out.println("mstf " + jump_call + " " + func.ST.vars_list.get(i).addr);
                    generatePCode(ast.right, symbolTable);
                    System.out.println("smp " + func_args_size.get(func_args_size.size() - 1));
                    System.out.println("cupi " + jump_call + " " + func.ST.vars_list.get(i).addr);
                }
                CALL_FUNCTION = callfunc_status;
                break;
        }

    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in); // getting input
        AST ast = AST.createAST(scanner); // creating AST with the input
        SymbolTable symbolTable = SymbolTable.generateSymbolTable(ast); // creating symbol table
        generatePCode(ast, symbolTable); // the function to create the pcode using the AST and the SymbolTable
    }

}