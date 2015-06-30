package com.blogpost.hiro99ma.pcd;

import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;
import javax.crypto.spec.IvParameterSpec;

import com.blogpost.hiro99ma.pcd.FelicaLite.Block;

import android.util.Log;

public final class FelicaLiteIssuance {

	///////////////////////////
	// public fields
	///////////////////////////

	public enum Result {
		SUCCESS,

		ENOTCARD,				///!< カードが見つからない
		EBADSYSCODE,			///!< システムコード不正
		EISSUED,				///!< 発行済み(1次、2次)
		ERROR,					///!< よくわからないがエラー
	}


	///////////////////////////
	// private fields
	///////////////////////////

	private static final String TAG = "FelicaLiteIssuance";


	///////////////////////////
	// methods
	///////////////////////////

	/**
	 * １次発行(システムブロックの書き換え禁止設定は行わない)
	 *
	 * @param dfd			DFD
	 * @param masterKey		個別化マスター鍵(24byte)
	 * @param keyVersion	鍵バージョン
	 *
	 * @return			true	１次発行成功
	 */
	public static Result issuance1(short dfd, byte[] masterKey, short keyVersion) {
		// 7.3.1 Pollingレスポンスの確認
		boolean ret = NfcPcd.pollingF(FelicaLite.SYSCODE);
		if(!ret) {
			Log.e(TAG, "card not found.");
			return Result.ENOTCARD;
		}

		// 7.3.2 システムコードの確認
		ret = checkSystemCode();
		if(!ret) {
			Log.e(TAG, "bad system code.");
			return Result.EBADSYSCODE;
		}

		// おまけ
		ret = checkNotIssuance();
		if(!ret) {
			Log.e(TAG, "issuanced card.");
			return Result.EISSUED;
		}

		// 7.3.3 IDの設定
		byte[] id = new byte[NfcPcd.SIZE_BLOCK];
		ret = writeID(id, dfd);
		if(!ret) {
			Log.e(TAG, "write ID fail.");
			return Result.ERROR;
		}

		// 7.3.4 カード鍵の書き込み
		// 7.3.5 カード鍵の確認
		ret = writeCardKey(id, masterKey);
		if(!ret) {
			Log.e(TAG, "write Card Key fail.");
			return Result.ERROR;
		}

		// 7.3.6 カード鍵バージョンの書き込み
		ret = writeKeyVersion(keyVersion);
		if(!ret) {
			Log.e(TAG, "write Key Version fail.");
			return Result.ERROR;
		}

		// 7.3.7 ユーザーブロックの書き込み
		//やらない

		// 7.3.8 システムブロックの書き換え禁止設定
		//やらない


		return Result.SUCCESS;
	}

	/**
	 * システムコード確認
	 *
	 * @return	true	FeliCa Liteである
	 */
	private static boolean checkSystemCode() {
		byte[] buf = new byte[NfcPcd.SIZE_BLOCK];
		FelicaLite f = FelicaLite.getInstance();
		boolean ret = f.read(buf,  FelicaLite.Block.SYS_C);
		if(ret == false) {
			Log.v(TAG, "checkSystemCode : read fail");
			return false;
		}
		if(NfcPcd.hl16(buf[0], buf[1]) != (short)FelicaLite.SYSCODE.val()) {
			Log.v(TAG, "checkSystemCode : invalid syscode");
			return false;
		}
		for(int i=2; i<NfcPcd.SIZE_BLOCK; i++) {
			if(buf[i] != 0x00) {
				Log.v(TAG, "checkSystemCode : invalid block");
				return false;
			}
		}

		return true;
	}

	/**
	 * 未発行確認
	 *
	 * @return		true	未発行である
	 */
	private static boolean checkNotIssuance() {
		byte[] buf = new byte[NfcPcd.SIZE_BLOCK];
		FelicaLite f = FelicaLite.getInstance();
		boolean ret = f.read(buf,  FelicaLite.Block.MC);
		if(ret == false) {
			Log.v(TAG, "checkNotIssuance : read fail");
			return false;
		}
		if(buf[2] == 0x00) {
			Log.v(TAG, "checkNotIssuance : first issuranced");
			return false;
		}
		if((buf[1] & 0x80) == 0) {
			Log.v(TAG, "checkNotIssuance : second issuranced");
			return false;
		}
		return true;
	}

	/**
	 * ID設定
	 *
	 */
	private static boolean writeID(byte[] id, short dfd) {
		FelicaLite f = FelicaLite.getInstance();
		boolean ret = f.read(id,  FelicaLite.Block.D_ID);
		if(ret == false) {
			Log.v(TAG, "writeID : read fail");
			return false;
		}

		id[8] = NfcPcd.h16(dfd);
		id[9] = NfcPcd.l16(dfd);
		for(int i=10; i<NfcPcd.SIZE_BLOCK; i++) {
			id[i] = 0x00;
		}
		ret = writeWithCheck(id, FelicaLite.Block.ID);
		if(ret == false) {
			Log.v(TAG, "writeID : write fail");
			return false;
		}

		return true;
	}

	/**
	 * カード鍵書き込み
	 *
	 * @param masterKey	個別化マスター鍵(24byte)
	 *
	 * @return
	 */
	private static boolean writeCardKey(byte[] id, byte[] masterKey) {
		byte[] ck = new byte[NfcPcd.SIZE_BLOCK];
		boolean ret = calcPersonalCardKey(ck, masterKey, id);
		if(ret == false) {
			Log.v(TAG, "writeCardKey: personal key fail");
			return false;
		}

		//CKはチェックできない
		FelicaLite f = FelicaLite.getInstance();
		ret = f.write(ck, FelicaLite.Block.CK);
		if(ret == false) {
			Log.v(TAG, "writeCardKey : write fail");
			return false;
		}

		ret = macCheck(ck);
		if(ret == false) {
			Log.v(TAG, "writeCardKey : mac fail");
			return false;
		}

		return true;
	}

	private static boolean macCheck(byte[] ck) {
		return macCheck(null, ck);
	}

	/**
	 * MAC比較
	 *
	 * @param masterKey	個別化マスター鍵(24byte)
	 * @param dummy		nullを設定してください
	 *
	 * @return		true	MAC一致
	 */
	public static boolean macCheck(byte[] masterKey, byte[] dummy) {
		byte[] rc = new byte[NfcPcd.SIZE_BLOCK];			//ランダム値を入れる
		SecureRandom random = new SecureRandom();
		random.nextBytes(rc);

		FelicaLite f = FelicaLite.getInstance();

		boolean ret = f.write(rc, FelicaLite.Block.RC);
		if(ret == false) {
			Log.v(TAG, "macCheck : write rc fail");
			return false;
		}
		int[] blkNo = new int[] { FelicaLite.Block.ID.val(), FelicaLite.Block.MAC.val() };
		byte[] buf = new byte[NfcPcd.SIZE_BLOCK * blkNo.length];
		ret = f.read(buf,  blkNo, blkNo.length);
		if(ret == false) {
			Log.v(TAG, "macCheck : read fail");
			return false;
		}

		byte[] ck = dummy;
		if(ck == null) {
			ck = new byte[16];
			ret = calcPersonalCardKey(ck, masterKey, buf);
			if(ret == false) {
				Log.v(TAG, "macCheck: personal key fail");
				return false;
			}
		}

		// buf[0-15]:ID, buf[16-31]:MAC
		byte[] mac = new byte[8];
		ret = calcMac(mac, ck, buf, rc);
		if(ret == false) {
			Log.v(TAG, "macCheck: mac calc fail");
			return false;
		}
		ret = NfcPcd.MemCmp(mac, buf, 8, 0, 16);

		return ret;
	}


	/**
	 * 鍵バージョン書き込み
	 *
	 * @param keyVersion	鍵バージョン
	 *
	 * @return		true	書き込み成功
	 */
	private static boolean writeKeyVersion(short keyVersion) {
		byte[] buf = new byte[NfcPcd.SIZE_BLOCK];
		buf[0] = NfcPcd.h16(keyVersion);
		buf[1] = NfcPcd.l16(keyVersion);
		boolean ret = writeWithCheck(buf, FelicaLite.Block.CKV);
		if(ret == false) {
			Log.v(TAG, "writeKeyVersion : write fail");
			return false;
		}

		return true;
	}

	/**
	 * チェック付きブロック書き込み(16byte)
	 *
	 * @param buf		書き込みデータ
	 * @param blockNo	書き込みブロック番号
	 *
	 * @return	true	チェックOK
	 */
	private static boolean writeWithCheck(byte[] buf, Block blk) {
		byte[] bufChk = new byte[NfcPcd.SIZE_BLOCK];
		FelicaLite f = FelicaLite.getInstance();

		boolean ret = f.write(buf, blk);
		if(ret == false) {
			Log.v(TAG, "checkWrite : write fail");
			return false;
		}

		ret = f.read(bufChk, blk);
		if(ret == false) {
			Log.v(TAG, "checkWrite : read fail");
			return false;
		}
		for(int i=0; i<NfcPcd.SIZE_BLOCK; i++) {
			if(buf[i] != bufChk[i]) {
				Log.v(TAG, "checkWrite : bad read result");
				return false;
			}
		}

		return true;
	}

	/**
	 * MAC計算
	 *
	 * @param mac	MAC計算結果(先頭から8byte書く)。エラーになっても書き換える可能性あり。
	 * @param ck	カード鍵(16byte)
	 * @param id	ID(16byte)
	 * @param rc	ランダムチャレンジブロック(16byte)
	 *
	 * @return		true	MAC計算成功
	 */
	private static boolean calcMac(byte[] mac, byte[] ck, byte[] id, byte[] rc) {
		byte[] sk = new byte[16];
		IvParameterSpec ips = null;

		// 秘密鍵を準備
		byte[] key = new byte[24];
		for(int i=0; i<8; i++) {
			key[i] = key[16+i] = ck[7-i];
			key[8+i] = ck[15-i];
		}

		byte[] rc1 = new byte[8];
		byte[] rc2 = new byte[8];
		byte[] id1 = new byte[8];
		byte[] id2 = new byte[8];
		for(int i=0; i<8; i++) {
			rc1[i] = rc[7-i];
			rc2[i] = rc[15-i];
			id1[i] = id[7-i];
			id2[i] = id[15-i];
		}

		// RC[1]==(CK)==>SK[1]
		ips = new IvParameterSpec(new byte[8]);		//zero
		int ret = enc83(sk, 0, key, rc1, 0, ips);		//RC1-->SK1
		if(ret != 8) {
			Log.e(TAG, "calcMac: proc1");
			return false;
		}

		// SK[1] =(iv)> RC[2] =(CK)=> SK[2]
		ips = new IvParameterSpec(sk, 0, 8);	//SK1
		ret = enc83(sk, 8, key, rc2, 0, ips);	//RC2-->SK2
		if(ret != 8) {
			Log.e(TAG, "calcMac: proc2");
			return false;
		}

		/////////////////////////////////////////////////////////
		for(int i=0; i<8; i++) {
			key[i] = key[16+i] = sk[i];
			key[8+i] = sk[8+i];
		}

		// RC[1] =(iv)=> ID[1] =(SK)=> tmp
		ips = new IvParameterSpec(rc1, 0, 8);	//RC1
		ret = enc83(mac, 0, key, id1, 0, ips);	//ID1-->tmp
		if(ret != 8) {
			Log.e(TAG, "calcMac: proc3");
			return false;
		}

		// tmp =(iv)=> ID[2] =(SK)=> tmp
		ips = new IvParameterSpec(mac);			//tmp
		ret = enc83(mac, 0, key, id2, 0, ips);	//ID1-->tmp
		if(ret != 8) {
			Log.e(TAG, "calcMac: proc4");
			return false;
		}

		for(int i=0; i<4; i++) {
			byte swp = mac[i];
			mac[i] = mac[7-i];
			mac[7-i] = swp;
		}

		return true;
	}


	/**
	 * 個別化カード鍵作成
	 *
	 * @param personalKey	生成した個別化カード鍵(16byte)
	 * @param masterKey		個別化マスター鍵(24byte)
	 * @param id			IDブロック(16byte)
	 *
	 * @return		true	作成成功
	 */
	static private boolean calcPersonalCardKey(byte[] personalKey, byte[] masterKey, byte[] id) {
		IvParameterSpec ips = new IvParameterSpec(new byte[8]);

		byte[] enc1 = new byte[8];		//L

		byte[] text = new byte[8];
		int ret = enc83(enc1, 0, masterKey, text, 0, ips);
		if(ret != 8) {
			Log.e(TAG, "calcPersonalCardKey: proc1");
			return false;
		}
		boolean msb = false;
		for(int i=7; i>=0; i--) {
			boolean bak = msb;
			msb = ((enc1[i] & 0x80) != 0) ? true : false;
			enc1[i] <<= 1;
			if(bak) {
				enc1[i] |= 0x01;
			}
		}
		if(msb) {
			enc1[7] ^= 0x1b;
		}

		byte[] id1 = new byte[8];		//M1
		byte[] id2 = new byte[8];		//M2
		for(int i=0; i<8; i++) {
			id1[i] = id[7-i];
			id2[i] = (byte)(id[15-i] ^ enc1[i]);
		}

		byte[] c1 = new byte[8];
		ret = enc83(c1, 0, masterKey, id1, 0, ips);	//c1
		if(ret != 8) {
			Log.e(TAG, "calcPersonalCardKey: proc2");
			return false;
		}

		ips = new IvParameterSpec(c1);
		byte[] t = new byte[8];
		ret = enc83(t, 0, masterKey, id2, 0, ips);	//t
		if(ret != 8) {
			Log.e(TAG, "calcPersonalCardKey: proc3");
			return false;
		}

		id1[0] ^= 0x80;		//M1'
		ips = new IvParameterSpec(new byte[8]);
		ret = enc83(c1, 0, masterKey, id1, 0, ips);	//c1'
		if(ret != 8) {
			Log.e(TAG, "calcPersonalCardKey: proc4");
			return false;
		}

		ips = new IvParameterSpec(c1);	//c1'
		ret = enc83(c1, 0, masterKey, id2, 0, ips);	//t'
		if(ret != 8) {
			Log.e(TAG, "calcPersonalCardKey: proc5");
			return false;
		}

		for(int i=0; i<8; i++) {
			personalKey[i] = t[i];
			personalKey[8+i] = c1[i];
		}

		return true;
	}


	/**
	 * Triple-DES暗号化
	 *
	 * @param outBuf		暗号化出力バッファ(8byte以上)
	 * @param outOffset		暗号化出力バッファへの書き込み開始位置(ここから8byte書く)
	 * @param key			秘密鍵(24byte [0-7]KEY1, [8-15]KEY2, [16-23]KEY1)
	 * @param inBuf			平文バッファ(8byte以上)
	 * @param inOffset		平文バッファの読み込み開始位置(ここから8byte読む)
	 * @param ips			初期ベクタ(8byte)
	 *
	 * @return		true	暗号化成功
	 */
	private static int enc83(byte[] outBuf, int outOffset, byte[] key, byte[] inBuf, int inOffset, IvParameterSpec ips) {
		int sz = 0;
		try {
			// 秘密鍵を準備
			SecretKeyFactory kf = SecretKeyFactory.getInstance("DESede");
			DESedeKeySpec dk = new DESedeKeySpec(key);
			SecretKey sk = kf.generateSecret(dk);
			dk = null;
			kf = null;

			// 暗号
			Cipher c = Cipher.getInstance("DESede/CBC/NoPadding");
			c.init(Cipher.ENCRYPT_MODE, sk, ips);
			sz = c.doFinal(inBuf, inOffset, 8, outBuf, outOffset);

		} catch (Exception e) {
			Log.e(TAG, "enc83 exception");
		}

		return sz;
	}
}
