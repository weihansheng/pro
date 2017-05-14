/**
 * Created by Johan007 on 17/5/11.
 */
public class Mem {
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
