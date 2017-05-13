/**
 * Created by Johan007 on 17/5/11.
 */
public class Instruction {

    public static final String SPECIAL = "000000";

    // 000
    public static final String J = "000000";
    public static final String BEQ = "000010";
    //public static final String BLTZ = "000001";
    public static final String BGTZ = "000100";
    public static final String SW = "000110";
    public static final String LW = "000111";
    public static final String BREAK = "000101";

    //110
    public static final String ADD = "110000";
    public static final String SUB = "110001";
    public static final String MUL = "110010";
    public static final String AND = "110011";
    public static final String OR = "110100";
    public static final String XOR = "110101";
    public static final String NOR = "110110";

    //111
    public static final String ADDI = "111000";
    public static final String ANDI = "111001";
    public static final String ORI = "111010";
    public static final String XORI = "111011";


    public int address;
    public static int length;
    String bits;
    String sMSB,
            sOperation,
            sOperand1,
            sOperand2,
            sResult,
            sImm;//立即数
            //sFunction;

    String format;

    String rs, rt, rd, base;
    int imm;

    boolean isBranch;

    public Instruction(String bits, int address) {
        this.bits = bits;
        this.sMSB = this.bits.substring(0, 3);
//        this.sOperation = this.bits.substring(0, 6);
//        this.sOperand1 = this.bits.substring(6, 11);
//        this.sOperand2 = this.bits.substring(11, 16);
//        this.sResult = this.bits.substring(16, 21);
//        this.sImm = this.bits.substring(21, 26);
//        this.sFunction = this.bits.substring(26, 32);
//        this.address = address;
        length = this.bits.length() / 8;
        this.isBranch = false;

        switch (this.sMSB){
            case "000":
                this.sOperation = this.bits.substring(0, 6);
                this.sOperand1 = this.bits.substring(6, 11);
                this.sOperand2 = this.bits.substring(11, 16);
                this.sResult = "";
                this.sImm = this.bits.substring(16, 32);
                //this.sFunction = this.sOperation;
                switch (this.sOperation)
                {
                    case Instruction.J:
                        // this.format = "J target";
                        this.format = String.format("J\t#%d",
                        Integer.valueOf(this.sOperand1 + this.sOperand2  + this.sImm + "00", 2));
                        this.isBranch = true;
                        this.imm = Integer.valueOf(this.sOperand1 + this.sOperand2  + this.sImm + "00", 2);
                        break;
                    case Instruction.BEQ:
                        // this.format = "BEQ rs, rt, offset";
                        this.format = String.format("BEQ\tR%d, R%d, #%d", Integer.valueOf(this.sOperand1, 2),
                                Integer.valueOf(this.sOperand2, 2),
                                Integer.valueOf(this.signExtend(  this.sImm  + "00", 32), 2));
                        this.isBranch = true;
                        //this.imm = Integer.valueOf(this.signExtend(this.sResult + this.sImm + this.sFunction + "00", 32), 2);
                        this.rs = "R" + Integer.valueOf(this.sOperand1, 2);
                        this.rt = "R" + Integer.valueOf(this.sOperand2, 2);
                        break;
                    case Instruction.BGTZ:
                        // this.format = "BGTZ rs, offset";
                        this.format = String.format("BGTZ\tR%d, #%d", Integer.valueOf(this.sOperand1, 2),
                                Integer.valueOf(this.signExtend(this.sResult + this.sImm  + "00", 32), 2));
                        this.isBranch = true;
                        this.imm = Integer.valueOf(this.signExtend(this.sResult + this.sImm  + "00", 32), 2);
                        this.rs = "R" + Integer.valueOf(this.sOperand1, 2);
                        break;
                    case Instruction.BREAK:
                        this.format = "BREAK";
                        break;
                    case Instruction.SW:
                        // this.format = "SW rt, offset(base)";
                        this.format = String.format("SW\tR%d, %d(R%d)", Integer.valueOf(this.sOperand2, 2),
                                Integer.valueOf(this.signExtend(this.sResult + this.sImm , 32), 2), Integer.valueOf(this.sOperand1, 2));
                        this.rt = "R" + Integer.valueOf(this.sOperand2, 2);
                        this.imm = Integer.valueOf(this.signExtend(this.sResult + this.sImm , 32), 2);
                        this.base = "R" + Integer.valueOf(this.sOperand1, 2);
                        break;
                    case Instruction.LW:
                        // this.format = "LW rt, offset(base)";
                        this.format = String.format("LW\tR%d, %d(R%d)", Integer.valueOf(this.sOperand2, 2),
                                Integer.valueOf(this.signExtend(this.sResult + this.sImm , 32), 2), Integer.valueOf(this.sOperand1, 2));
                        this.rt = "R" + Integer.valueOf(this.sOperand2, 2);
                        this.imm = Integer.valueOf(this.signExtend(this.sResult + this.sImm , 32), 2);
                        this.base = "R" + Integer.valueOf(this.sOperand1, 2);
                        break;

                }
                break;
            case "110":
                this.sOperation = this.bits.substring(0, 3)+this.bits.substring(13, 16);
                this.sOperand1 = this.bits.substring(3, 8);
                this.sOperand2 = this.bits.substring(8, 13);
                this.sResult = this.bits.substring(16, 21);
                //this.sImm = this.bits.substring(21, 26);
                //this.sFunction = this.sOperation;
                switch (this.sOperation)
                {
                    case Instruction.ADD:
                        // this.format = "ADD rd, rs, rt"
                        this.format = String.format("ADD\tR%d, R%d, R%d", Integer.valueOf(this.sResult, 2),
                                Integer.valueOf(this.sOperand1, 2), Integer.valueOf(this.sOperand2, 2));
                        this.rs = "R" + Integer.valueOf(this.sOperand1, 2);
                        this.rt = "R" + Integer.valueOf(this.sOperand2, 2);
                        this.rd = "R" + Integer.valueOf(this.sResult, 2);
                        break;
                    case Instruction.SUB:
                        // this.format = "SUB rd, rs, rt";
                        this.format = String.format("SUB\tR%d, R%d, R%d", Integer.valueOf(this.sResult, 2),
                                Integer.valueOf(this.sOperand1, 2), Integer.valueOf(this.sOperand2, 2));
                        this.rs = "R" + Integer.valueOf(this.sOperand1, 2);
                        this.rt = "R" + Integer.valueOf(this.sOperand2, 2);
                        this.rd = "R" + Integer.valueOf(this.sResult, 2);
                        break;
                    case Instruction.MUL:
                        // this.format = "MUL rd, rs, rt";
                        this.format = String.format("MUL\tR%d, R%d, R%d", Integer.valueOf(this.sResult, 2),
                                Integer.valueOf(this.sOperand1, 2), Integer.valueOf(this.sOperand2, 2));
                        this.rs = "R" + Integer.valueOf(this.sOperand1, 2);
                        this.rt = "R" + Integer.valueOf(this.sOperand2, 2);
                        this.rd = "R" + Integer.valueOf(this.sResult, 2);
                        break;
                    case Instruction.AND:
                        // this.format = "AND rd, rs, rt"
                        this.format = String.format("AND\tR%d, R%d, R%d", Integer.valueOf(this.sResult, 2),
                                Integer.valueOf(this.sOperand1, 2), Integer.valueOf(this.sOperand2, 2));
                        this.rs = "R" + Integer.valueOf(this.sOperand1, 2);
                        this.rt = "R" + Integer.valueOf(this.sOperand2, 2);
                        this.rd = "R" + Integer.valueOf(this.sResult, 2);
                        break;
                    case Instruction.OR:
                        // this.format = "OR rd, rs, rt"
                        this.format = String.format("OR\tR%d, R%d, R%d", Integer.valueOf(this.sResult, 2),
                                Integer.valueOf(this.sOperand1, 2), Integer.valueOf(this.sOperand2, 2));
                        this.rs = "R" + Integer.valueOf(this.sOperand1, 2);
                        this.rt = "R" + Integer.valueOf(this.sOperand2, 2);
                        this.rd = "R" + Integer.valueOf(this.sResult, 2);
                        break;
                    case Instruction.XOR:
                        // this.format = "XOR rd, rs, rt"
                        this.format = String.format("XOR\tR%d, R%d, R%d", Integer.valueOf(this.sResult, 2),
                                Integer.valueOf(this.sOperand1, 2), Integer.valueOf(this.sOperand2, 2));
                        this.rs = "R" + Integer.valueOf(this.sOperand1, 2);
                        this.rt = "R" + Integer.valueOf(this.sOperand2, 2);
                        this.rd = "R" + Integer.valueOf(this.sResult, 2);
                        break;
                    case Instruction.NOR:
                        // this.format = "NOR rd, rs, rt"
                        this.format = String.format("NOR\tR%d, R%d, R%d", Integer.valueOf(this.sResult, 2),
                                Integer.valueOf(this.sOperand1, 2), Integer.valueOf(this.sOperand2, 2));
                        this.rs = "R" + Integer.valueOf(this.sOperand1, 2);
                        this.rt = "R" + Integer.valueOf(this.sOperand2, 2);
                        this.rd = "R" + Integer.valueOf(this.sResult, 2);
                        break;

                }
                break;
            case "111":
                this.sOperation = this.bits.substring(0, 3)+this.bits.substring(13, 16);
                this.sOperand1 = this.bits.substring(3, 8);
                //this.sOperand2 = this.bits.substring(11, 16);
                this.sResult = this.bits.substring(8, 13);
                this.sImm = this.bits.substring(16, 32);
                //this.sFunction = this.sOperation;
                switch (this.sOperation)
                {
                    case Instruction.ADDI:
                        // this.format = "ADDI rt, rs, sa";
                        this.format = String.format("ADDI\tR%d, R%d, #%d", Integer.valueOf(this.sResult, 2),
                                Integer.valueOf(this.sOperand1, 2), Integer.valueOf(this.sImm, 2));
                        this.imm = Integer.valueOf(this.sImm, 2);
                        this.rs = "R" + Integer.valueOf(this.sOperand1, 2);
                        this.rt = "R" + Integer.valueOf(this.sResult, 2);
                        break;
                    case Instruction.ANDI:
                        // this.format = "ANDI rt, rs, sa";
                        this.format = String.format("ANDI\tR%d, R%d, #%d", Integer.valueOf(this.sResult, 2),
                                Integer.valueOf(this.sOperand1, 2), Integer.valueOf(this.sImm, 2));
                        this.imm = Integer.valueOf(this.sImm, 2);
                        this.rs = "R" + Integer.valueOf(this.sOperand1, 2);
                        this.rt = "R" + Integer.valueOf(this.sResult, 2);
                        break;
                    case Instruction.ORI:
                        // this.format = "ORI rt, rs, sa";
                        this.format = String.format("ORI\tR%d, R%d, #%d", Integer.valueOf(this.sResult, 2),
                                Integer.valueOf(this.sOperand1, 2), Integer.valueOf(this.sImm, 2));
                        this.imm = Integer.valueOf(this.sImm, 2);
                        this.rs = "R" + Integer.valueOf(this.sOperand1, 2);
                        this.rt = "R" + Integer.valueOf(this.sResult, 2);
                        break;
                    case Instruction.XORI:
                        // this.format = "XORI rt, rs, sa";
                        this.format = String.format("ADDI\tR%d, R%d, #%d", Integer.valueOf(this.sResult, 2),
                                Integer.valueOf(this.sOperand1, 2), Integer.valueOf(this.sImm, 2));
                        this.imm = Integer.valueOf(this.sImm, 2);
                        this.rs = "R" + Integer.valueOf(this.sOperand1, 2);
                        this.rt = "R" + Integer.valueOf(this.sResult, 2);
                        break;


                }
                break;
        }
//        if (this.sOperation.equals(Instruction.SPECIAL)) {
//            if (this.sFunction.equals(Instruction.JR)) {
//                // this.format = "JR rs";
//                this.format = String.format("JR\tR%s", Integer.valueOf(this.sOperand1, 2));
//                this.isBranch = true;
//                this.rs = "R" + Integer.valueOf(this.sOperand1, 2);
//            } else if (this.sFunction.equals(Instruction.ADD)) {
//                // this.format = "ADD rd, rs, rt"
//                this.format = String.format("ADD\tR%d, R%d, R%d", Integer.valueOf(this.sResult, 2),
//                        Integer.valueOf(this.sOperand1, 2), Integer.valueOf(this.sOperand2, 2));
//                this.rs = "R" + Integer.valueOf(this.sOperand1, 2);
//                this.rt = "R" + Integer.valueOf(this.sOperand2, 2);
//                this.rd = "R" + Integer.valueOf(this.sResult, 2);
//
//            } else if (this.sFunction.equals(Instruction.SUB)) {
//                // this.format = "SUB rd, rs, rt";
//                this.format = String.format("SUB\tR%d, R%d, R%d", Integer.valueOf(this.sResult, 2),
//                        Integer.valueOf(this.sOperand1, 2), Integer.valueOf(this.sOperand2, 2));
//                this.rs = "R" + Integer.valueOf(this.sOperand1, 2);
//                this.rt = "R" + Integer.valueOf(this.sOperand2, 2);
//                this.rd = "R" + Integer.valueOf(this.sResult, 2);
//
//            } else if (this.sFunction.equals(Instruction.BREAK)) {
//
//                this.format = "BREAK";
//
//            } else if (this.sFunction.equals(Instruction.SLL)) {// if(rd==rt==00000)NOP,else SLL
//                if (this.sOperand2.equals("00000") && this.sResult.equals("00000")) {
//
//                    this.format = "NOP";
//
//                } else {
//                    // this.format = "SLL\trd, rt, sa";
//                    this.format = String.format("SLL\tR%d, R%d, #%d", Integer.valueOf(this.sResult, 2),
//                            Integer.valueOf(this.sOperand2, 2), Integer.valueOf(this.sImm, 2));
//                    this.imm = Integer.valueOf(this.sImm, 2);
//                    this.rt = "R" + Integer.valueOf(this.sOperand2, 2);
//                    this.rd = "R" + Integer.valueOf(this.sResult, 2);
//                }
//            } else if (this.sFunction.equals(Instruction.SRL)) {
//                // this.format = "SRL rd, rt, sa";
//                this.format = String.format("SRL\tR%d, R%d, #%d", Integer.valueOf(this.sResult, 2),
//                        Integer.valueOf(this.sOperand2, 2), Integer.valueOf(this.sImm, 2));
//                this.imm = Integer.valueOf(this.sImm, 2);
//                this.rt = "R" + Integer.valueOf(this.sOperand2, 2);
//                this.rd = "R" + Integer.valueOf(this.sResult, 2);
//
//            } else if (this.sFunction.equals(Instruction.SRA)) {
//                // this.format = "SRA rd, rt, sa";
//                this.format = String.format("SRA\tR%d, R%d, #%d", Integer.valueOf(this.sResult, 2),
//                        Integer.valueOf(this.sOperand2, 2), Integer.valueOf(this.sImm, 2));
//                this.imm = Integer.valueOf(this.sImm, 2);
//                this.rt = "R" + Integer.valueOf(this.sOperand2, 2);
//                this.rd = "R" + Integer.valueOf(this.sResult, 2);
//
//            } else if (this.sFunction.equals(Instruction.AND)) {
//                // this.format = "AND rd, rs, rt"
//                this.format = String.format("AND\tR%d, R%d, R%d", Integer.valueOf(this.sResult, 2),
//                        Integer.valueOf(this.sOperand1, 2), Integer.valueOf(this.sOperand2, 2));
//                this.rs = "R" + Integer.valueOf(this.sOperand1, 2);
//                this.rt = "R" + Integer.valueOf(this.sOperand2, 2);
//                this.rd = "R" + Integer.valueOf(this.sResult, 2);
//
//            } else if (this.sFunction.equals(Instruction.NOR)) {
//                // this.format = "NOR rd, rs, rt"
//                this.format = String.format("NOR\tR%d, R%d, R%d", Integer.valueOf(this.sResult, 2),
//                        Integer.valueOf(this.sOperand1, 2), Integer.valueOf(this.sOperand2, 2));
//                this.rs = "R" + Integer.valueOf(this.sOperand1, 2);
//                this.rt = "R" + Integer.valueOf(this.sOperand2, 2);
//                this.rd = "R" + Integer.valueOf(this.sResult, 2);
//
//            } else if (this.sFunction.equals(Instruction.SLT)) {
//                // this.format = "SLT rd, rs, rt"
//                this.format = String.format("SLT\tR%d, R%d, R%d", Integer.valueOf(this.sResult, 2),
//                        Integer.valueOf(this.sOperand1, 2), Integer.valueOf(this.sOperand2, 2));
//                this.rs = "R" + Integer.valueOf(this.sOperand1, 2);
//                this.rt = "R" + Integer.valueOf(this.sOperand2, 2);
//                this.rd = "R" + Integer.valueOf(this.sResult, 2);
//
//            }
//        } else if (this.sOperation.equals(Instruction.J)) {
//            // this.format = "J target";
//            this.format = String.format("J\t#%d",
//                    Integer.valueOf(this.sOperand1 + this.sOperand2 + this.sResult + this.sImm + this.sFunction + "00", 2));
//            this.isBranch = true;
//            this.imm = Integer.valueOf(this.sOperand1 + this.sOperand2 + this.sResult + this.sImm + this.sFunction + "00", 2);
//        } else if (this.sOperation.equals(Instruction.BEQ)) {
//            // this.format = "BEQ rs, rt, offset";
//            this.format = String.format("BEQ\tR%d, R%d, #%d", Integer.valueOf(this.sOperand1, 2),
//                    Integer.valueOf(this.sOperand2, 2),
//                    Integer.valueOf(this.signExtend(this.sResult + this.sImm + this.sFunction + "00", 32), 2));
//            this.isBranch = true;
//            this.imm = Integer.valueOf(this.signExtend(this.sResult + this.sImm + this.sFunction + "00", 32), 2);
//            this.rs = "R" + Integer.valueOf(this.sOperand1, 2);
//            this.rt = "R" + Integer.valueOf(this.sOperand2, 2);
//        } else if (this.sOperation.equals(Instruction.BLTZ)) {
//            // this.format = "BLTZ rs, offset";
//            this.format = String.format("BLTZ\tR%d, #%d", Integer.valueOf(this.sOperand1, 2),
//                    Integer.valueOf(this.signExtend(this.sResult + this.sImm + this.sFunction + "00", 32), 2));
//            this.isBranch = true;
//            this.imm = Integer.valueOf(this.signExtend(this.sResult + this.sImm + this.sFunction + "00", 32), 2);
//            this.rs = "R" + Integer.valueOf(this.sOperand1, 2);
//        } else if (this.sOperation.equals(Instruction.BGTZ)) {
//            // this.format = "BGTZ rs, offset";
//            this.format = String.format("BGTZ\tR%d, #%d", Integer.valueOf(this.sOperand1, 2),
//                    Integer.valueOf(this.signExtend(this.sResult + this.sImm + this.sFunction + "00", 32), 2));
//            this.isBranch = true;
//            this.imm = Integer.valueOf(this.signExtend(this.sResult + this.sImm + this.sFunction + "00", 32), 2);
//            this.rs = "R" + Integer.valueOf(this.sOperand1, 2);
//        } else if (this.sOperation.equals(Instruction.SW)) {
//            // this.format = "SW rt, offset(base)";
//            this.format = String.format("SW\tR%d, %d(R%d)", Integer.valueOf(this.sOperand2, 2),
//                    Integer.valueOf(this.signExtend(this.sResult + this.sImm + this.sFunction, 32), 2), Integer.valueOf(this.sOperand1, 2));
//            this.rt = "R" + Integer.valueOf(this.sOperand2, 2);
//            this.imm = Integer.valueOf(this.signExtend(this.sResult + this.sImm + this.sFunction, 32), 2);
//            this.base = "R" + Integer.valueOf(this.sOperand1, 2);
//        } else if (this.sOperation.equals(Instruction.LW)) {
//            // this.format = "LW rt, offset(base)";
//            this.format = String.format("LW\tR%d, %d(R%d)", Integer.valueOf(this.sOperand2, 2),
//                    Integer.valueOf(this.signExtend(this.sResult + this.sImm + this.sFunction, 32), 2), Integer.valueOf(this.sOperand1, 2));
//            this.rt = "R" + Integer.valueOf(this.sOperand2, 2);
//            this.imm = Integer.valueOf(this.signExtend(this.sResult + this.sImm + this.sFunction, 32), 2);
//            this.base = "R" + Integer.valueOf(this.sOperand1, 2);
//        } else if (this.sOperation.equals(Instruction.MUL)) {
//            // this.format = "MUL rd, rs, rt";
//            this.format = String.format("MUL\tR%d, R%d, R%d", Integer.valueOf(this.sResult, 2),
//                    Integer.valueOf(this.sOperand1, 2), Integer.valueOf(this.sOperand2, 2));
//            this.rs = "R" + Integer.valueOf(this.sOperand1, 2);
//            this.rt = "R" + Integer.valueOf(this.sOperand2, 2);
//            this.rd = "R" + Integer.valueOf(this.sResult, 2);
//        }
//        // category 2
//        else if (this.sMSB.equals("1")) {
//            if (this.sOperation.equals("110000")) {// ADD
//                // this.format = "ADD rd, rs, #imm";
//                this.format = String.format("ADD\tR%d, R%d, #%d", Integer.valueOf(this.sOperand2, 2),
//                        Integer.valueOf(this.sOperand1, 2), Integer.valueOf(this.sResult + this.sImm + this.sFunction, 2));
//                this.imm = Integer.valueOf(this.sResult + this.sImm + this.sFunction, 2);
//                this.rd = "R" + Integer.valueOf(this.sOperand2, 2);
//                this.rs = "R" + Integer.valueOf(this.sOperand1, 2);
//            } else if (this.sOperation.equals("110001")) {// SUB
//                // this.format = "SUB rd, rs, #imm";
//                this.format = String.format("SUB\tR%d, R%d, #%d", Integer.valueOf(this.sOperand2, 2),
//                        Integer.valueOf(this.sOperand1, 2), Integer.valueOf(this.sResult + this.sImm + this.sFunction, 2));
//                this.imm = Integer.valueOf(this.sResult + this.sImm + this.sFunction, 2);
//                this.rd = "R" + Integer.valueOf(this.sOperand2, 2);
//                this.rs = "R" + Integer.valueOf(this.sOperand1, 2);
//            } else if (this.sOperation.equals("100001")) {// MUL
//                // this.format = "MUL rd, rs, rt";
//                this.format = String.format("MUL\tR%d, R%d, #%d", Integer.valueOf(this.sOperand2, 2),
//                        Integer.valueOf(this.sOperand1, 2), Integer.valueOf(this.sResult + this.sImm + this.sFunction, 2));
//                this.imm = Integer.valueOf(this.sResult + this.sImm + this.sFunction, 2);
//                this.rd = "R" + Integer.valueOf(this.sOperand2, 2);
//                this.rs = "R" + Integer.valueOf(this.sOperand1, 2);
//            } else if (this.sOperation.equals("110010")) {// AND
//                // this.format = "AND rd, rs, rt";
//                this.format = String.format("AND\tR%d, R%d, #%d", Integer.valueOf(this.sOperand2, 2),
//                        Integer.valueOf(this.sOperand1, 2), Integer.valueOf(this.sResult + this.sImm + this.sFunction, 2));
//                this.imm = Integer.valueOf(this.sResult + this.sImm + this.sFunction, 2);
//                this.rd = "R" + Integer.valueOf(this.sOperand2, 2);
//                this.rs = "R" + Integer.valueOf(this.sOperand1, 2);
//            } else if (this.sOperation.equals("110011")) {// NOR
//                // this.format = "NOR rd, rs, rt";
//                this.format = String.format("NOR\tR%d, R%d, #%d", Integer.valueOf(this.sOperand2, 2),
//                        Integer.valueOf(this.sOperand1, 2), Integer.valueOf(this.sResult + this.sImm + this.sFunction, 2));
//                this.imm = Integer.valueOf(this.sResult + this.sImm + this.sFunction, 2);
//                this.rd = "R" + Integer.valueOf(this.sOperand2, 2);
//                this.rs = "R" + Integer.valueOf(this.sOperand1, 2);
//            } else if (this.sOperation.equals("110101")) {// SLT
//                // this.format = "SLT rd, rs, r";
//                this.format = String.format("SLT\tR%d, R%d, #%d", Integer.valueOf(this.sOperand2, 2),
//                        Integer.valueOf(this.sOperand1, 2), Integer.valueOf(this.sResult + this.sImm + this.sFunction, 2));
//                this.imm = Integer.valueOf(this.sResult + this.sImm + this.sFunction, 2);
//                this.rd = "R" + Integer.valueOf(this.sOperand2, 2);
//                this.rs = "R" + Integer.valueOf(this.sOperand1, 2);
//            }
//        }
    }

    public String getFormat() {
        return this.format;
    }

    public String signExtend(String string, int length) {
        char[] chars = new char[length];
        int size = string.length();
        for (int i = 0; i < size; i++) {
            chars[length - 1 - i] = string.charAt(size - 1 - i);
        }
        for (int i = size; i < length; i++) {
            chars[i - size] = string.charAt(0);
        }
        return new String(chars);
    }

    public String unSignExtend(String string, int length) {
        char[] chars = new char[length];
        int size = string.length();
        for (int i = 0; i < size; i++) {
            chars[length - 1 - i] = string.charAt(size - 1 - i);
        }
        for (int i = size; i < length; i++) {
            chars[i - size] = '0';
        }
        return new String(chars);
    }
}
