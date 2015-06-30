package com.blogpost.hiro99ma.pcd;

import android.util.Log;

public class Felica extends NfcF {

	///////////////////////////
	// public fields
	///////////////////////////

	public static final SysCode SYSCODE = SysCode.COMMON;		//共通
	public static final SysCode SYSC_CYBER = SysCode.CYBER;		//サイバネ


	///////////////////////////
	// private fields
	///////////////////////////

	private static final String TAG = "Felica";
	private static Felica mNfc = null;

	//timeout
	private static final short kPUSH_TIMEOUT = (short)(2100 * 2);

	//FALP command
//	private static final byte kFCMD_HELLO = 0x00;
//	private static final byte kFCMD_YEAH = 0x01;
//	private static final byte kFCMD_CSND = 0x02;
//	private static final byte kFCMD_SEND = 0x03;
//	private static final byte kFCMD_ADIOS = 0x05;
//	private static final byte kFCMD_BYE = 0x07;
//
//	//OBEX command
//	private static final byte kOBEX_SUCCESS = (byte)0xa0;


	///////////////////////////
	// methods
	///////////////////////////

	public static Felica getInstance() {
		if(mNfc == null) {
			mNfc = new Felica();
		}
		return mNfc;
	}

	private Felica() {}


	/**
	 * [FeliCa]PUSHコマンド
	 *
	 * @param[in]	data		PUSHデータ
	 * @param[in]	dataLen		dataの長さ
	 *
	 * @retval		true			成功
	 * @retval		false			失敗
	 *
	 * @attention	- dataはそのまま送信するため、上位層で加工しておくこと。
	 */
	public static boolean push(byte[] data, int dataLen) {
		boolean ret;
		byte[] responseLen = new byte[1];
		final NfcPcd.NfcId id = NfcPcd.getNfcId();

		if (dataLen > 224) {
			Log.e(TAG, "bad len");
			return false;
		}

		s_CommandBuf[0] = (byte)0xb0;			//PUSH
		NfcPcd.MemCpy(s_CommandBuf, id.Id, id.Length, 1, 0);
		s_CommandBuf[9] = (byte)dataLen;
		NfcPcd.MemCpy(s_CommandBuf, data, dataLen, 10, 0);

		// xx:IDm
		// [cmd]b0 xx xx xx xx xx xx xx xx len (push data...)
		ret = NfcPcd.communicateThruEx(kPUSH_TIMEOUT, s_CommandBuf, 10 + dataLen, s_ResponseBuf, responseLen);
		if (!ret || (responseLen[0] != 10) || (s_ResponseBuf[0] != s_CommandBuf[0]+1) ||
		  !NfcPcd.MemCmp(s_ResponseBuf, id.Id, id.Length, 1, 0) ||
		  (s_ResponseBuf[9] != dataLen)) {

			Log.e(TAG, "push1 : ret=" + ret);
			return false;
		}

		// xx:IDm
		// [cmd]a4 xx xx xx xx xx xx xx xx 00
		s_CommandBuf[0] = (byte)0xa4;			//inactivate? activate2?
		NfcPcd.MemCpy(s_CommandBuf, id.Id, id.Length, 1, 0);
		s_CommandBuf[9] = 0x00;

		ret = NfcPcd.communicateThruEx(s_CommandBuf, 10, s_ResponseBuf, responseLen);
		if (!ret || (responseLen[0] != 10) || (s_ResponseBuf[0] != s_CommandBuf[0]+1) ||
		  !NfcPcd.MemCmp(s_ResponseBuf, id.Id, id.Length, 1, 0) ||
		  (s_ResponseBuf[9] != 0x00)) {

			Log.e(TAG, "push2 : ret=" + ret);
			return false;
		}

		return true;
	}

	public static boolean pushUrl(String str) {
		boolean ret = false;
		byte[] data = null;
		int data_len = 0;

		final byte[] str_byte = str.getBytes();
		short str_len = (short)(str.length() + 2);
		data = new byte[256];

		int chksum = 0;
		int cnt = 0;

		//
		data[cnt] = 0x01;
		chksum += data[cnt] & 0xff;
		cnt++;

		// header
		data[cnt] = 0x02;		//URL
		chksum += data[cnt] & 0xff;
		cnt++;
		data[cnt] = (byte)(str_len & 0x00ff);
		chksum += data[cnt] & 0xff;
		cnt++;
		data[cnt] = (byte)((str_len & 0xff00) >> 8);
		chksum += data[cnt] & 0xff;
		cnt++;

		str_len -= 2;

		// param
		data[cnt] = (byte)(str_len & 0x00ff);
		chksum += data[cnt] & 0xff;
		cnt++;
		data[cnt] = (byte)((str_len & 0xff00) >> 8);
		chksum += data[cnt] & 0xff;
		cnt++;
		for(int i=0; i<str_len; i++) {
			data[cnt] = str_byte[i];
			chksum += data[cnt] & 0xff;
			cnt++;
		}

		//check sum
		short sum = (short)-chksum;
		data[cnt] = (byte)((sum & 0xff00) >> 8);
		cnt++;
		data[cnt] = (byte)(sum & 0x00ff);
		cnt++;

		data_len = cnt;
		ret = push(data, data_len);

		return ret;
	}
}
