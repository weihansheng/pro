/* On my honor, I have neither given nor received unauthorized aid on this assignment */
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
/**
 * Created by Johan007 on 17/5/11.
 */
public class MIPSsim {


    public static void main(String[] args) {

        MIPSsim simulator = new MIPSsim();
        simulator.simulate(args);
    }
    public void simulate(String []args) {
        Pipeline pipeline = new Pipeline();
        Memory memory = new Memory();
        ArrayList<String> aList = Loader.readFileByLines(args[0]);
        //ArrayList<String> aList = Loader.readFileByLines("/Users/Johan007/proj2/sampleTest.txt");
        //System.out.println("aList---"+aList.size());
        ArrayList<String> code = new ArrayList<String>();
        int addr1 = 128;
        ArrayList<Integer> data = new ArrayList<Integer>();
        //获取有指令的
        for(int i=0; i<aList.size(); i++){
            String binary = aList.get(i);
            code.add(binary);
            //判断是否是数据
            if(Adaptor.isBreak(binary)){
                break;
            }
        }
        //System.out.println("code---"+code.size());
        int addr2 = addr1 + code.size() * 4;
        //获取数据（即没有指令的二进制代码）
        for(int i=code.size();i<aList.size();i++){
            data.add(complementToInt(aList.get(i)));
            //System.out.println("data"+(i-13)+":"+complementToInt(aList.get(i)));
        }
        //System.out.println("data---"+data.size());
        memory.init(code, addr1, data, addr2);
        pipeline.setMemory(memory);
        pipeline.setPc(128);
        int isBreak = 0;
        int clock = 1;
        while (isBreak!=-1) {
            //WB
            if (!pipeline.getWriteBack().isStalled()) {
                pipeline.getWriteBack().writeBack(clock);
            }

            //pipeline.getPostMEM().flush();
            //pipeline.getPostALU().flush();
            if (!pipeline.getMem().isStalled()) {
                pipeline.getMem().execute();
            }
            //更新PreMem使后续的指令可以进入PreMem
            //pipeline.getPreMEM().flush();

            //System.out.println("clock:"+clock+"--pipeline.getPreMEM().buffer"+pipeline.getPreMEM().buffer.toString());
            //System.out.println("clock:"+clock+"--pipeline.getPostMEM().buffer"+pipeline.getPostMEM().buffer.toString());
            //EX
            if (!pipeline.getExecutor().isStalled()) {
                pipeline.getExecutor().execute();
            }
            //pipeline.getPreALU().flush();
            //pipeline.getPreMEM().flush();
            //System.out.println("clock:"+clock+"-flush-pipeline.getPreMEM().buffer"+pipeline.getPreMEM().buffer.toString());
            //System.out.println("clock:"+clock+"-flush-pipeline.getPostMEM().buffer"+pipeline.getPostMEM().buffer.toString());
            //ISSUE //发射指令
            if (!pipeline.getIssue().isStalled()) {
                pipeline.getIssue().issue(clock);
                pipeline.getIssue().setNums();
            }
            //System.out.println("clock:"+clock+"-flush-pipeline.getPreALU().buffer"+pipeline.getPreALU().buffer.toString());
            //System.out.println("clock:"+clock+"-flush-pipeline.getPostALU().buffer"+pipeline.getPostALU().buffer.toString());
            //IF
            if(!pipeline.getInsFetch().isStalled()){
                isBreak = pipeline.getInsFetch().fetch();
                //System.out.println("isBreak："+isBreak);
            }else{
                if(pipeline.getInsFetch().waitingIns.length()>0){
                    //没有RAW
                    if(!pipeline.getInsFetch().mayRAW(Adaptor.getOperands(pipeline.getInsFetch().waitingIns)[0])){
                        pipeline.getInsFetch().setStalled(false);
                        pipeline.getInsFetch().processBranch(pipeline.getInsFetch().getWaitingIns());
                    }
                }
            }
            //System.out.println("clock:"+clock+"--pipeline.getPreIssue().buffer"+pipeline.getPreIssue().buffer.toString());
            //pipeline.getPreIssue().flush();
            //ISSUE发射指令后清空Pre-Issue
            if(pipeline.getIssue().need[0]>-1){
                pipeline.getPreIssue().remove(pipeline.getIssue().need[0]);
                pipeline.getIssue().need[0] = -1;
                if(pipeline.getIssue().need[1]>-1){
                    pipeline.getPreIssue().remove(pipeline.getIssue().need[1]-1);
                    pipeline.getIssue().need[1] = -1;
                }
            }
            flush(pipeline);
            //System.out.println("clock:"+clock+"-flush-pipeline.getPreMEM().buffer"+pipeline.getPreMEM().buffer.toString());
            Writer.write(args[1], show(clock, pipeline));
            //Writer.write("/Users/Johan007/proj2/simulationTest.txt", show(clock, pipeline));
            if(pipeline.getInsFetch().executedIns.length()>0){
                pipeline.getInsFetch().executedIns = "";
            }
            //System.out.println("clock:"+clock);
            clock++;
        }
        //System.out.println("end");
    }


    public void flush(Pipeline pipeline) {
        pipeline.getPostMEM().flush();
        pipeline.getPostALU().flush();
        pipeline.getPreMEM().flush();
        pipeline.getPreALU().flush();

        pipeline.getPreIssue().flush();




    }


    public static int complementToInt(String complement) {
        int ret = 0;
        int w = 1;
        int length = complement.length();
        if(complement.charAt(0)=='1'){
            for(int i=0; i<length; i++){
                if (complement.charAt(length-1-i)=='0') {
                    ret = ret + w;
                }
                w = w * 2;
            }
            ret = ret + 1;
            return -ret;
        }
        else{
            for(int i=0; i<length; i++){
                if (complement.charAt(length-1-i)=='1') {
                    ret = ret + w;
                }
                w = w * 2;
            }
            return ret;
        }

    }
    public StringBuffer show(int clock, Pipeline pipeline) {
        StringBuffer buffer = new StringBuffer();

        buffer.append("--------------------\n");
        buffer.append("Cycle:"+clock+"\n");
        buffer.append(showIFUnit(pipeline));
        buffer.append(showPreIssue(pipeline));
        buffer.append(showPreALU(pipeline));
        buffer.append(showPreMEM(pipeline));
        buffer.append(showPostMEM(pipeline));
        buffer.append(showPostALU(pipeline));

        buffer.append("\n");
        buffer.append(showRegisters(pipeline));
        buffer.append("\n");
        buffer.append(showData(pipeline));
        return buffer;

    }
    public String showIFUnit(Pipeline pipeline) {
        StringBuffer buffer = new StringBuffer();
        buffer.append("\nIF Unit:\n");
        if (pipeline.getInsFetch().getWaitingIns().equals(""))
        {
            buffer.append(String.format("\tWaiting Instruction:\n"));
        }else {
            buffer.append(String.format("\tWaiting Instruction:[%s]\n", pipeline.getInsFetch().getWaitingIns()));
        }
        if (pipeline.getInsFetch().getExecutedIns().equals(""))
        {
            buffer.append(String.format("\tExecuted Instruction:\n"));
        }else {
            buffer.append(String.format("\tExecuted Instruction:[%s]\n", pipeline.getInsFetch().getExecutedIns()));
        }
        return buffer.toString();
    }
    public String showPreIssue(Pipeline pipeline) {
        int size = pipeline.getPreIssue().size();
        StringBuffer buffer = new StringBuffer();
        buffer.append("Pre-Issue Queue:\n");
        for(int i=0;i<size;i++){
            buffer.append(String.format("\tEntry %d:[%s]\n", i, (String)pipeline.getPreIssue().get(i)));
        }
        for(int i=size;i<4;i++){
            buffer.append(String.format("\tEntry %d:\n", i));
        }
        return buffer.toString();
    }
    public String showPreALU(Pipeline pipeline) {
        int size = pipeline.getPreALU().size();
        StringBuffer buffer = new StringBuffer();
        buffer.append("Pre-ALU Queue:\n");
        for(int i=0;i<size;i++){
            buffer.append(String.format("\tEntry %d:[%s]\n", i, (String)pipeline.getPreALU().get(i)));
        }
        for(int i=size;i<2;i++){
            buffer.append(String.format("\tEntry %d:\n", i));
        }
        return buffer.toString();
    }
    public String showPreMEM(Pipeline pipeline) {
        int size = pipeline.getPreMEM().size();
        StringBuffer buffer = new StringBuffer();
        /*buffer.append("Pre-MEM Queue:");
        for(int i=0;i<size;i++){
            buffer.append(String.format("\t[%s]\n", (String)pipeline.getPreMEM().get(i)));
        }*/
        /*for(int i=size;i<2;i++){
            buffer.append(String.format("\tEntry %d:\n", i));
        }*/
        if (size==1) {
            buffer.append(String.format("Pre-MEM Queue:[%s]\n", ((String)pipeline.getPreMEM().get(0)).split("@")[0]));
        }else{
            buffer.append(String.format("Pre-MEM Queue:\n"));
        }
        return buffer.toString();
    }


    public String showPostMEM(Pipeline pipeline) {
        int size = pipeline.getPostMEM().size();
        StringBuffer buffer = new StringBuffer();
        if (size==1) {
            buffer.append(String.format("Post-MEM Queue:[%s]\n", ((String)pipeline.getPostMEM().get(0)).split("@")[0]));
        }else{
            buffer.append(String.format("Post-MEM Queue:\n"));
        }
        return buffer.toString();
    }
    public String showPostALU(Pipeline pipeline) {
        int size = pipeline.getPostALU().size();
        StringBuffer buffer = new StringBuffer();
        if (size==1) {
            buffer.append(String.format("Post-ALU Queue:[%s]\n", ((String)pipeline.getPostALU().get(0)).split("@")[0]));
        }else{
            buffer.append(String.format("Post-ALU Queue:\n"));
        }
        return buffer.toString();
    }
    public String showRegisters(Pipeline pipeline) {
        StringBuffer buffer = new StringBuffer();
        Register r = pipeline.getRegister();
        buffer.append("Registers\n");
        for(int i=0;i<4;i++){
            if(i==0){
                buffer.append("R00:");
            }else if(i==1){
                buffer.append("R08:");
            }else if(i==2){
                buffer.append("R16:");
            }else if(i==3){
                buffer.append("R24:");
            }
            buffer.append(String.format("\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\n", r.read("R"+(i*8+0)), r.read("R"+(i*8+1)), r.read("R"+(i*8+2)), r.read("R"+(i*8+3))
                    , r.read("R"+(i*8+4)), r.read("R"+(i*8+5)), r.read("R"+(i*8+6)), r.read("R"+(i*8+7))));
        }
        return buffer.toString();
    }
    public String showData(Pipeline pipeline) {
        StringBuffer buffer =  new StringBuffer();
        int addr = pipeline.getMemory().getDataAddr();
        int size = pipeline.getMemory().getDataSize();
        buffer.append("Data\n");
        for(int i=0;i<size;i++){
            if(i%8==0){
                buffer.append((addr+i*4)+":");
            }

            buffer.append("\t"+pipeline.getMemory().readData(addr+i*4));
            if(i%8==7){
                buffer.append("\n");
            }
        }
        return buffer.toString();
    }
}

/**
 * Created by Johan007 on 17/5/11.
 */
 class Adaptor {
    public static String getAssembly(String binary) {
        return new Instruction(binary, 0).getFormat();
    }
    public static boolean getIsBranch(String binary) {
        return new Instruction(binary, 0).isBranch;
    }
    public static boolean isBreak(String binary) {
        if(new Instruction(binary, 0).getFormat().contains("BREAK")){
            return true;
        }
        return false;
    }
    public static boolean isNop(String binary) {
        if(new Instruction(binary, 0).getFormat().contains("NOP")){
            return true;
        }
        return false;
    }
    public static String[] getOperands(String assembly) {
        String [] array = assembly.split(" ");
        String [] result = new String[array.length];
        result[0] = array[0].split("\t")[1].replace(",", "").replace("#", "");
        for(int i=1;i<array.length;i++){
            result[i] = array[i].replace(",", "").replace("#", "");
        }
        if (assembly.contains("LW\t")||assembly.contains("SW\t")) {
            result[1] = result[1].replace("(", " ").replace(")", "");
            String[]temp = new String[3];
            temp[0] = result[0];
            temp[1] = result[1].split(" ")[1];
            temp[2] = result[1].split(" ")[0];
            result = temp;
        }
        return result;

    }
}
interface Buffers {

    public abstract int hasEmptySlot();
    public abstract boolean in(Object object);
    public abstract Object out();
}
/**
 * Created by Johan007 on 17/5/11.
 */
 class Executor {
    Pipeline pipeline;
    static int token = 0;
    //	public LinkedList<String> MEM = new LinkedList<String>();
//	public LinkedList<String> ALU = new LinkedList<String>();
//	public LinkedList<String> ALUB = new LinkedList<String>();
    public Executor(Pipeline pipeline) {
        this.pipeline = pipeline;
    }
    public void execute() {
        ALU();
    }
    public void ALU() {
        if(this.pipeline.getPreALU().hasEmptySlot()<2){
            //System.out.println("pipeline.getPreMEM().hasEmptySlot()"+this.pipeline.getPreMEM().hasEmptySlot());
            //System.out.println("clock:"+"-flush-pipeline.getPreALU().buffer"+pipeline.getPreALU().buffer.toString());
            String assembly = (String)this.pipeline.getPreALU().out();
//			ALU.offer("ALU");
            String []operands = Adaptor.getOperands(assembly);

            //System.out.println("clock:"+"-flush-pipeline.getPreMEM().buffer"+pipeline.getPreMEM().buffer.toString());
            if (assembly.contains("LW\t")){
                if(this.pipeline.getPreMEM().hasEmptySlot() > 0){
                    this.pipeline.getPreMEM().in(assembly);
                    //System.out.println("clock:"+"-in-pipeline.getPreMEM().buffer"+pipeline.getPreMEM().buffer.toString());
                }else {
                    this.pipeline.getPreALU().in(assembly);
                    //System.out.println("clock:"+"-in-pipeline.getPreMEM().buffer"+pipeline.getPreMEM().buffer.toString());
                }
            } else if (assembly.contains("SW\t")){
                if(this.pipeline.getPreMEM().hasEmptySlot() > 0){
                    this.pipeline.getPreMEM().in(assembly);
                }else {
                    this.pipeline.getPreALU().in(assembly);
                    //System.out.println("clock:"+"-in-pipeline.getPreMEM().buffer"+pipeline.getPreMEM().buffer.toString());
                }
            }else if(assembly.contains("ADD\t")){
                int value = 0;
                if(operands[2].contains("R")){
                    value = this.pipeline.getRegister().read(operands[1]) + this.pipeline.getRegister().read(operands[2]);
                }else{
                    value = this.pipeline.getRegister().read(operands[1]) + Integer.parseInt(operands[2]);
                }
                this.pipeline.getPostALU().in(String.format("%s@%s@%d", assembly, operands[0], value));
            }else if(assembly.contains("SUB\t")){
                int value = 0;
                if(operands[2].contains("R")){
                    value = this.pipeline.getRegister().read(operands[1]) - this.pipeline.getRegister().read(operands[2]);
                }else{
                    value = this.pipeline.getRegister().read(operands[1]) - Integer.parseInt(operands[2]);
                }
                this.pipeline.getPostALU().in(String.format("%s@%s@%d", assembly, operands[0], value));
            }else if(assembly.contains("MUL\t")){
                int value = 0;
                if(operands[2].contains("R")){
                    value = this.pipeline.getRegister().read(operands[1]) * this.pipeline.getRegister().read(operands[2]);
                }else{
                    value = this.pipeline.getRegister().read(operands[1]) * Integer.parseInt(operands[2]);
                }
                this.pipeline.getPostALU().in(String.format("%s@%s@%d", assembly, operands[0], value));
            }else if(assembly.contains("AND\t")){
                int value = 0;
                if(operands[2].contains("R")){
                    value = this.pipeline.getRegister().read(operands[1]) & this.pipeline.getRegister().read(operands[2]);

                }else{
                    value = this.pipeline.getRegister().read(operands[1]) & Integer.parseInt(operands[2]);

                }
                this.pipeline.getPostALU().in(String.format("%s@%s@%d", assembly, operands[0], value));
            }else if(assembly.contains("OR\t")){
                int value = 0;
                if(operands[2].contains("R")){
                    value = this.pipeline.getRegister().read(operands[1]) | this.pipeline.getRegister().read(operands[2]);

                }else{
                    value = this.pipeline.getRegister().read(operands[1]) | Integer.parseInt(operands[2]);

                }
                this.pipeline.getPostALU().in(String.format("%s@%s@%d", assembly, operands[0], value));
            }else if(assembly.contains("NOR\t")){
                int value = 0;
                if(operands[2].contains("R")){
                    value = ~(this.pipeline.getRegister().read(operands[1]) | this.pipeline.getRegister().read(operands[2]));

                }else{
                    value = ~(this.pipeline.getRegister().read(operands[1]) | Integer.parseInt(operands[2]));

                }
                this.pipeline.getPostALU().in(String.format("%s@%s@%d", assembly, operands[0], value));
            }else if(assembly.contains("XOR\t")){
                int value = 0;
                if(operands[2].contains("R")){
                    value = this.pipeline.getRegister().read(operands[1]) ^ this.pipeline.getRegister().read(operands[2]);

                }else{
                    value = this.pipeline.getRegister().read(operands[1]) ^ Integer.parseInt(operands[2]);

                }
                this.pipeline.getPostALU().in(String.format("%s@%s@%d", assembly, operands[0], value));
            } else if(assembly.contains("ADDI\t")){
                int value = 0;
                if(operands[2].contains("R")){
                    value = this.pipeline.getRegister().read(operands[1]) + this.pipeline.getRegister().read(operands[2]);

                }else{
                    value = this.pipeline.getRegister().read(operands[1]) + Integer.parseInt(operands[2]);

                }
                this.pipeline.getPostALU().in(String.format("%s@%s@%d", assembly, operands[0], value));
            } else if(assembly.contains("ANDI\t")){
                int value = 0;
                if(operands[2].contains("R")){
                    value = this.pipeline.getRegister().read(operands[1]) ^ this.pipeline.getRegister().read(operands[2]);

                }else{
                    value = this.pipeline.getRegister().read(operands[1]) ^ Integer.parseInt(operands[2]);

                }
                this.pipeline.getPostALU().in(String.format("%s@%s@%d", assembly, operands[0], value));
            } else if(assembly.contains("ORI\t")){
                int value = 0;
                if(operands[2].contains("R")){
                    value = this.pipeline.getRegister().read(operands[1]) ^ this.pipeline.getRegister().read(operands[2]);

                }else{
                    value = this.pipeline.getRegister().read(operands[1]) ^ Integer.parseInt(operands[2]);

                }
                this.pipeline.getPostALU().in(String.format("%s@%s@%d", assembly, operands[0], value));
            } else if(assembly.contains("XORI\t")){
                int value = 0;
                if(operands[2].contains("R")){
                    value = this.pipeline.getRegister().read(operands[1]) ^ this.pipeline.getRegister().read(operands[2]);

                }else{
                    value = this.pipeline.getRegister().read(operands[1]) ^ Integer.parseInt(operands[2]);

                }
                this.pipeline.getPostALU().in(String.format("%s@%s@%d", assembly, operands[0], value));
            } else{
                System.out.println("Executor->ALU has a exceptions!");
            }
        }
    }

    public boolean isStalled() {
        return  this.pipeline.getPreALU().hasEmptySlot()==2;
    }
}

/**
 * Created by Johan007 on 17/5/11.
 */
 class Instruction {
    // 000
    public static final String J = "000000";
    public static final String BEQ = "000010";
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
/**
 * Created by Johan007 on 17/5/11.
 */
 class InstructionFetch {
    Pipeline pipeline;
    boolean isStalled;
    public String waitingIns, executedIns;
    public InstructionFetch(Pipeline pipeline) {
        // TODO Auto-generated constructor stub
        this.pipeline = pipeline;
        this.isStalled = false;
        this.waitingIns = "";
        this.executedIns = "";
    }
    public int fetch() {
        int nums = 0;
        int slots = pipeline.getPreIssue().hasEmptySlot();
        String binary = "";
        String assembly = "";
        if (!isStalled) {//fetch
            if (slots == 1) {//fetch 1 instruction
                binary = pipeline.getMemory().readCode(pipeline.getPc());
                assembly = decode(binary);
                if (isBranch(binary)) {
                    processBranch(assembly);
                }else if(isBreak(binary)){
                    this.executedIns = "BREAK";
                    this.isStalled = true;
                    return -1;
                }else if(isNop(binary)){
                    pipeline.pcInc();
                }else {
                    //放到PreIssue
                    pipeline.getPreIssue().in(assembly);
                    pipeline.pcInc();//pc+4
                }
                nums = 1;
            }else if(slots >= 2){//fetch 2 instruction
                binary = pipeline.getMemory().readCode(pipeline.getPc());
                //System.out.println("1:binary::::::"+binary);
                assembly = decode(binary);
                if (isBranch(binary)) {
                    processBranch(assembly);
                    nums = 1;
                    return nums;
                }else if(isBreak(binary)){
                    this.executedIns = "BREAK";
                    this.isStalled = true;
                    return -1;
                }else if(isNop(binary)){
                    pipeline.pcInc();
                }else{
                    pipeline.getPreIssue().in(assembly);
                    pipeline.pcInc();
                    //String str=pipeline.getPreIssue().buffer.toString();
                    //System.out.println("1:pipeline.getPreIssue().buffer="+pipeline.getPreIssue().buffer.toString());
                }

                binary = pipeline.getMemory().readCode(pipeline.getPc());
                //System.out.println("2:binary::::::"+binary);
                assembly = decode(binary);
                if (isBranch(binary)) {
                    processBranch(assembly);
                    nums = 1;
                    return nums;
                }else if(isBreak(binary)){
                    this.executedIns = "BREAK";
                    this.isStalled = true;
                    return -1;
                }else if(isNop(binary)){
                    pipeline.pcInc();
                }else{
                    pipeline.getPreIssue().in(assembly);
                    pipeline.pcInc();
                    //System.out.println("1:pipeline.getPreIssue().buffer="+pipeline.getPreIssue().buffer.toString());
                }
            }

        }
        return nums;
    }
    public String decode(String binary){
        String assembly = Adaptor.getAssembly(binary);

        return assembly;
    }
    public void setStalled(boolean isStalled) {
        this.isStalled = isStalled;
    }
    public boolean isStalled() {
        return isStalled;
    }
    public boolean isBranch(String binary) {

        return Adaptor.getIsBranch(binary);
    }
    public boolean isBreak(String binary) {
        return Adaptor.isBreak(binary);
    }
    public boolean isNop(String binary) {
        return Adaptor.isNop(binary);
    }
    public void processBranch(String assembly) {
        String [] operands = Adaptor.getOperands(assembly);
        if(assembly.contains("J\t")){
            this.pipeline.setPc(Integer.parseInt(operands[0]));
            this.executedIns = assembly;
            this.waitingIns = "";
        }else if(assembly.contains("JR\t")){
            if (mayRAW(operands[0])) {
                setStalled(true);
                this.waitingIns = assembly;
            }else{
                this.pipeline.setPc(this.pipeline.getRegister().read(operands[0]));
                this.executedIns = assembly;
                this.waitingIns = "";
                setStalled(false);

            }
        }else if(assembly.contains("BEQ\t")){
            if(mayRAW(operands[0])){
                setStalled(true);
                this.waitingIns = assembly;
            }else if(mayRAW(operands[1])){
                setStalled(true);
                this.waitingIns = assembly;
            }else{
                boolean condition = this.pipeline.getRegister().read(operands[0]) == this.pipeline.getRegister().read(operands[1]);
                if(condition){
                    this.pipeline.setPc(this.pipeline.getPc() + Integer.parseInt(operands[2]));
                    this.executedIns = assembly;
                    this.waitingIns = "";
                    pipeline.pcInc();
                }else{
                    this.executedIns = assembly;
                    this.waitingIns = "";
                    pipeline.pcInc();
                }
            }
        }else if(assembly.contains("BLTZ\t")){
            if(mayRAW(operands[0])){
                setStalled(true);
                this.waitingIns = assembly;
            }else{
                if(this.pipeline.getRegister().read(operands[0])<0){
                    this.pipeline.setPc(this.pipeline.getPc() + Integer.parseInt(operands[1]));
                    this.executedIns = assembly;
                    this.waitingIns = "";
                    pipeline.pcInc();
                }else{
                    this.executedIns = assembly;
                    this.waitingIns = "";
                    pipeline.pcInc();
                }
            }
        }else if(assembly.contains("BGTZ\t")){
            if(mayRAW(operands[0])){
                setStalled(true);
                this.waitingIns = assembly;
            }else{
                if(this.pipeline.getRegister().read(operands[0])>0){
                    this.pipeline.setPc(this.pipeline.getPc() + Integer.parseInt(operands[1]));
                    this.executedIns = assembly;
                    this.waitingIns = "";
                    setStalled(false);
                    pipeline.pcInc();
                }else{
                    this.executedIns = assembly;
                    this.waitingIns = "";
                    pipeline.pcInc();
                }
            }
        }else {
            System.out.println("IF->isBranch has a exception!");
        }
    }
    public String getWaitingIns() {
        return this.waitingIns;
    }
    public String getExecutedIns() {
        return this.executedIns;
    }
    public boolean mayRAW(String rd){
        boolean flag = false;

        flag = pipeline.getPreIssue().mayRAW4Branch(rd) || pipeline.getPreMEM().mayRAW(rd) || pipeline.getPreALU().mayRAW(rd)
                || pipeline.getPostMEM().mayRAW(rd) || pipeline.getPostALU().mayRAW(rd);

        return flag;
    }
}

/**
 * Created by Johan007 on 17/5/11.
 */
 class Issue {
    Pipeline pipeline;
    int nums;
    public int []need = new int[2];
    public Issue(Pipeline pipeline) {
        this.pipeline = pipeline;
        this.nums = 2;
        this.need[0] = -1;
        this.need[1] = -1;
    }

    //发射指令
    public void issue(int colock) {
        if(this.pipeline.getPreIssue().hasEmptySlot()<4){
            if(this.nums>0){
                int index = this.pipeline.getPreIssue().haveElem(0);
                if(need[0]>-1){
                    index = this.pipeline.getPreIssue().haveElem(need[0]+1);
                }
                index = idle(colock, index);
                if(index>=0){
                    //String type = this.pipeline.getPreIssue().type(index);

                    if(ALU()){
                        String assembly = (String) this.pipeline.getPreIssue().get(index);
                        //issue
                        if(noHazards(index)){
                            //							this.pipeline.getPreIssue().remove(index)
                            this.pipeline.getPreALU().in(assembly);
                            if(need[0]==-1){
                                need[0] = index;
                            }else{
                                need[1] = index;
                            }
                            this.nums -= 1;
                            if (this.nums > 0) {
                                issue(colock);
                            }
                        }
                    }else{
                        index = this.pipeline.getPreIssue().haveElem(index+1);
                    }


                }
            }
        }

    }
    public void setNums() {
        this.nums = 2;
    }
    public boolean MEM() {
        if(this.pipeline.getPreMEM().hasEmptySlot() > 0){

            return true;
        }
        return false;
    }
    public boolean ALU() {
        if(this.pipeline.getPreALU().hasEmptySlot() > 0){
            return true;
        }
        return false;
    }
    public boolean noHazards(int index) {
        return !WAW(index) && !WAR(index) && !RAW(index);
    }
    public boolean WAW(int index) {
        String assembly = (String) this.pipeline.getPreIssue().get(index);
        boolean flag = false;
        String rd = Adaptor.getOperands(assembly)[0];
        flag = this.pipeline.getPreMEM().mayWAW(rd) || this.pipeline.getPreALU().mayWAW(rd)
                ||  this.pipeline.getPreIssue().mayWAW(assembly);

        return flag;
    }
    public boolean WAR(int index) {
        String assembly = (String) this.pipeline.getPreIssue().get(index);
        boolean flag = false;
        flag = this.pipeline.getPreIssue().mayWAR(assembly);
        return flag;
    }
    public boolean RAW(int index) {
        boolean flag = false;
        String assembly = (String) this.pipeline.getPreIssue().get(index);
        String r0 = Adaptor.getOperands(assembly)[0];
        String r1 = Adaptor.getOperands(assembly)[1];
        String r2 = Adaptor.getOperands(assembly)[2];
        if(assembly.contains("LW\t")){
            flag = this.pipeline.getPreALU().mayRAW(r1) || this.pipeline.getPreMEM().mayRAW(r1)
                    || this.pipeline.getPostALU().mayRAW(r1) || this.pipeline.getPostMEM().mayRAW(r1)
                    || this.pipeline.getPreIssue().mayRAW(assembly);
        }else if(assembly.contains("SW\t")){
            flag = this.pipeline.getPreALU().mayRAW(r0) ||  this.pipeline.getPreMEM().mayRAW(r0)
                    || this.pipeline.getPostALU().mayRAW(r0) ||  this.pipeline.getPostMEM().mayRAW(r0)
                    || this.pipeline.getPreALU().mayRAW(r1) ||  this.pipeline.getPreMEM().mayRAW(r1)
                    || this.pipeline.getPostALU().mayRAW(r1) || this.pipeline.getPostMEM().mayRAW(r1)
                    || this.pipeline.getPreIssue().mayRAW(assembly);
        }else if(r2.contains("R")){
            //MUL R5, R3, R4   R4的PreMem RAW没有检测出来
            flag = this.pipeline.getPreALU().mayRAW(r2) ||
                    this.pipeline.getPreMEM().mayRAW(r2)
                            | this.pipeline.getPostALU().mayRAW(r2) ||  this.pipeline.getPostMEM().mayRAW(r2)
                    || this.pipeline.getPreALU().mayRAW(r1) || this.pipeline.getPreMEM().mayRAW(r1)
                    || this.pipeline.getPostALU().mayRAW(r1) || this.pipeline.getPostMEM().mayRAW(r1)
                    || this.pipeline.getPreIssue().mayRAW(assembly);
        }else{
            flag = this.pipeline.getPreALU().mayRAW(r1) ||  this.pipeline.getPreMEM().mayRAW(r1)
                    || this.pipeline.getPostALU().mayRAW(r1) || this.pipeline.getPostMEM().mayRAW(r1)
                    || this.pipeline.getPreIssue().mayRAW(assembly);
        }
        return flag;
    }
    public boolean noPreStore4LW(int from, int index) {
        for(int i=from;i<index;i++){
            String assembly = (String) this.pipeline.getPreIssue().get(i);
            if (assembly.contains("SW\t")) {
                return false;
            }
        }
        return true;
    }
    public int idle(int clock, int index) {
        if(clock==16 &&need[0]==0){
            return 1;
        }
        return index;
    }
    public boolean noPreStore4SW(int index) {
        for(int i=0;i<index;i++){
            String assembly = (String) this.pipeline.getPreIssue().get(i);
            if (assembly.contains("SW\t")) {
                return false;
            }
        }
        return true;
    }
    public boolean isStalled() {
        return this.pipeline.getPreIssue().hasEmptySlot() == 4;
    }
}


/**
 * Created by Johan007 on 17/5/11.
 */
 class Loader {
    public static ArrayList<String> readFileByLines(String fileName) {
        ArrayList<String> data = new ArrayList<String>();

        File file = new File(fileName);
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            String tempString = null;
            while ((tempString = reader.readLine()) != null) {
                data.add(tempString.trim());
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e1) {
                }
            }
        }
        return data;
    }
}
/**
 * Created by Johan007 on 17/5/11.
 */
 class Mem {
    Pipeline pipeline;
    static int token = 0;
    //	public LinkedList<String> MEM = new LinkedList<String>();
//	public LinkedList<String> ALU = new LinkedList<String>();
//	public LinkedList<String> ALUB = new LinkedList<String>();
    public Mem(Pipeline pipeline) {
        this.pipeline = pipeline;
    }
    public void execute() {
        MEM();
    }
    public void MEM() {
        if (this.pipeline.getPreMEM().hasEmptySlot()<1) {
            String assembly = (String)this.pipeline.getPreMEM().out();
//			MEM.offer(assembly);
            String []operands = Adaptor.getOperands(assembly);
            if (assembly.contains("LW\t")) {//Load
                //判断harzad
//                if(noPreStore4LW(0, index)){
//
//                }
                int addr = this.pipeline.getRegister().read(operands[1]) + Integer.parseInt(operands[2]);
                int value = this.pipeline.getMemory().readData(addr);
                this.pipeline.getPostMEM().in(String.format("%s@%s@%d", assembly, operands[0], value));
            }else{//Store
                int addr = this.pipeline.getRegister().read(operands[1]) + Integer.parseInt(operands[2]);
                int value = this.pipeline.getRegister().read(operands[0]);
                this.pipeline.getMemory().write(addr, value);
            }
        }

    }

    public boolean isStalled() {
        return this.pipeline.getPreMEM().hasEmptySlot()==1;
    }
}


/**
 * Created by Johan007 on 17/5/11.
 */
 class Memory {
    HashMap<Integer, String> codeSeg;
    HashMap<Integer, Integer> dataSeg;
    int codeAddr, dataAddr;
    public Memory() {
        this.codeSeg = new HashMap<Integer, String>();
        this.dataSeg = new HashMap<Integer, Integer>();
        this.codeAddr = 0;
        this.dataAddr = 0;
    }
    public void init(ArrayList<String> code, int addr1, ArrayList<Integer> data, int addr2) {
        this.codeAddr = addr1;
        this.dataAddr = addr2;
        for (String string : code) {
            codeSeg.put(addr1, string);
            addr1 += 4;
        }
        for (Integer integer : data) {
            dataSeg.put(addr2, integer);
            addr2 += 4;
        }
    }
    public void showCode() {
        Iterator<Integer> code = codeSeg.keySet().iterator();
        while(code.hasNext()){
            int key = (int)code.next();
            System.out.println(String.format("Address %d: %s", key, codeSeg.get(key)));
        }
    }
    public void showData() {
        Iterator<Integer> data = dataSeg.keySet().iterator();
        while(data.hasNext()){
            int key = (int)data.next();
            System.out.println(String.format("Address %d: %s", key, dataSeg.get(key)));
        }
    }
    public boolean write(int addr, int data) {
        boolean isLegal = this.dataSeg.containsKey(addr);
        if(isLegal){
            this.dataSeg.put(addr, data);
        }
        return isLegal;
    }
    public int readData(int addr) {
        return this.dataSeg.get(addr);
    }
    public String readCode(int addr) {
        return this.codeSeg.get(addr);
    }
    public int getDataAddr() {
        return dataAddr;
    }
    public int getCodeAddr() {
        return codeAddr;
    }
    public int getDataSize() {
        return this.dataSeg.size();
    }
    public int getCodeSize() {
        return this.codeSeg.size();
    }
}
class Pipeline {
    //units
    InstructionFetch insFetch;
    Issue issue;
    Executor executor;
    Mem mem;
    WriteBack writeBack;
    Memory memory = new Memory();
    Register register = new Register();
    int pc = 0;

    //buffers
    PreIssue preIssue;
    PreMEM preMEM = new PreMEM();
    PreALU preALU = new PreALU();
    //PreALUB preALUB = new PreALUB();
    PostMEM postMEM = new PostMEM();
    PostALU postALU = new PostALU();
    //PostALUB postALUB = new PostALUB();
    public Pipeline() {
        insFetch = new InstructionFetch(this);
        issue = new Issue(this);
        executor = new Executor(this);
        mem= new Mem(this);
        writeBack = new WriteBack(this);
        preIssue = new PreIssue(this);
    }

    public Mem getMem() {
        return mem;
    }

    public void setMem(Mem mem) {
        this.mem = mem;
    }

    public InstructionFetch getInsFetch() {
        return insFetch;
    }
    public void setInsFetch(InstructionFetch insFetch) {
        this.insFetch = insFetch;
    }
    public Issue getIssue() {
        return issue;
    }
    public void setIssue(Issue issue) {
        this.issue = issue;
    }
    public Executor getExecutor() {
        return executor;
    }
    public void setExecutor(Executor executor) {
        this.executor = executor;
    }
    public WriteBack getWriteBack() {
        return writeBack;
    }
    public void setWriteBack(WriteBack writeBack) {
        this.writeBack = writeBack;
    }
    public Memory getMemory() {
        return memory;
    }
    public void setMemory(Memory memory) {
        this.memory = memory;
    }
    public Register getRegister() {
        return register;
    }
    public void setRegister(Register register) {
        this.register = register;
    }
    public int getPc() {
        return pc;
    }
    public void setPc(int pc) {
        this.pc = pc;
    }
    public PreIssue getPreIssue() {
        return preIssue;
    }
    public void setPreIssue(PreIssue preIssue) {
        this.preIssue = preIssue;
    }
    public PreMEM getPreMEM() {
        return preMEM;
    }
    public void setPreMEM(PreMEM preMEM) {
        this.preMEM = preMEM;
    }
    public PreALU getPreALU() {
        return preALU;
    }
    public void setPreALU(PreALU preALU) {
        this.preALU = preALU;
    }
    //    public PreALUB getPreALUB() {
//        return preALUB;
//    }
//    public void setPreALUB(PreALUB preALUB) {
//        this.preALUB = preALUB;
//    }
    public PostMEM getPostMEM() {
        return postMEM;
    }
    public void setPostMEM(PostMEM postMEM) {
        this.postMEM = postMEM;
    }
    public PostALU getPostALU() {
        return postALU;
    }
    public void setPostALU(PostALU postALU) {
        this.postALU = postALU;
    }
    //    public PostALUB getPostALUB() {
//        return postALUB;
//    }
//    public void setPostALUB(PostALUB postALUB) {
//        this.postALUB = postALUB;
//    }
    public void pcInc() {
        pc = pc + 4;
    }
}
class PostALU implements Buffers{

    LinkedList<String> buffer;
    String [] inBuffer = new String[4];
    String [] outBuffer = new String[4];
    int in, out;
    public PostALU() {
        this.buffer = new LinkedList<String>();
        this.in = 0;
        this.out = 0;
    }
    @Override
    public int hasEmptySlot() {

        return 1 - buffer.size();
    }

    @Override
    public boolean in(Object object) {

        inBuffer[in] = (String)object;
        in++;
        return false;
    }

    @Override
    public Object out() {
        outBuffer[out] = buffer.peek();
        out++;
        return buffer.peek();
    }
    public int size() {
        return buffer.size();
    }
    public Object get(int index) {
        return buffer.get(index);
    }
    public boolean mayRAW(String rd) {
        for (String string : buffer) {
            if(Adaptor.getOperands(string)[0].equals(rd)){
                return true;
            }
        }
        return false;
    }
    public int getOut() {
        return out;
    }
    public void flush() {
        for(int i=out-1;i>-1;i--){
            buffer.remove(outBuffer[i]);
        }
        for(int i=0;i<in;i++){
            buffer.offer(inBuffer[i]);
        }
        in = 0;
        out = 0;
    }
}
class PostMEM implements Buffers{

    LinkedList<String> buffer;
    String [] inBuffer = new String[4];
    String [] outBuffer = new String[4];
    int in, out;
    public PostMEM() {
        this.buffer = new LinkedList<String>();
        this.in = 0;
        this.out = 0;
    }

    @Override
    public int hasEmptySlot() {

        return 1 - buffer.size();
    }

    @Override
    public boolean in(Object object) {

        inBuffer[in] = (String)object;
        in++;
        return false;
    }

    @Override
    public Object out() {
        outBuffer[out] = buffer.peek();
        out++;
        return buffer.peek();
    }
    public Object get(int index) {
        return buffer.get(index);
    }
    public int size() {
        return buffer.size();
    }
    public boolean mayRAW(String rd) {
        for (String string : buffer) {
            if(string.contains("LW\t")){
                if (Adaptor.getOperands(string)[0].equals(rd)) {
                    return true;
                }
            }
        }
        return false;
    }
    public void flush() {
        for(int i=out-1;i>-1;i--){
            buffer.remove(outBuffer[i]);
        }
        for(int i=0;i<in;i++){
            buffer.offer(inBuffer[i]);
        }
        in = 0;
        out = 0;
    }
}
class PreALU implements Buffers{
    LinkedList<String> buffer;
    String [] inBuffer = new String[4];
    String [] outBuffer = new String[4];
    int in, out;
    public PreALU() {
        this.buffer = new LinkedList<String>();
        this.in = 0;
        this.out = 0;
    }
    @Override
    public int hasEmptySlot() {

        return 2 - this.buffer.size();
    }

    @Override
    public boolean in(Object object) {
        inBuffer[in] = (String)object;
        in++;
        return false;
    }

    @Override
    public Object out() {
        outBuffer[out] = buffer.peek();
        out++;
        return buffer.peek();
    }
    public int getOut() {
        return out;
    }
    public Object get(int index) {
        return this.buffer.get(index);
    }
    public Object remove(int index) {
        return this.buffer.remove(index);
    }
    public boolean mayWAW(String rd) {
        for (String string : buffer) {
            if(Adaptor.getOperands(string)[0].equals(rd)){
                return true;
            }
        }
        return false;
    }
    public boolean mayRAW(String rd) {
        for (String string : buffer){
            if (Adaptor.getOperands(string)[0].equals(rd)) {
                return true;
            }
        }
        return false;

    }
    public int size() {
        return buffer.size();
    }
    public void flush() {
        for(int i=out-1;i>-1;i--){
            //System.out.println("PreALU remove"+outBuffer[i]);
            buffer.remove(outBuffer[i]);
        }
        for(int i=0;i<in;i++){
            buffer.offer(inBuffer[i]);
        }
        in = 0;
        out = 0;
    }
}
class PreIssue implements Buffers{
    Pipeline pipeline;
    //双向列表 作为List使用时,一般采用add / get方法来 压入/获取对象;作为Queue使用时,才会采用 offer/poll/take等方法
    LinkedList<String> buffer;
    String [] inBuffer = new String[4];
    String [] outBuffer = new String[4];
    int in, out;
    public PreIssue(Pipeline pipeline) {
        this.buffer = new LinkedList<String>();
        this.pipeline = pipeline;
        this.in = 0;
        this.out = 0;
    }
    public int hasEmptySlot() {

        return 4 - this.buffer.size();
    }

    public boolean in(Object object) {

        inBuffer[in] = (String)object;
        //System.out.println("-------"+inBuffer[in]);
        in++;
        //防止两个同时进入PreIssue时，不能判断RAW
        for(int i=0;i<in;i++){
            buffer.offer(inBuffer[i]);//作为List使用时,一般采用add / get方法来 压入/获取对象;作为Queue使用时,才会采用 offer/poll/take等方法
        }
        in = 0;
        return false;
    }

    public Object out() {
        outBuffer[out] = buffer.peek();
        out++;
        return buffer.peek();//返回此列表的头元素，或null，如果此列表为空
    }
    public Object get(int index) {
        return this.buffer.get(index);
    }
    public Object remove(int index) {
        return this.buffer.remove(index);
    }
    //	public String type(int index) {
//		String assembly = this.buffer.get(index);
//		String type = "";
//
//		if (assembly.contains("LW\t")||assembly.contains("SW\t")) {
//			//MEM\
//			type = "MEM";
//		}else if(assembly.contains("SLL\t")||assembly.contains("SRL\t")||assembly.contains("SRA\t")||assembly.contains("MUL\t")){
//			//ALUB
//			type = "ALUB";
//		}else{
//			//ALU
//			type = "ALU";
//		}
//		return type;
//	}
    public int haveElem(int from) {
        for(int i=from;i<this.buffer.size();i++){
            if(pipeline.getIssue().noHazards(i)){
                return i;
            }
        }
        return -1;
    }
    public boolean mayWAW(String assembly) {
        for(int i=0;i<buffer.size();i++){
            if (buffer.get(i).equals(assembly)) {
                break;
            }
            String s1 = Adaptor.getOperands(buffer.get(i))[0];
            String s2 = Adaptor.getOperands(assembly)[0];
            if (buffer.get(i).contains("SW\t")||assembly.contains("SW\t")) {
                return false;
            }
            if (s1.equals(s2)) {
                return true;
            }
        }
        return false;
    }
    public boolean mayWAR(String assembly) {
        for(int i=0;i<buffer.size();i++){
            if(buffer.get(i).equals(assembly)){
                break;
            }
            if(assembly.contains("SW\t")){
                return false;
            }
            String [] operands = Adaptor.getOperands(buffer.get(i));

            String s2 = Adaptor.getOperands(assembly)[0];
            if(buffer.get(i).contains("LW\t")){
                if(operands[1].equals(s2)){
                    return true;
                }
            }else if(buffer.get(i).contains("SW\t")){
                if(operands[0].equals(s2)||operands[1].equals(s2)){
                    return true;
                }
            }else{
                if (operands[1].equals(s2)||operands[2].equals(s2)) {
                    return true;
                }
            }
        }
        return false;
    }
    public boolean mayRAW(String assembly) {
        for(int i=0;i<buffer.size();i++){
            if(buffer.get(i).equals(assembly)){
                break;
            }
            if(buffer.contains("SW\t")){
                continue;
            }

            String s1 = Adaptor.getOperands(buffer.get(i))[0];
            String s2 = Adaptor.getOperands(buffer.get(i))[1];
            String s20 = Adaptor.getOperands(assembly)[0];
            String s21 = Adaptor.getOperands(assembly)[1];
            String s22 = Adaptor.getOperands(assembly)[2];

//            System.out.println(" assembly---"+s1);
//            System.out.println(" s1---"+s1);
//            System.out.println(" s2---"+s1);
//            System.out.println(" s20---"+s1);
//            System.out.println(" s21---"+s1);
//            System.out.println(" s22---"+s1);
            if(assembly.contains("LW\t")){
                if (s21.equals(s1)) {
                    return true;
                }
            }else if(assembly.contains("SW\t")){
                if (s20.equals(s1)||s21.equals(s1)){
                    return true;
                }
            }else{
                if(s21.equals(s1)||s22.equals(s1)||s21.equals(s2)||s22.equals(s2)){
                    return true;
                }
            }
        }
        return false;
    }
    public boolean mayRAW4Branch(String r) {
        //System.out.println(buffer.toString());
        for(int i=0;i<buffer.size();i++ ){
            if(buffer.get(i).contains("SW\t")){
                continue;
            }
            if(Adaptor.getOperands(buffer.get(i))[0].equals(r)){
                return true;
            }
        }
        return false;
    }
    public int size() {
        return buffer.size();
    }
    public void flush() {
        for(int i=out-1;i>-1;i--){
            buffer.remove(outBuffer[i]);
        }
        for(int i=0;i<in;i++){
            buffer.offer(inBuffer[i]);//作为List使用时,一般采用add / get方法来 压入/获取对象;作为Queue使用时,才会采用 offer/poll/take等方法
        }
        in = 0;
        out = 0;
    }
}
class PreMEM implements Buffers{

    LinkedList<String> buffer;
    String [] inBuffer = new String[4];
    String [] outBuffer = new String[4];
    int in, out;
    public PreMEM() {
        this.buffer = new LinkedList<String>();
        this.in = 0;
        this.out = 0;
    }
    @Override
    public int hasEmptySlot() {

        return 1 - this.buffer.size();
    }

    @Override
    public boolean in(Object object) {

        inBuffer[in] = (String)object;
        in++;
        return false;
    }

    @Override
    public Object out() {
        outBuffer[out] = buffer.peek();
        out++;
        Object obj=buffer.peek();
        for(int i=out-1;i>-1;i--){
            buffer.remove(outBuffer[i]);
        }
        out=0;
        return obj;
    }

    public boolean mayWAW(String rd) {
        for (String string : buffer) {
            if(string.contains("SW\t"))continue;
            if(Adaptor.getOperands(string)[0].equals(rd)){
                return true;
            }
        }
        return false;
    }
    public boolean mayRAW(String rd) {
        for (String string : buffer){
            if(string.contains("LW\t")){
                if(Adaptor.getOperands(string)[0].equals(rd)){
                    return true;
                }
            }
        }

        return false;

    }
    public Object get(int index) {
        return buffer.get(index);
    }
    public int size() {
        return buffer.size();
    }
    public void flush() {
        for(int i=out-1;i>-1;i--){
            buffer.remove(outBuffer[i]);
        }
        for(int i=0;i<in;i++){
            buffer.offer(inBuffer[i]);
        }
        in = 0;
        out = 0;
    }
}
class Register {
    HashMap<String, Integer> registers;
    HashMap<String, Boolean> busy;
    HashMap<String, Boolean> stall;
    public Register() {
        this.registers = new HashMap<String, Integer>();
        this.busy = new HashMap<String, Boolean>();
        this.stall = new HashMap<String, Boolean>();
        init();
    }
    public void init() {
        for(int i=0;i<32;i++){
            this.registers.put("R"+i, 0);
            this.busy.put("R"+i, false);
            this.stall.put("R"+i, false);
        }
    }
    public int read(String key) {
        return this.registers.get(key);
    }
    public void write(String key, int value) {
        this.registers.put(key, value);
    }
    public void show() {
        Iterator<String> data = registers.keySet().iterator();
        while(data.hasNext()){
            String key = (String)data.next();
            System.out.println(String.format("Address %s: %d", key, registers.get(key)));
        }
    }
}
class WriteBack {
    Pipeline pipeline;
    //	public LinkedList<String> MEM = new LinkedList<String>();
//	public LinkedList<String> ALU = new LinkedList<String>();
//	public LinkedList<String> ALUB = new LinkedList<String>();
    public WriteBack(Pipeline pipeline) {

        this.pipeline = pipeline;
    }

    public void writeBack(int clock) {
        if(this.pipeline.getPostMEM().hasEmptySlot()==0){
            //"%s|%s|%d"
            String result = (String)this.pipeline.getPostMEM().out();
            String [] args = result.split("@");
            this.pipeline.getRegister().write(args[1], Integer.parseInt(args[2]));
//			MEM.offer(args[1]);
        }
        if(this.pipeline.getPostALU().hasEmptySlot()==0){
            String result = (String)this.pipeline.getPostALU().out();
            String [] args = result.split("@");
            this.pipeline.getRegister().write(args[1], Integer.parseInt(args[2]));
//			ALU.offer(args[1]);
        }
//        if(this.pipeline.getPostALUB().hasEmptySlot()==0){
//            String result = (String)this.pipeline.getPostALUB().out();
//            String [] args = result.split("@");
//            this.pipeline.getRegister().write(args[1], Integer.parseInt(args[2]));
////			ALUB.offer(args[1]);
//        }
    }
    public boolean isStalled() {
        return this.pipeline.getPostMEM().hasEmptySlot() == 1 &&
                this.pipeline.getPostALU().hasEmptySlot()==1;
    }
}
class Writer {
    public static boolean write(String fileName, StringBuffer sBuffer) {
        File file = new File(fileName);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        try {
            FileOutputStream out = new FileOutputStream(file, true);// append mod
            out.write(sBuffer.toString().getBytes("utf-8"));
            out.close();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return true;
    }
}

