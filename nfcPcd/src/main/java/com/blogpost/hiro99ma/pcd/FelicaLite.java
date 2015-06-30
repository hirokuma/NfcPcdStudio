package com.blogpost.hiro99ma.pcd;

import android.util.Log;

public class FelicaLite extends NfcF {

	///////////////////////////
	// public fields
	///////////////////////////

	public static final SysCode SYSCODE = SysCode.Lite;

	public enum Block {
		PAD0(0x0000),
		PAD1(0x0001),
		PAD2(0x0002),
		PAD3(0x0003),
		PAD4(0x0004),
		PAD5(0x0005),
		PAD6(0x0006),
		PAD7(0x0007),
		PAD8(0x0008),
		PAD9(0x0009),
		PAD10(0x000a),
		PAD11(0x000b),
		PAD12(0x000c),
		PAD13(0x000d),
		REG(0x000e),
		RC(0x0080),
		MAC(0x0081),
		ID(0x0082),
		D_ID(0x0083),
		SER_C(0x0084),
		SYS_C(0x0085),
		CKV(0x0086),
		CK(0x0087),
		MC(0x0088);

		private final int blk;
		Block(int blk) { this.blk = blk; }
		public int val() { return blk; }
	}


	///////////////////////////
	// private fields
	///////////////////////////

	private static final String TAG = "FelicaLite";
	private static FelicaLite mNfc = null;


	///////////////////////////
	// methods
	///////////////////////////

	public static FelicaLite getInstance() {
		if(mNfc == null) {
			mNfc = new FelicaLite();
		}
		return mNfc;
	}

	private FelicaLite() {}

	/**
	 * read from card
	 *
	 * @param[out]	buf			read buffer(16byte * blockNum)
	 * @param[in]	blockNo[]	ブロック番号(0～)
	 * @param[in]	blockNum	ブロック数(1～4)
	 */
	public boolean read(byte[] buf, int[] blockNo, int blockNum) {
		Log.d(TAG, "read");
        final int[] optr = new int[] { NfcF.SVCODE_RO };				//service code
		return read(buf, blockNo, blockNum, optr);
	}

	/**
	 * read from card
	 *
	 * @param[out]	buf			read buffer(16byte)
	 * @param[in]	blockNo		ブロック番号(0～)
	 */
/*	public boolean read(byte[] buf, int blockNo) {
		Log.d(TAG, "read1");
		final int[] bno = new int[] { blockNo };
		return read(buf, bno, 1);
	}
*/
	/**
	 * read from card
	 *
	 * @param[out]	buf			read buffer(16byte)
	 * @param[in]		blk			ブロック(PAD0～)
	 */
	public boolean read(byte[] buf, Block blk) {
		Log.d(TAG, "read1");
		final int[] bno = new int[] { blk.val() };
		return read(buf, bno, 1);
	}

	/**
	 * write to card
	 *
	 * @param[out]	buf			write buffer(16byte)
	 * @param[in]	blockNo		ブロック番号(0～)
	 */
/*	public boolean write(byte[] buf, int blockNo) {
		Log.d(TAG, "write");
		final int[] optw = new int[] { NfcF.SVCODE_RW };
		final int[] bno = new int[] { blockNo };
		return write(buf, bno, 1, optw);
	}
*/
	/**
	 * write to card
	 *
	 * @param[out]	buf			write buffer(16byte)
	 * @param[in]		blk			ブロック(PAD0～)
	 */
	public boolean write(byte[] buf, Block blk) {
		Log.d(TAG, "write");
		final int[] optw = new int[] { NfcF.SVCODE_RW };
		final int[] bno = new int[] { blk.val() };
		return write(buf, bno, 1, optw);
	}
}
