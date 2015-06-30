/**
 *
 */
package com.blogpost.hiro99ma.pcd;

import android.util.Log;

/**
 * @author hiroshi
 *
 */
public class NfcA implements INfc {

	///////////////////////////
	// public fields
	///////////////////////////

	public static final int[] KEY_DEFAULT = new int[] {
			0xff, 0xff, 0xff, 0xff, 0xff, 0xff,		//KeyA
			0xff, 0xff, 0xff, 0xff, 0xff, 0xff		//KeyB
	};


	///////////////////////////
	// private fields
	///////////////////////////

	private static final String TAG = "NfcA";
	private static NfcA mNfc = null;


	///////////////////////////
	// methods
	///////////////////////////

	public static NfcA getInstance() {
		if(mNfc == null) {
			mNfc = new NfcA();
		}
		return mNfc;
	}

	/**
	 *
	 */
	protected NfcA() {}

	/* (non-Javadoc)
	 * @see com.blogpost.hiro99ma.pcd.INfc#deselect()
	 */
	@Override
	public void deselect() {
		// TODO Auto-generated method stub

	}

	/**
	 * read from card
	 *
	 * @param[out]	buf			read buffer(16byte)
	 * @param[in]	blockNo		ブロック番号(0～)
	 * @param[in]	blockNum	ブロック数(1～)
	 * @param[in]	opt			[0..5]KeyA
	 *
	 * @return		true		成功
	 */
	@Override
	public boolean read(byte[] buf, int[] blockNo, int blockNum, int[] opt) {
		if(buf.length < blockNum * NfcPcd.SIZE_BLOCK) {
			Log.e(TAG, "less read buffer");
			return false;
		}
		byte[] len = new byte[1];
		final NfcPcd.NfcId id = NfcPcd.getNfcId();

		s_CommandBuf[0] = (byte)blockNo[0];
		s_CommandBuf[2] = (byte)1;
		s_CommandBuf[3] = (byte)opt[0];
		s_CommandBuf[4] = (byte)opt[1];
		s_CommandBuf[5] = (byte)opt[2];
		s_CommandBuf[6] = (byte)opt[3];
		s_CommandBuf[7] = (byte)opt[4];
		s_CommandBuf[8] = (byte)opt[5];
		NfcPcd.MemCpy(s_CommandBuf, id.Id, id.Length, 9, 0);

		// Key A Authentication
		s_CommandBuf[1] = 0x60;		//Key A Auth
		boolean ret = NfcPcd.inDataExchange(
						s_CommandBuf, 9 + id.Length,
						s_ResponseBuf, len);
		if(!ret) {
			Log.e(TAG, "Auth A fail");

			// Key B Authentication
			s_CommandBuf[1] = 0x61;		//Key B Auth
			s_CommandBuf[3] = (byte)opt[6];
			s_CommandBuf[4] = (byte)opt[7];
			s_CommandBuf[5] = (byte)opt[8];
			s_CommandBuf[6] = (byte)opt[9];
			s_CommandBuf[7] = (byte)opt[10];
			s_CommandBuf[8] = (byte)opt[11];
			ret = NfcPcd.inDataExchange(
							s_CommandBuf, 9 + id.Length,
							s_ResponseBuf, len);
			if(!ret) {
				Log.e(TAG, "Auth B fail");
				//return false;
			}
		}

		// Read
		s_CommandBuf[1] = 0x30;		//read
		ret = NfcPcd.inDataExchange(
						s_CommandBuf, 3,
						s_ResponseBuf, len);
		if(ret) {
			NfcPcd.MemCpy(buf, s_ResponseBuf, len[0], 0, 0);
		} else {
			Log.e(TAG, "read fail3");
		}

		return ret;
	}


	/**
	 * read from card
	 *
	 * @param[out]	buf			read buffer(16byte)
	 * @param[in]	blockNo		ブロック番号(0～)
	 *
	 * @return		true		成功
	 */
	public boolean read(byte[] buf, int blockNo) {
		int[] blk = new int[] { blockNo };
		return read(buf, blk, 1, KEY_DEFAULT);
	}


	/**
	 * write to card
	 *
	 * @param[out]	buf			write buffer(16byte * blockNum)
	 * @param[in]	blockNo[]	ブロック番号(0～)
	 * @param[in]	blockNum	ブロック数(1～)
	 * @param[in]	opt			option(null if not use)
	 *
	 * @return		true		成功
	 */
	@Override
	public boolean write(byte[] buf, int[] blockNo, int blockNum, int[] opt) {
		// TODO Auto-generated method stub
		return false;
	}

}
