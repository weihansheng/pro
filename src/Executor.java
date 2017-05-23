/**
 * Created by Johan007 on 17/5/11.
 */
public class Executor {
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
