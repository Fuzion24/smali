package org.recursivedescent;

import org.jf.dexlib.Code.Format.Instruction31t;
import org.jf.dexlib.Code.Instruction;
import org.jf.dexlib.Code.MultiOffsetInstruction;
import org.jf.dexlib.Code.NonBranchInstruction;
import org.jf.dexlib.Code.OffsetInstruction;

import java.util.LinkedList;
import java.util.List;

public abstract class NInstructionNode {

    protected final Instruction insn;
    protected int offset;

    private NInstructionNode next;
    private NInstructionNode prev;

    NInstructionNode(Instruction insn) {
        this.insn = insn;
    }

    public int getOffset() { return offset; }
    public void setOffset(int offset) { this.offset = offset; }

    public Instruction getInsn() { return insn; }

    public NInstructionNode seq()  { return next; }
    public NInstructionNode pred() { return prev; }

    public List<NInstructionNode> allTargets() {
        final List<NInstructionNode> ret = new LinkedList<NInstructionNode>();
        if (next != null) {
            ret.add(next);
        }
        ret.addAll(nonSeq());
        return ret;
    }

    public abstract List<NInstructionNode> nonSeq();

    private static class BasicInsn extends NInstructionNode {

        BasicInsn(NonBranchInstruction insn) {
            super((Instruction) insn);
        }

        private static final List<NInstructionNode> NONSEQ = new LinkedList<NInstructionNode>();

        @Override
        public List<NInstructionNode> nonSeq() { return NONSEQ; }
    }

    private static class BranchInsn extends NInstructionNode {

        BranchInsn(OffsetInstruction insn) {
            super((Instruction) insn);
        }

        private final List<NInstructionNode> branch = null;

        @Override
        public List<NInstructionNode> nonSeq() {
            return branch;
        }
    }

    private static class SwitchInsn extends NInstructionNode {

        private TableInsn table = null;
        private List<NInstructionNode> branches = null;

        SwitchInsn(Instruction31t insn) {
            super(insn);
        }

        @Override
        public List<NInstructionNode> nonSeq() {
            final List<NInstructionNode> ret = new LinkedList<NInstructionNode>();
            ret.add(table);
            ret.addAll(branches);
            return ret;
        }
    }

    private static class TableInsn extends NInstructionNode {

        // Offsets are relative to the referencing instruction
        private static final List<NInstructionNode> NONSEQ = new LinkedList<NInstructionNode>();

        TableInsn(MultiOffsetInstruction insn) {
            super((Instruction) insn);
        }

        @Override
        public List<NInstructionNode> nonSeq() { return NONSEQ; }
    }
}
