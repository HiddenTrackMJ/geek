package org.bytedeco.javacv;

/**
 * User: hewro
 * Date: 2020/2/13
 * Time: 15:55
 * Description:
 */
import java.security.Key;
import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;
import javax.crypto.spec.IvParameterSpec;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;
@SuppressWarnings("restriction")
public class DesUtil {
    private final static String encoding = "utf-8";
    private final static String key = "cnmobile";
    private final static String secretKey = "cmcc_cnmobile_asiainfo_ocs";
    /**
     * 加密
     * @param plainText
     * @return
     */
    private static String encode(String plainText) {
        Key deskey = null;
        byte[] encryptData = null;
        try {
            DESedeKeySpec spec = new DESedeKeySpec(secretKey.getBytes());
            SecretKeyFactory keyfactory = SecretKeyFactory.getInstance("desede");
            deskey = keyfactory.generateSecret(spec);
            Cipher cipher = Cipher.getInstance("desede/CBC/PKCS5Padding");
            IvParameterSpec ips = new IvParameterSpec(key.getBytes());
            cipher.init(Cipher.ENCRYPT_MODE, deskey, ips);
            encryptData = cipher.doFinal(plainText.getBytes(encoding));
            BASE64Encoder base64Encoder = new BASE64Encoder();
            return str2Hex(base64Encoder.encode(encryptData));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }
    private static String str2Hex(String theStr) {
        int tmp;
        String tmpStr;
        byte[] bytes = theStr.getBytes();
        StringBuffer result = new StringBuffer(bytes.length * 2);
        for (int i = 0; i < bytes.length; i++) {
            tmp = bytes[i];
            if (tmp < 0) {
                tmp += 256;
            }
            tmpStr = Integer.toHexString(tmp);
            if (tmpStr.length() == 1) {
                result.append('0');
            }
            result.append(tmpStr);
        }
        return result.toString();
    }

    public static void main(String[] args) {
        //加密后存在特殊字符，如url传输需urlencode编码
        try {
            System.out.println(DesUtil.encode("phone=19999999999&source=01"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
