package nars.term.atom;

import jcog.The;
import nars.io.IO;
import nars.term.Compound;

import java.io.DataInput;
import java.io.IOException;

import static nars.Op.ATOM;

public class BytesAtom extends Atomic implements The {

	public static BytesAtom atomBytes(String raw) {
		return atomBytes(raw.getBytes());
	}

	public static BytesAtom atomBytes(Compound c) {
		return atomBytes(IO.termToBytes(c));
	}

	public static BytesAtom atomBytes(DataInput i) throws IOException {
		byte[] bb = new byte[i.readUnsignedShort()];
		i.readFully(bb);
		return atomBytes(bb);
	}

	private static BytesAtom atomBytes(byte[] raw) {
		return new BytesAtom(raw);
	}


	private BytesAtom(byte[] compressed) {
		super(IO.opAndEncoding((byte)0 /* ATOM */, (byte) 2), compressed);
	}

	@Override
	public String toString() {
		byte[] b = bytes;
		//byte[] b = QuickLZ.decompress(bytes, 3)
		return "\"" + new String(b, 3, b.length-3) + "\"";
	}

	@Override
	public byte opID() {
		return ATOM.id;
	}


//	public static AtomCompressed compressed(byte[] raw) {
//		byte[] compressed = QuickLZ.compress(raw, 1);
//
//		{
//			byte[] oo = new byte[raw.length * 2];
//			ByteArrayDataOutput o = new ByteArrayDataOutput(oo);
//			LZ4.compress(raw, 0, raw.length, o, new LZ4.LZ4Table());
//			System.out.println(o.getPosition() + " " + Arrays.toString(oo));
//		}
//		{
//			byte[] oo = new byte[raw.length * 2];
//			ByteArrayDataOutput o = new ByteArrayDataOutput(oo);
//			LZ4.compressHC(raw, 0, raw.length, o, new LZ4.LZ4HCTable());
//			System.out.println(o.getPosition() + " " + Arrays.toString(oo));
//		}
//
//
//		//TODO use prefixReserve to use only one array
//		return new AtomCompressed(compressed);
//	}
}