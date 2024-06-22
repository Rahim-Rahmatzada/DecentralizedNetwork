public class NodeUtils {
    public static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    public static int computeDistance(String hashID1, String hashID2) {
        String binary1 = hexToBinary(hashID1);
        String binary2 = hexToBinary(hashID2);
        if (binary1.length() != binary2.length()) {
            throw new IllegalArgumentException("HashIDs must have the same length.");
        }
        int matchingBits = 0;
        for (int i = 0; i < binary1.length(); i++) {
            if (binary1.charAt(i) == binary2.charAt(i)) {
                matchingBits++;
            } else {
                break;
            }
        }
        return 256 - matchingBits;
    }

    private static String hexToBinary(String hex) {
        StringBuilder binary = new StringBuilder();
        for (int i = 0; i < hex.length(); i++) {
            String binByte = String.format("%4s", Integer.toBinaryString(Character.digit(hex.charAt(i), 16))).replace(' ', '0');
            binary.append(binByte);
        }
        return binary.toString();
    }




}