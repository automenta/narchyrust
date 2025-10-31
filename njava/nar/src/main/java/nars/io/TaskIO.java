package nars.io;

import com.google.common.io.ByteArrayDataOutput;
import nars.*;
import nars.task.AbstractCommandTask;

import java.io.DataInput;
import java.io.IOException;

import static nars.Op.COMMAND;

/** TODO */
public enum TaskIO {
	;

	private static boolean hasTruth(byte punc) {
        return punc == Op.BELIEF || punc == Op.GOAL;
    }

    public static Task readTask(DataInput in) throws IOException {

        byte punc = in.readByte();

        Term term = TermIO.the.read(Op.terms, in);

        //Term term = preterm;//.normalize();
//        if (term == null)
//            throw new IOException("un-normalizable task target");

        if (punc != COMMAND) {
            Truth truth = hasTruth(punc) ? readTruth(in) : null;

            long start = in.readLong();
            long end = in.readLong();

            long[] evi = readEvidence(in);

            float pri = in.readFloat();

            long cre = in.readLong();

            return NALTask.task(term, punc, truth, start, end, evi).<NALTask>withPri(pri);

		} else {
            return new AbstractCommandTask(term);
        }
    }

    private static long[] readEvidence(DataInput in) throws IOException {
        int eviLength = in.readByte();
        long[] evi = new long[eviLength];
        for (int i = 0; i < eviLength; i++)
            evi[i] = in.readLong();

        return evi;
    }

    private static Truth readTruth(DataInput in) throws IOException {

        return Truth.read(in);
    }

    /**
     * WARNING
     */
    public static Task bytesToTask(byte[] b) throws IOException {
        return readTask(IO.input(b));
    }

	/**
	 * with Term first
	 */
	static void write(Task t, ByteArrayDataOutput out, boolean budget, boolean creation)  {

		byte p = t.punc();
		out.writeByte(p);

        t.term().write(out);

        if (p != COMMAND)
			writeNALTaskData((NALTask)t, out, budget, creation, p);
	}

	private static void writeNALTaskData(NALTask t, ByteArrayDataOutput out, boolean budget, boolean creation, byte p) {
		if (hasTruth(p))
			Truth.write(t.truth(), out);

		//TODO use delta zig zag encoding (with creation time too)
		out.writeLong(t.start());
		out.writeLong(t.end());

		IO.writeEvidence(out, t.stamp());

		if (budget)
			IO.writeBudget(out, t);

		if (creation) {
            out.writeLong(t.creation());
        }
	}
}