package com.blogpost.hiro99ma.pcd;

import android.util.Log;

public class NfcF implements INfc {

	///////////////////////////
	// public fields
	///////////////////////////

	public static final SysCode SYSCODE = SysCode.NDEF;

	public enum SysCode {
		COMMON(0xfe00),				//共通領域
		CYBER(0x0003),				//サイバネ
		Lite(0x88b4),				//FeliCa Lite
		NDEF(0x12fc);				//NDEF

		private final int code;
		SysCode(int val) { code = val; }
		int val() { return code; }
	}

	public static final short SVCODE_RW = (short)0x0009;		//Read/Write
	public static final short SVCODE_RO = (short)0x000b;		//ReadOnly


	///////////////////////////
	// private fields
	///////////////////////////

	private static final String TAG = "NfcF";
	private static NfcF mNfc = null;

	//block parameter
	private static final short BM_LEN2 = (short)0x8000;	//block list element length 2byte
	//private static final short BM_LEN3 = 0x0000;		//block list element length 3byte
	private static final short AM_NORMAL = 0x0000;		//ramdom, cyclic, parse(dec, direct)
	//private static final short AM_CACHEBACK = 0x1000;	//parse(cacheback access)
	private static final short SCN_NORMAL = 0x00;		//Service Code Number(0～)


	///////////////////////////
	// methods
	///////////////////////////

	public static NfcF getInstance() {
		if(mNfc == null) {
			mNfc = new NfcF();
		}
		return mNfc;
	}

	protected NfcF() {}

	@Override
	public void deselect() {
		// TODO Auto-generated method stub
	}


	/**
	 * 2byteブロック長
	 * @param blockNo		ブロック番号(0～)
	 * @return				ブロックリストエレメント
	 */
	private static short create_blocklist2(int blockNo) {
		return (short)(BM_LEN2 | AM_NORMAL | (SCN_NORMAL << 8) | (short)blockNo);
	}

	/**
	 * read from card
	 *
	 * @param[out]	buf			read buffer(16byte * blockNum)
	 * @param[in]	blockNo[]	ブロック番号(0～)
	 * @param[in]	blockNum	ブロック数(1～)
	 * @param[in]	opt			[0]Service Code
	 */
	@Override
	public boolean read(byte[] buf, int[] blockNo, int blockNum, int[] opt) {
		if(buf.length < blockNum * NfcPcd.SIZE_BLOCK) {
			Log.e(TAG, "less read buffer");
			return false;
		}
		byte[] len = new byte[1];
		final NfcPcd.NfcId id = NfcPcd.getNfcId();

		s_CommandBuf[0] = 0x06;
		NfcPcd.MemCpy(s_CommandBuf, id.Id, id.Length, 1, 0);
		s_CommandBuf[9] = 0x01;							//サービス数(今のところ、１つ)
		s_CommandBuf[10] = NfcPcd.l16((short)opt[0]);	//Service Code List(upper)
		s_CommandBuf[11] = NfcPcd.h16((short)opt[0]);	//Service Code List(lower)
		s_CommandBuf[12] = (byte)blockNum;				//ブロック数
		for(int i=0; i<blockNum; i++) {
			short blist = create_blocklist2((short)(blockNo[i] & 0xffff));
			s_CommandBuf[13 + i*2] = NfcPcd.h16(blist);
			s_CommandBuf[14 + i*2] = NfcPcd.l16(blist);
		}
		boolean ret = NfcPcd.communicateThruEx(
							s_CommandBuf, 13 + blockNum*2, s_ResponseBuf, len);
		if (!ret || (s_ResponseBuf[0] != s_CommandBuf[0]+1)
		  || !NfcPcd.MemCmp(s_ResponseBuf, id.Id, id.Length, 1, 0)
		  || (s_ResponseBuf[9] != 0x00)
		  || (s_ResponseBuf[10] != 0x00)) {
			Log.e(TAG, "read : ret=" + ret);
			return false;
		}
		//s_ResponseBuf[11] == 0x01
		NfcPcd.MemCpy(buf, s_ResponseBuf, blockNum * NfcPcd.SIZE_BLOCK, 0, 12);

		return true;
	}

	/**
	 * write to card
	 *
	 * @param[out]	buf			write buffer(16byte * blockNum)
	 * @param[in]	blockNo[]	ブロック番号(0～)
	 * @param[in]	blockNum	ブロック数(1～)
	 * @param[in]	opt			[0]Service Code
	 */
	@Override
	public boolean write(byte[] buf, int[] blockNo, int blockNum, int[] opt) {
		if(buf.length < blockNum * NfcPcd.SIZE_BLOCK) {
			Log.e(TAG, "less write buffer");
			return false;
		}
		byte[] len = new byte[1];
		final NfcPcd.NfcId id = NfcPcd.getNfcId();

		s_CommandBuf[0] = 0x08;
		NfcPcd.MemCpy(s_CommandBuf, id.Id, id.Length, 1, 0);
		s_CommandBuf[9] = 0x01;							//サービス数(今のところ、１つ)
		s_CommandBuf[10] = NfcPcd.l16((short)opt[0]);	//Service Code List(upper)
		s_CommandBuf[11] = NfcPcd.h16((short)opt[0]);	//Service Code List(lower)
		s_CommandBuf[12] = (byte)blockNum;				//ブロック数
		for(int i=0; i<blockNum; i++) {
			short blist = create_blocklist2((short)(blockNo[i] & 0xffff));
			s_CommandBuf[13 + i*2] = NfcPcd.h16(blist);
			s_CommandBuf[14 + i*2] = NfcPcd.l16(blist);
		}
		NfcPcd.MemCpy(s_CommandBuf, buf, NfcPcd.SIZE_BLOCK * blockNum, 13 + blockNum*2, 0);
		boolean ret = NfcPcd.communicateThruEx(
							s_CommandBuf, 13 + blockNum*2 + NfcPcd.SIZE_BLOCK * blockNum,
							s_ResponseBuf, len);
		if (!ret || (s_ResponseBuf[0] != s_CommandBuf[0]+1)
		  || !NfcPcd.MemCmp(s_ResponseBuf, id.Id, id.Length, 1, 0)
		  || (s_ResponseBuf[9] != 0x00)
		  || (s_ResponseBuf[10] != 0x00)) {
			Log.e(TAG, "write : ret=" + ret);
			return false;
		}

		return true;
	}
}
