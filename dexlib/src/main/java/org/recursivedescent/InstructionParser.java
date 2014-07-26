package org.recursivedescent;

import org.jf.dexlib.ClassDataItem;
import org.jf.dexlib.Code.Format.*;
import org.jf.dexlib.Code.Instruction;
import org.jf.dexlib.Code.MultiOffsetInstruction;
import org.jf.dexlib.Code.OffsetInstruction;
import org.jf.dexlib.Code.Opcode;
import org.jf.dexlib.CodeItem;
import org.jf.dexlib.DexFile;

import java.util.*;

public class InstructionParser {

    private final DexFile dex;
    public  final Map<Integer, InstructionNode.UnresolvedInstructionNode> offsetMap = new HashMap<Integer, InstructionNode.UnresolvedInstructionNode>();
    private final CodeItem.TryItem[] tries;

    private InstructionParser(DexFile dex, byte[] encodedInstructions, ClassDataItem.EncodedMethod em) {
        this.dex = dex;
        this.tries = em.codeItem.getTries();
        final PointedArray initial = new PointedArray(encodedInstructions, 0);
        parse(initial);
    }

    private void parse(PointedArray insns) {

        if (offsetMap.containsKey(insns.cursor)) {
            return;
        }

        if (insns.cursor < 0 || insns.cursor >= insns.array.length) {
            return;
        }

        final Instruction instruction = oneInsn(insns);
        final int size = instruction.getSize(insns.cursor/2)*2;

        Integer seq = null;
        if (instruction.opcode.canContinue()) {
            seq = size;
        }

        List<Integer> branches = new ArrayList<Integer>();
        if (instruction instanceof OffsetInstruction) {
            branches.add(((OffsetInstruction) instruction).getTargetAddressOffset() * 2);
            if (instruction.opcode.equals(Opcode.PACKED_SWITCH) || instruction.opcode.equals(Opcode.SPARSE_SWITCH)) {
                PointedArray datacursor = insns.shift(branches.get(0));
                parse(datacursor);

                InstructionNode.UnresolvedInstructionNode datainsn = offsetMap.get(datacursor.cursor);

                int[] offs = ((MultiOffsetInstruction) datainsn.insn).getTargets();
                for (int o : offs) {
                    branches.add(o * 2);
                }
            }
        }

        List<InstructionNode.UnresolvedInstructionNode.UnresolvedTryCatch> tcs = new ArrayList<InstructionNode.UnresolvedInstructionNode.UnresolvedTryCatch>();
        if (tries != null && instruction.opcode.canThrow()) {
            for (CodeItem.TryItem t : tries) {
                final int startAddr = t.getStartCodeAddress() * 2;
                final int endAddr = startAddr + (t.getTryLength() * 2);
                if (insns.cursor >= startAddr && insns.cursor < endAddr) {
                    int catchAll = t.encodedCatchHandler.getCatchAllHandlerAddress() * 2;
                    InstructionNode.UnresolvedInstructionNode.UnresolvedTryCatch utc;
                    if (catchAll >= 0) {
                        utc = new InstructionNode.UnresolvedInstructionNode.UnresolvedTryCatch(catchAll - insns.cursor);
                    } else {
                        utc = new InstructionNode.UnresolvedInstructionNode.UnresolvedTryCatch();
                    }
                    for (CodeItem.EncodedTypeAddrPair p : t.encodedCatchHandler.handlers) {
                        final int handler = p.getHandlerAddress() * 2;
                        utc.add(p.exceptionType, handler - insns.cursor);
                    }
                    tcs.add(utc);
                }
            }
        }

        final InstructionNode.UnresolvedInstructionNode insn = new InstructionNode.UnresolvedInstructionNode(instruction, insns.cursor, seq, branches, tcs);

        offsetMap.put(insns.cursor, insn);

        for (Integer o : insn.allTargets()) {
            parse(insns.shift(o));
        }

    }

    private Instruction oneInsn(PointedArray insns) {

        // TODO: Properly express junk
        if (insns.cursor < 0 || insns.cursor >= insns.array.length) {
            System.out.println("Went off the deep end");
            return new Instruction10x(Opcode.JUNK_OP);
        }

        try {

        short opcodeValue = (short)(insns.head() & 0xFF);
        if (opcodeValue == 0xFF) {
            opcodeValue = (short)((0xFF << 8) | insns.tail().head());
        }

        Opcode opcode = Opcode.getOpcodeByValue(opcodeValue);

        Instruction instruction = null;

        if (opcode == null) {
            System.err.println(String.format("unknown opcode encountered - %x. Treating as nop.",
                    (opcodeValue & 0xFFFF)));
            instruction = new UnknownInstruction(opcodeValue);
        } else {
            if (opcode == Opcode.NOP) {
                byte secondByte = insns.tail().head();
                switch (secondByte) {
                    case 0:
                    {
                        instruction = new Instruction10x(Opcode.NOP, insns.array, insns.cursor);
                        break;
                    }
                    case 1:
                    {
                        instruction = new PackedSwitchDataPseudoInstruction(insns.array, insns.cursor);
                        break;
                    }
                    case 2:
                    {
                        instruction = new SparseSwitchDataPseudoInstruction(insns.array, insns.cursor);
                        break;
                    }
                    case 3:
                    {
                        instruction = new ArrayDataPseudoInstruction(insns.array, insns.cursor);
                        break;
                    }
                }
            } else {
                instruction = opcode.format.Factory.makeInstruction(dex, opcode, insns.array, insns.cursor);
            }
        }

        //System.out.println(instruction.opcode);
        return instruction;

        } catch (Exception e) {
            System.out.println("Exception thrown while parsing: " + e.getMessage());
            System.out.println("Offset: " + insns.cursor + " Bytes: " + insns.head() + " " + insns.tail().head());
            return new Instruction10x(Opcode.JUNK_OP);
        }
    }

    public static InstructionNode parse(DexFile dex, byte[] insns, ClassDataItem.EncodedMethod em) {
        final InstructionParser p = new InstructionParser(dex, insns, em);
        return InstructionNode.resolve(p.offsetMap);
    }

}
