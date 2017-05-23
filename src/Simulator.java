import java.util.ArrayList;

/**
 * Created by Johan007 on 17/5/11.
 */
public class Simulator {


    public static void main(String[] args) {

        Simulator simulator = new Simulator();
        simulator.simulate(args);
    }
    public void simulate(String []args) {
        Pipeline pipeline = new Pipeline();
        Memory memory = new Memory();
        //ArrayList<String> aList = Loader.readFileByLines(args[0]);
        ArrayList<String> aList = Loader.readFileByLines("/Users/Johan007/proj2/sampleTest.txt");
        System.out.println("aList---"+aList.size());
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
        System.out.println("code---"+code.size());
        int addr2 = addr1 + code.size() * 4;
        //获取数据（即没有指令的二进制代码）
        for(int i=code.size();i<aList.size();i++){
            data.add(complementToInt(aList.get(i)));
            //System.out.println("data"+(i-13)+":"+complementToInt(aList.get(i)));
        }
        System.out.println("data---"+data.size());
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
            //Writer.write(args[1], show(clock, pipeline));
            Writer.write("/Users/Johan007/proj2/simulationTest.txt", show(clock, pipeline));
            if(pipeline.getInsFetch().executedIns.length()>0){
                pipeline.getInsFetch().executedIns = "";
            }
            //System.out.println("clock:"+clock);
            clock++;
        }
        System.out.println("end");
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
