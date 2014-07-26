package org.recursivedescent;

import org.jf.dexlib.Code.Instruction;
import org.jf.dexlib.CodeItem;
import org.jf.dexlib.TypeIdItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InstructionNode {
    public Instruction insn;
    public int offset;
    public InstructionNode seq = null;
    public InstructionNode pred = null;
    public final List<InstructionNode> branches = new ArrayList<InstructionNode>();
    public final List<TryCatch> trycatches = new ArrayList<TryCatch>();

    private InstructionNode(UnresolvedInstructionNode in) {
        this.insn   = in.insn;
        this.offset = in.offset;
    }

    public InstructionNode(Instruction insn) {
        this.insn = insn;
    }

    public static InstructionNode resolve(Map<Integer, UnresolvedInstructionNode> instructionMap) {
        final Map<Integer, InstructionNode> resolvedMap = new HashMap<Integer, InstructionNode>(instructionMap.size());
        for (UnresolvedInstructionNode insn : instructionMap.values()) {
            resolvedMap.put(insn.offset, new InstructionNode(insn));
        }

        for (InstructionNode i : resolvedMap.values()) {
            final UnresolvedInstructionNode u = instructionMap.get(i.offset);
            if (u.seq != null) {
                i.seq = resolvedMap.get(u.offset + u.seq);
                if (i.seq != null) {
                    i.seq.pred = i;
                } else {
                    //System.out.println("hrmmmm");
                }
            }
            for (int b : u.branches) {
                i.branches.add(resolvedMap.get(u.offset + b));
            }
            for (UnresolvedInstructionNode.UnresolvedTryCatch utc : u.trycatches) {
                InstructionNode catchAll = null;
                if (utc.catchAll != null) catchAll = resolvedMap.get(u.offset + utc.catchAll);
                final TryCatch tc = new TryCatch(catchAll);
                for (TypeIdItem typ : utc.catches.keySet()) {
                    final InstructionNode cat = resolvedMap.get(u.offset + utc.catches.get(typ));
                    tc.catches.put(typ, cat);
                }
                i.trycatches.add(tc);
            }
        }
        return resolvedMap.get(0);
    }

    public List<InstructionNode> allTargets() {
        final List<InstructionNode> ret = new ArrayList<InstructionNode>();
        if (seq != null) { ret.add(seq); }
        ret.addAll(nonSeq());
        return ret;
    }

    public List<InstructionNode> nonSeq() {
        final List<InstructionNode> ret = new ArrayList<InstructionNode>();
        ret.addAll(branches);
        for (TryCatch tc : trycatches) { ret.addAll(tc.allCatches()); }
        return ret;
    }

    @Override
    public String toString() {
        return this.offset + ": " + this.insn.opcode.name();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) return false;
        if (!(o instanceof InstructionNode)) return false;
        final InstructionNode other = (InstructionNode) o;
        return (this.offset == other.offset);
    }

    public static class TryCatch {
        public final InstructionNode catchAll;
        public final Map<TypeIdItem, InstructionNode> catches = new HashMap<TypeIdItem, InstructionNode>();

        public TryCatch(InstructionNode catchAll) {
            this.catchAll = catchAll;
        }

        public List<InstructionNode> allCatches() {
            final List<InstructionNode> ret = new ArrayList<InstructionNode>();
            if (catchAll != null) ret.add(catchAll);
            ret.addAll(catches.values());
            return ret;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null) return false;
            if (!(o instanceof TryCatch)) return false;
            final TryCatch cast = (TryCatch) o;
            if (cast.catchAll != catchAll) return false;
            if (cast.catches.size() != catches.size()) return false;
            for (TypeIdItem t : catches.keySet()) {
                if (!cast.catches.containsKey(t)) return false;
                final InstructionNode i = catches.get(t);
                final InstructionNode u = cast.catches.get(t);
                if (!i.equals(u)) return false;
            }
            return true;
        }
    }

    protected static class UnresolvedInstructionNode {
        public final Instruction insn;
        public final int offset;

        public final Integer seq;
        public final List<Integer> branches = new ArrayList<Integer>();
        public final List<UnresolvedTryCatch> trycatches = new ArrayList<UnresolvedTryCatch>();

        public UnresolvedInstructionNode(Instruction insn, int offset, Integer seq, List<Integer> branches, List<UnresolvedTryCatch> trycatches) {
            this.insn   = insn;
            this.offset = offset;
            this.seq = seq;
            if (branches != null)   this.branches.addAll(branches);
            if (trycatches != null) this.trycatches.addAll(trycatches);
        }

        public List<Integer> allTargets() {
            final List<Integer> ret = new ArrayList<Integer>();
            if (seq != null) {
                ret.add(seq);
            }
            ret.addAll(branches);
            for (UnresolvedTryCatch utc : trycatches) ret.addAll(utc.allCatches());
            return ret;
        }

        @Override
        public String toString() {
            return this.offset + ": " + this.insn.opcode.name();
        }

        public static class UnresolvedTryCatch {

            public final Integer catchAll;
            public final Map<TypeIdItem, Integer> catches = new HashMap<TypeIdItem,Integer>();

            public UnresolvedTryCatch() {
                this.catchAll = null;
            }

            public UnresolvedTryCatch(int catchAll) {
                this.catchAll = catchAll;
            }

            public UnresolvedTryCatch add(TypeIdItem type, Integer offset) {
                this.catches.put(type, offset);
                return this;
            }

            public List<Integer> allCatches() {
                final List<Integer> ret = new ArrayList<Integer>();
                if (catchAll != null) ret.add(catchAll);
                ret.addAll(catches.values());
                return ret;
            }

        }
    }
}
