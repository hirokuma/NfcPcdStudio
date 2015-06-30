package com.blogpost.hiro99ma.pcd;

public interface INfc {
	static byte[] s_CommandBuf = new byte[NfcPcd.SIZE_CMDBUF];
	static byte[] s_ResponseBuf = new byte[NfcPcd.SIZE_RESBUF];

	abstract void deselect();

	/**
	 * read from card
	 *
	 * @param[out]	buf			read buffer(16byte)
	 * @param[in]	blockNo		ブロック番号(0～)
	 * @param[in]	blockNum	ブロック数(1～)
	 * @param[in]	opt			option(null if not use)
	 *
	 * @return		true		成功
	 */
	abstract boolean read(byte[] buf, int[] blockNo, int blockNum, int[] opt);

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
	abstract boolean write(byte[] buf, int[] blockNo, int blockNum, int[] opt);
}
